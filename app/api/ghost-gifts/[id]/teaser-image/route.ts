import { env } from "cloudflare:workers";
import { getD1 } from "../../../../../db";
import { hashGiftToken, isGiftToken } from "../../../../../lib/ghost-gifts";
import { isAllowedProductImageUrl } from "../../../../../lib/product-link-preview";

export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  const { id: token } = await context.params;
  if (!isGiftToken(token)) return new Response(null, { status: 404 });
  const row = await getD1().prepare(
    `SELECT a.image_url AS imageUrl
     FROM ghost_gifts g INNER JOIN almost_buys a ON a.id = g.almost_buy_id
     WHERE g.token_hash = ? AND g.status IN ('pending', 'revealed')
       AND g.expires_at > CURRENT_TIMESTAMP LIMIT 1`,
  ).bind(await hashGiftToken(token)).first<{ imageUrl: string | null }>();
  if (!row?.imageUrl || !isAllowedProductImageUrl(row.imageUrl)) {
    return Response.redirect("https://theghostcart.com/mascot/mascot-cart.png", 302);
  }
  try {
    const source = await fetch(row.imageUrl, { redirect: "follow" });
    if (!source.ok || !source.body) throw new Error("image unavailable");
    const transformed = await env.IMAGES.input(source.body)
      .transform({ width: 640, blur: 30 })
      .output({ format: "image/jpeg", quality: 58 });
    const response = await transformed.response();
    const headers = new Headers(response.headers);
    headers.set("Cache-Control", "private, no-store, max-age=0");
    headers.set("X-Content-Type-Options", "nosniff");
    return new Response(response.body, { status: response.status, headers });
  } catch {
    return Response.redirect("https://theghostcart.com/mascot/mascot-cart.png", 302);
  }
}
