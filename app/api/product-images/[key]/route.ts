import { getContentMediaBucket } from "../../../../lib/content-media";
import { isValidProductImageKey } from "../../../../lib/product-image-rehost";

type RouteContext = { params: Promise<{ key: string }> };

// Public, unauthenticated, read-only: these are re-hosted copies of product
// photos that were already publicly visible on the source retailer's page -
// no more sensitive than the raw hotlink they replace. Written only by
// rehostProductImage() (lib/product-image-rehost.ts), keyed by
// sha256(sourceUrl), so this is safe to cache forever - the same source
// image always resolves to the same object.
export async function GET(_request: Request, { params }: RouteContext) {
  const { key } = await params;
  if (!isValidProductImageKey(key)) {
    return Response.json({ error: "invalid image key" }, { status: 400 });
  }

  const bucket = getContentMediaBucket();
  const object = await bucket.get(`product-images/${key}`);
  if (!object) {
    return Response.json({ error: "image not found" }, { status: 404 });
  }

  return new Response(object.body, {
    headers: {
      "content-type": object.httpMetadata?.contentType ?? "application/octet-stream",
      "cache-control": "public, max-age=31536000, immutable",
    },
  });
}
