import { env } from "cloudflare:workers";

// R2 storage for admin-uploaded editorial media (banners, "Ghost Cart
// Stories" cards). Object keys are always server-generated - callers never
// accept a client-supplied filename as (or as part of) the key.

export function getContentMediaBucket(): R2Bucket {
  if (!env.CONTENT_MEDIA) {
    throw new Error(
      "Cloudflare R2 binding `CONTENT_MEDIA` is unavailable. Run `npx wrangler r2 bucket create ghostcart-content-media` once, then deploy so the platform can bind it.",
    );
  }
  return env.CONTENT_MEDIA;
}

// No folder prefix: the bucket is dedicated to this feature, and a flat key
// keeps the public image route's dynamic segment simple (no slash to encode).
export function generateContentMediaKey(extension: string): string {
  return `${crypto.randomUUID()}.${extension}`;
}

const KEY_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(png|jpg)$/;

export function isValidContentMediaKey(key: string): boolean {
  return KEY_PATTERN.test(key);
}

export async function putContentMedia(
  key: string,
  bytes: Uint8Array,
  contentType: string,
): Promise<void> {
  const bucket = getContentMediaBucket();
  await bucket.put(key, bytes, {
    httpMetadata: { contentType },
  });
}

export async function deleteContentMedia(key: string): Promise<void> {
  try {
    const bucket = getContentMediaBucket();
    await bucket.delete(key);
  } catch (error) {
    // The D1 row is always deleted first by callers, so a failure here only
    // leaves an orphaned R2 object (wasted storage) - never a dangling
    // reference. Not worth failing the whole request over.
    console.error("Failed to delete content media object", key, error);
  }
}
