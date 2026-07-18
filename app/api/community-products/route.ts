import { getD1 } from "../../../db";
import { readNonNegativeInteger, readSafeUrl, sanitizeShortText } from "../../../lib/backend-contract";
import {
  canonicalizeRetailerUrl,
  isAllowedProductImageUrl,
} from "../../../lib/product-link-preview";
import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";

const MAX_FEED_SIZE = 30;

type CommunityRow = {
  id: string;
  sourceDomain: string;
  canonicalUrl: string;
  title: string;
  category: string;
  imageUrl: string | null;
  priceCents: number;
  currencyCode: string;
  ghostCount: number;
  lastGhostedAt: string;
};

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function serialize(row: CommunityRow) {
  return {
    id: row.id,
    sourceDomain: row.sourceDomain,
    sourceUrl: row.canonicalUrl,
    title: row.title,
    category: row.category,
    imageUrl: row.imageUrl,
    priceCents: Number(row.priceCents),
    currencyCode: row.currencyCode,
    ghostCount: Number(row.ghostCount),
    lastGhostedAt: row.lastGhostedAt,
    activityTag: "User Ghosted",
    isCommunity: true,
  };
}

export async function GET(request: Request) {
  try {
    const rawLimit = Number(new URL(request.url).searchParams.get("limit"));
    const limit = Number.isInteger(rawLimit) ? Math.min(Math.max(rawLimit, 1), MAX_FEED_SIZE) : 12;
    const result = await getD1()
      .prepare(
        `SELECT id, source_domain AS sourceDomain, canonical_url AS canonicalUrl, title, category,
                image_url AS imageUrl, price_cents AS priceCents,
                currency_code AS currencyCode, ghost_count AS ghostCount,
                last_ghosted_at AS lastGhostedAt
         FROM community_products
         WHERE status = 'visible' AND ghost_count > 0
         ORDER BY last_ghosted_at DESC, ghost_count DESC
         LIMIT ?`,
      )
      .bind(limit)
      .all<CommunityRow>();
    return json(
      {
        products: (result.results ?? []).map(serialize),
        privacy: {
          anonymous: true,
          sourceLinksExposed: true,
          note: "Only the public retailer link is exposed; the person who shared it remains anonymous.",
        },
      },
      { headers: { "Cache-Control": "public, max-age=30, s-maxage=60" } },
    );
  } catch {
    return json({ error: "Community products are temporarily unavailable" }, { status: 503 });
  }
}

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as Record<string, unknown>;
    if (payload.shareWithCommunity !== true) {
      return json({ error: "Anonymous community sharing requires explicit consent" }, { status: 400 });
    }
    const sourceValue = typeof payload.sourceUrl === "string" ? payload.sourceUrl : "";
    const canonicalUrl = canonicalizeRetailerUrl(sourceValue);
    const title = sanitizeShortText(payload.title, "title", 160, { required: true });
    const category = sanitizeShortText(payload.category ?? "Other", "category", 80, { required: true });
    const price = readNonNegativeInteger(payload.priceCents ?? 0, "priceCents");
    const image = readSafeUrl(payload.imageUrl, "imageUrl");
    const error = title.error || category.error || price.error || image.error;
    if (error) return json({ error }, { status: 400 });
    if (image.value && !isAllowedProductImageUrl(image.value)) {
      return json({ error: "Product image must use a safe public HTTPS URL" }, { status: 400 });
    }
    if (payload.currencyCode !== undefined && payload.currencyCode !== "AED") {
      return json({ error: "Community products currently use AED values only" }, { status: 400 });
    }
    const source = typeof payload.source === "string" && /^(android|web|ios)$/.test(payload.source)
      ? payload.source
      : "unknown";
    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "community-products",
      actorHash,
      cost: 1,
      limit: 30,
      windowSeconds: 60 * 60,
    });
    if (!rateLimit.allowed) {
      return json(
        { error: "Too many community product actions. Try again later." },
        { status: 429, headers: { "Retry-After": String(rateLimit.retryAfterSeconds) } },
      );
    }

    const canonicalKey = await sha256(canonicalUrl.toString());
    const id = `ugc_${canonicalKey.slice(0, 24)}`;
    const now = new Date().toISOString();
    const db = getD1();
    await db
      .prepare(
        `INSERT INTO community_products (
           id, canonical_key, canonical_url, source_domain, title, category,
           image_url, price_cents, currency_code, ghost_count, status,
           last_ghosted_at, created_at, updated_at
         ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'AED', 0, 'visible', ?, ?, ?)
         ON CONFLICT(canonical_key) DO UPDATE SET
           title = excluded.title,
           category = excluded.category,
           image_url = COALESCE(excluded.image_url, community_products.image_url),
           price_cents = CASE WHEN excluded.price_cents > 0 THEN excluded.price_cents ELSE community_products.price_cents END,
           updated_at = excluded.updated_at`,
      )
      .bind(
        id,
        canonicalKey,
        canonicalUrl.toString(),
        canonicalUrl.hostname,
        title.value,
        category.value,
        image.value ?? null,
        price.value ?? 0,
        now,
        now,
        now,
      )
      .run();

    const ghostResult = await db
      .prepare(
        `INSERT INTO community_product_ghosts
           (community_product_id, actor_hash, source, created_at)
         VALUES (?, ?, ?, ?)
         ON CONFLICT(community_product_id, actor_hash) DO NOTHING`,
      )
      .bind(id, actorHash, source, now)
      .run();
    const counted = Number(ghostResult.meta.changes ?? 0) > 0;
    if (counted) {
      await db
        .prepare(
          `UPDATE community_products
           SET ghost_count = ghost_count + 1, last_ghosted_at = ?, updated_at = ?
           WHERE id = ?`,
        )
        .bind(now, now, id)
        .run();
    }
    const row = await db
      .prepare(
        `SELECT id, source_domain AS sourceDomain, canonical_url AS canonicalUrl, title, category,
                image_url AS imageUrl, price_cents AS priceCents,
                currency_code AS currencyCode, ghost_count AS ghostCount,
                last_ghosted_at AS lastGhostedAt
         FROM community_products WHERE id = ? LIMIT 1`,
      )
      .bind(id)
      .first<CommunityRow>();
    return json(
      { product: row ? serialize(row) : null, counted },
      { status: counted ? 201 : 200, headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    return json(
      { error: error instanceof Error ? error.message : "Unable to share this product" },
      { status: 400, headers: { "Cache-Control": "no-store" } },
    );
  }
}
