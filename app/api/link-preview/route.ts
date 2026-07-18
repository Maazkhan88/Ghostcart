import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";
import { previewRetailerLink } from "../../../lib/product-link-preview";

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "no-store");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
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
    return json({ product: result.product });
  } catch (error) {
    return json(
      { error: error instanceof Error ? error.message : "Unable to preview this product" },
      { status: 400 },
    );
  }
}