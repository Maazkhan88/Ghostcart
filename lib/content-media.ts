import { env } from "cloudflare:workers";
import {
  IMAGE_EXTENSIONS,
  IMAGE_MIME_TYPES,
  MAX_IMAGE_DIMENSION,
  MAX_UPLOAD_BYTES,
  readImageDimensions,
  sniffImageType,
  stripImageMetadata,
} from "./image-processing";

// R2 storage for admin-uploaded editorial media (banners, "Ghost Cart
// Stories" cards, product photos). Object keys are always server-generated -
// callers never accept a client-supplied filename as (or as part of) the key.

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

const KEY_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(png|jpg|mp4)$/;

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

export type UploadImageResult =
  | { ok: true; key: string }
  | { ok: false; error: string; status: number };

// Shared validate-then-store pipeline: real magic-byte content sniffing
// (never the claimed MIME type/extension - rejects a renamed .svg or any
// other disguised file), a size limit, a dimension limit, and EXIF/metadata
// stripping before the bytes ever reach R2. Used by every admin image
// upload route (content blocks, product photos) so the same rules apply
// everywhere rather than being re-implemented per route.
export async function uploadImageFile(file: File): Promise<UploadImageResult> {
  if (file.size > MAX_UPLOAD_BYTES) {
    return { ok: false, error: `file exceeds the ${MAX_UPLOAD_BYTES / 1_000_000}MB limit`, status: 400 };
  }

  const bytes = new Uint8Array(await file.arrayBuffer());

  const sniffedType = sniffImageType(bytes);
  if (!sniffedType) {
    return {
      ok: false,
      error: "file must be a genuine PNG or JPEG image (SVG and other formats are not accepted)",
      status: 400,
    };
  }

  const dimensions = readImageDimensions(bytes, sniffedType);
  if (!dimensions) {
    return { ok: false, error: "could not read image dimensions", status: 400 };
  }
  if (dimensions.width > MAX_IMAGE_DIMENSION || dimensions.height > MAX_IMAGE_DIMENSION) {
    return {
      ok: false,
      error: `image dimensions must not exceed ${MAX_IMAGE_DIMENSION}px per side`,
      status: 400,
    };
  }

  const stripped = stripImageMetadata(bytes, sniffedType);
  const key = generateContentMediaKey(IMAGE_EXTENSIONS[sniffedType]);
  await putContentMedia(key, stripped, IMAGE_MIME_TYPES[sniffedType]);
  return { ok: true, key };
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
