import { getContentMediaBucket, isValidContentMediaKey } from "../../../../../lib/content-media";

type RouteContext = { params: Promise<{ key: string }> };

// Public, unauthenticated, read-only: content-block images are editorial
// marketing assets (banners, "Ghost Cart Stories") meant to be visible to
// every app user, not private data. Deletion/creation stay admin-gated in
// app/api/content-blocks/route.ts and [id]/route.ts.
export async function GET(_request: Request, { params }: RouteContext) {
  const { key } = await params;
  if (!isValidContentMediaKey(key)) {
    return Response.json({ error: "invalid image key" }, { status: 400 });
  }

  const bucket = getContentMediaBucket();
  const object = await bucket.get(key);
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
