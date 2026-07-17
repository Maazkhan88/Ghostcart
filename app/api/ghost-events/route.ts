import { getD1 } from "../../../db";
import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";

const MAX_PRODUCTS_PER_CHECKOUT = 40;
const DEFAULT_RANKING_LIMIT = 12;
const MAX_RANKING_LIMIT = 30;
const MIN_PUBLIC_GHOSTERS = 3;
const MAX_EVENTS_PER_ACTOR_PER_HOUR = 60;
const SAFE_ID = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$/;
const UAE_UTC_OFFSET_HOURS = 4;

type GhostEventPayload = {
  checkoutId?: unknown;
  productIds?: unknown;
  source?: unknown;
};

type CatalogIdentifier = {
  id: number;
  idText: string;
  slug: string;
};

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}

export async function GET(request: Request) {
  try {
    const requestedLimit = Number(new URL(request.url).searchParams.get("limit"));
    const limit = Number.isInteger(requestedLimit)
      ? Math.min(Math.max(requestedLimit, 1), MAX_RANKING_LIMIT)
      : DEFAULT_RANKING_LIMIT;
    const db = getD1();
    const [rankingResult, activityResult] = await db.batch([
      db
        .prepare(
          `SELECT
             e.product_id AS productId,
             p.slug AS productSlug,
             p.name AS productName,
             p.category AS category,
             p.image_url AS imageUrl,
             COUNT(*) AS ghostCount,
             COUNT(DISTINCT e.actor_hash) AS uniqueGhosters,
             MAX(e.created_at) AS lastActivityAt
           FROM ghost_events e
           INNER JOIN products p ON p.id = e.product_id AND p.is_active = 1
           WHERE e.event_date = DATE('now', '+4 hours')
             AND e.actor_hash IS NOT NULL
           GROUP BY e.product_id, p.slug, p.name, p.category, p.image_url
           HAVING COUNT(DISTINCT e.actor_hash) >= ?
           ORDER BY ghostCount DESC, lastActivityAt DESC, e.product_id ASC
           LIMIT ?`,
        )
        .bind(MIN_PUBLIC_GHOSTERS, limit),
      db.prepare(
        `SELECT
           COUNT(*) AS rawGhostCount,
           COUNT(DISTINCT actor_hash) AS uniqueGhosters,
           MAX(created_at) AS lastActivityAt
         FROM ghost_events
         WHERE event_date = DATE('now', '+4 hours')
           AND actor_hash IS NOT NULL
           AND product_id IS NOT NULL`,
      ),
    ]);
    const activity = (activityResult.results?.[0] ?? {}) as {
      rawGhostCount?: number;
      uniqueGhosters?: number;
      lastActivityAt?: string | null;
    };
    const uniqueGhosters = Number(activity.uniqueGhosters ?? 0);
    const hasActivity = Number(activity.rawGhostCount ?? 0) > 0;
    const meetsPrivacyThreshold = uniqueGhosters >= MIN_PUBLIC_GHOSTERS;
    const generatedAt = new Date();
    const lastActivityAt = activity.lastActivityAt ?? null;
    const freshnessSeconds = lastActivityAt
      ? Math.max(0, Math.floor((generatedAt.getTime() - Date.parse(lastActivityAt)) / 1000))
      : null;
    const rankingRows = (rankingResult.results ?? []) as Array<{
      productId: number;
      productSlug: string;
      productName: string;
      category: string;
      imageUrl: string | null;
      ghostCount: number;
      uniqueGhosters: number;
      lastActivityAt: string;
    }>;

    return json(
      {
        period: "today",
        timeZone: "Asia/Dubai",
        date: new Date(
          generatedAt.getTime() + UAE_UTC_OFFSET_HOURS * 60 * 60 * 1000,
        )
          .toISOString()
          .slice(0, 10),
        generatedAt: generatedAt.toISOString(),
        lastActivityAt,
        freshnessSeconds,
        dataState: !hasActivity
          ? "no_activity"
          : meetsPrivacyThreshold
            ? "live"
            : "privacy_threshold",
        privacy: {
          minimumUniqueGhosters: MIN_PUBLIC_GHOSTERS,
          suppressed: hasActivity && !meetsPrivacyThreshold,
        },
        totalGhosts: meetsPrivacyThreshold ? Number(activity.rawGhostCount ?? 0) : null,
        rankings: meetsPrivacyThreshold
          ? rankingRows.map((row, index) => ({
              rank: index + 1,
              productId: String(row.productId),
              productSlug: row.productSlug,
              productName: row.productName,
              category: row.category,
              imageUrl: row.imageUrl,
              ghostCount: Number(row.ghostCount),
              uniqueGhosters: Number(row.uniqueGhosters),
              lastActivityAt: row.lastActivityAt,
            }))
          : [],
      },
      {
        headers: {
          "Cache-Control": "public, max-age=30, s-maxage=60, stale-while-revalidate=120",
        },
      },
    );
  } catch {
    return json(
      { error: "Live Ghost Cart trends are temporarily unavailable" },
      { status: 503, headers: { "Cache-Control": "no-store" } },
    );
  }
}

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as GhostEventPayload;
    const checkoutId = typeof payload.checkoutId === "string"
      ? payload.checkoutId.trim()
      : "";
    const rawProductIds = Array.isArray(payload.productIds)
      ? payload.productIds
      : [];
    const productIds = Array.from(
      new Set(
        rawProductIds
          .filter((value): value is string => typeof value === "string")
          .map((value) => value.trim())
          .filter(Boolean),
      ),
    );
    const source = typeof payload.source === "string"
      ? payload.source.trim().toLowerCase()
      : "unknown";

    if (!SAFE_ID.test(checkoutId)) {
      return json({ error: "checkoutId is invalid" }, { status: 400 });
    }
    if (!productIds.length || productIds.length > MAX_PRODUCTS_PER_CHECKOUT) {
      return json(
        { error: `productIds must contain between 1 and ${MAX_PRODUCTS_PER_CHECKOUT} unique items` },
        { status: 400 },
      );
    }
    if (productIds.some((productId) => !SAFE_ID.test(productId))) {
      return json({ error: "one or more productIds are invalid" }, { status: 400 });
    }
    if (!/^(android|web|ios|unknown)$/.test(source)) {
      return json({ error: "source is invalid" }, { status: 400 });
    }

    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "ghost-events",
      actorHash,
      cost: productIds.length,
      limit: MAX_EVENTS_PER_ACTOR_PER_HOUR,
      windowSeconds: 60 * 60,
    });
    if (!rateLimit.allowed) {
      return json(
        { error: "Too many Ghost Cart activity events; try again later" },
        {
          status: 429,
          headers: {
            "Cache-Control": "no-store",
            "Retry-After": String(rateLimit.retryAfterSeconds),
            "X-RateLimit-Remaining": "0",
          },
        },
      );
    }

    const placeholders = productIds.map(() => "?").join(", ");
    const db = getD1();
    const lookup = await db
      .prepare(
        `SELECT id, CAST(id AS TEXT) AS idText, slug
         FROM products
         WHERE is_active = 1
           AND (CAST(id AS TEXT) IN (${placeholders}) OR slug IN (${placeholders}))`,
      )
      .bind(...productIds, ...productIds)
      .all<CatalogIdentifier>();
    const identifiers = lookup.results ?? [];
    const byInput = new Map<string, CatalogIdentifier>();
    for (const product of identifiers) {
      byInput.set(String(product.idText), product);
      byInput.set(product.slug, product);
    }
    const invalidProductIds = productIds.filter((id) => !byInput.has(id));
    if (invalidProductIds.length) {
      return json(
        {
          error: "Every trend event must reference an active Ghost Cart catalog product",
          invalidProductIds,
        },
        {
          status: 400,
          headers: {
            "Cache-Control": "no-store",
            "X-RateLimit-Remaining": String(rateLimit.remaining),
          },
        },
      );
    }
    const canonicalProducts = Array.from(
      new Map(
        productIds.map((input) => {
          const product = byInput.get(input) as CatalogIdentifier;
          return [product.id, product] as const;
        }),
      ).values(),
    );
    const statements = canonicalProducts.map((product) =>
      db
        .prepare(
          `INSERT INTO ghost_events
             (event_key, product_key, product_id, actor_hash, event_date, source)
           VALUES (?, ?, ?, ?, DATE('now', '+4 hours'), ?)
           ON CONFLICT DO NOTHING`,
        )
        .bind(
          `${checkoutId}:${product.id}`,
          product.slug,
          product.id,
          actorHash,
          source,
        ),
    );
    const results = await db.batch(statements);
    const recorded = results.reduce(
      (
        total: number,
        result: { meta?: { changes?: number } },
      ) => total + (Number(result.meta?.changes ?? 0) > 0 ? 1 : 0),
      0,
    );

    return json(
      {
        recorded,
        duplicateCount: canonicalProducts.length - recorded,
        message:
          recorded > 0
            ? "Anonymous Ghost Cart activity recorded"
            : "This activity was already counted",
      },
      {
        status: recorded > 0 ? 201 : 200,
        headers: {
          "Cache-Control": "no-store",
          "X-RateLimit-Remaining": String(rateLimit.remaining),
        },
      },
    );
  } catch {
    return json(
      { error: "Unable to record Ghost Cart activity right now" },
      { status: 503, headers: { "Cache-Control": "no-store" } },
    );
  }
}
