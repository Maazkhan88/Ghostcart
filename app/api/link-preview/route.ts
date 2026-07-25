import { getD1 } from "../../../db";
import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";
import { previewRetailerLink, sha256Hex, type RetailerProductPreview } from "../../../lib/product-link-preview";

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "no-store");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}

// If this exact listing's image has already been captured before (by anyone
// - a product photo isn't personal data, and reusing it needs no user
// scoping), reuse it instead of re-scraping Amazon every single time, which
// is what a live re-scrape is doing today even for a product we've already
// successfully seen. Checks two existing sources of "known good images":
// this user's or anyone else's past almost_buys captures (exact source_url
// match - already canonicalized at capture time), and the anonymous
// community_products feed (already keyed by the same canonical-URL hash).
// No new table, no migration - both sources already exist for other reasons.
async function findKnownProductImage(canonicalUrl: string): Promise<string | null> {
  try {
    const db = getD1();
    const fromHistory = await db
      .prepare(
        `SELECT image_url AS imageUrl FROM almost_buys
         WHERE source_url = ? AND image_url IS NOT NULL
         ORDER BY captured_at DESC LIMIT 1`,
      )
      .bind(canonicalUrl)
      .first<{ imageUrl: string }>();
    if (fromHistory?.imageUrl) return fromHistory.imageUrl;

    const canonicalKey = await sha256Hex(canonicalUrl);
    const fromCommunity = await db
      .prepare(`SELECT image_url AS imageUrl FROM community_products WHERE canonical_key = ? AND image_url IS NOT NULL LIMIT 1`)
      .bind(canonicalKey)
      .first<{ imageUrl: string }>();
    return fromCommunity?.imageUrl ?? null;
  } catch (error) {
    console.error(`[link-preview] known-image lookup failed: ${error instanceof Error ? error.message : String(error)}`);
    return null;
  }
}

function withKnownImage(product: RetailerProductPreview, imageUrl: string): RetailerProductPreview {
  const complete = product.title.length > 0 && product.priceCents !== null;
  return {
    ...product,
    imageUrl,
    status: complete ? "complete" : product.status,
    note: complete ? undefined : product.note,
  };
}

export async function POST(request: Request) {
  try {
    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "link-preview",
      actorHash,
      cost: 1,
      limit: 20,
      windowSeconds: 60 * 60,
    });
    if (!rateLimit.allowed) {
      return json(
        { error: "Too many link previews. Try again later." },
        { status: 429, headers: { "Retry-After": String(rateLimit.retryAfterSeconds) } },
      );
    }
    const payload = (await request.json()) as { url?: unknown };
    if (typeof payload.url !== "string" || payload.url.length > 2048) {
      return json({ error: "Share a valid public HTTPS link" }, { status: 400 });
    }
    const result = await previewRetailerLink(payload.url);
    if (result.kind === "listing") {
      return json({ listing: { sourceDomain: result.sourceDomain, retailer: result.retailer, items: result.items } });
    }
    let product = result.product;
    if (!product.imageUrl) {
      const knownImage = await findKnownProductImage(product.canonicalUrl);
      if (knownImage) {
        console.log(`[link-preview] reused known image host=${product.sourceDomain} (live scrape had none)`);
        product = withKnownImage(product, knownImage);
      }
    }
    return json({ product });
  } catch (error) {
    return json(
      { error: error instanceof Error ? error.message : "Unable to preview this product" },
      { status: 400 },
    );
  }
}