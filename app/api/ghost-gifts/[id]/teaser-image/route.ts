import { env } from "cloudflare:workers";
import { getD1 } from "../../../../../db";
import { getContentMediaBucket } from "../../../../../lib/content-media";
import { hashGiftToken, isGiftToken } from "../../../../../lib/ghost-gifts";
import { isValidProductImageKey } from "../../../../../lib/product-image-rehost";
import { isAllowedProductImageUrl } from "../../../../../lib/product-link-preview";

async function readSourceImage(imageUrl: string): Promise<ReadableStream> {
  const url = new URL(imageUrl);
  const isGhostCartHost = url.hostname === "theghostcart.com" || url.hostname === "www.theghostcart.com";

  if (isGhostCartHost && url.pathname.startsWith("/api/product-images/")) {
    const key = decodeURIComponent(url.pathname.slice("/api/product-images/".length));
    if (!isValidProductImageKey(key)) throw new Error("invalid stored product image key");
    const object = await getContentMediaBucket().get(`product-images/${key}`);
    if (!object?.body) throw new Error("stored product image unavailable");
    return object.body;
  }

  // Calling fetch() against this Worker's own custom domain recursively fails
  // at the edge. Read bundled product artwork through the static-assets binding
  // instead; only the path is used by ASSETS.fetch().
  if (isGhostCartHost) {
    const source = await env.ASSETS.fetch(new Request(`https://assets.local${url.pathname}`));
    if (!source.ok || !source.body) throw new Error("bundled product image unavailable");
    return source.body;
  }

  const source = await fetch(imageUrl, { redirect: "follow" });
  if (!source.ok || !source.body) throw new Error("remote product image unavailable");
  return source.body;
}

export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  const { id: token } = await context.params;
  if (!isGiftToken(token)) return new Response(null, { status: 404 });
  const row = await getD1().prepare(
    `SELECT a.image_url AS imageUrl
     FROM ghost_gifts g INNER JOIN almost_buys a ON a.id = g.almost_buy_id
     WHERE g.token_hash = ? AND g.status IN ('pending', 'revealed')
       AND g.expires_at > CURRENT_TIMESTAMP LIMIT 1`,
  ).bind(await hashGiftToken(token)).first<{ imageUrl: string | null }>();
  if (!row?.imageUrl || !isAllowedProductImageUrl(row.imageUrl)) return new Response(null, { status: 404 });
  try {
    const source = await readSourceImage(row.imageUrl);
    const transformed = await env.IMAGES.input(source)
      .transform({ width: 640 })
      .transform({ blur: 30 })
      .output({ format: "image/jpeg", quality: 58 });
    const response = await transformed.response();
    const headers = new Headers(response.headers);
    headers.set("Cache-Control", "private, no-store, max-age=0");
    headers.set("X-Content-Type-Options", "nosniff");
    return new Response(response.body, { status: response.status, headers });
  } catch (error) {
    console.error("gift teaser image transform failed", error);
    return new Response(null, { status: 404 });
  }
}
