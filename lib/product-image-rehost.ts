import { getContentMediaBucket } from "./content-media";
import { isAllowedProductImageUrl, sha256Hex } from "./product-link-preview";

// Reuses the existing CONTENT_MEDIA bucket (already deployed and bound - see
// lib/content-media.ts) rather than provisioning a separate bucket, just
// namespaced under a distinct key prefix. Trade-off, made deliberately: this
// mixes admin-curated editorial media with scraped-and-rehosted retailer
// photos in the same bucket. Avoiding that would need a second R2 binding
// (`wrangler r2 bucket create` + a new binding in wrangler.ghostcart-app.jsonc
// + a deploy) for what's otherwise a pure key-prefix separation - not
// justified today. The prefix alone is enough to enumerate/purge
// product-images/* separately later if that mixing ever becomes a problem.
const PRODUCT_IMAGE_KEY_PREFIX = "product-images/";
const PRODUCT_IMAGE_ORIGIN = "https://theghostcart.com";
const FETCH_TIMEOUT_MS = 6_000;
const MAX_IMAGE_BYTES = 8_000_000;

const EXTENSION_BY_CONTENT_TYPE: Record<string, string> = {
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/webp": "webp",
};

export function isValidProductImageKey(key: string): boolean {
  return /^[0-9a-f]{64}\.(jpg|png|webp)$/.test(key);
}

async function readLimitedBytes(response: Response): Promise<Uint8Array | null> {
  const declaredLength = Number(response.headers.get("content-length") ?? 0);
  if (declaredLength > MAX_IMAGE_BYTES) return null;
  if (!response.body) return null;
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > MAX_IMAGE_BYTES) {
      await reader.cancel();
      return null;
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  return bytes;
}

// Downloads a scraped product-image URL (already validated by
// isAllowedProductImageUrl upstream) and stores a permanent copy in R2,
// returning our own hosted URL instead of the original hotlink. Amazon's CDN
// URLs can go dead, get hotlink-blocked, or simply change when a listing is
// re-scraped later - a copy we own doesn't have any of those failure modes,
// which also makes the "reuse a prior capture's image" fallback
// (app/api/link-preview/route.ts) permanently reliable instead of only as
// reliable as whatever Amazon happens to still be serving.
//
// Deliberately never throws: any failure (network, bad content-type, over
// the size cap, R2 write failure) falls back to the original URL, so a
// hiccup here degrades to today's behavior rather than losing the image.
export async function rehostProductImage(sourceImageUrl: string): Promise<string> {
  if (!isAllowedProductImageUrl(sourceImageUrl)) return sourceImageUrl;
  try {
    const sourceHost = new URL(sourceImageUrl).hostname;
    if (sourceHost === "theghostcart.com" || sourceHost === "www.theghostcart.com") {
      return sourceImageUrl; // already ours - avoid a pointless re-download/re-upload
    }

    const hash = await sha256Hex(sourceImageUrl);
    const bucket = getContentMediaBucket();

    // Content-addressed by source URL: a repeat capture of the same product
    // image is a free hit (no re-download, no duplicate object) - the same
    // property that makes the link-preview reuse fallback work well here too.
    for (const ext of Object.values(EXTENSION_BY_CONTENT_TYPE)) {
      const key = `${PRODUCT_IMAGE_KEY_PREFIX}${hash}.${ext}`;
      const existing = await bucket.head(key);
      if (existing) return `${PRODUCT_IMAGE_ORIGIN}/api/product-images/${hash}.${ext}`;
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);
    let response: Response;
    try {
      response = await fetch(sourceImageUrl, {
        signal: controller.signal,
        headers: {
          Accept: "image/*",
          "User-Agent": "GhostCartLinkPreview/2.2 (+https://theghostcart.com)",
        },
      });
    } finally {
      clearTimeout(timeout);
    }
    if (!response.ok) return sourceImageUrl;

    const contentType = response.headers.get("content-type")?.split(";")[0]?.trim().toLowerCase() ?? "";
    const ext = EXTENSION_BY_CONTENT_TYPE[contentType];
    if (!ext) return sourceImageUrl; // not a recognized image type - keep the raw hotlink rather than store junk

    const bytes = await readLimitedBytes(response);
    if (!bytes || bytes.byteLength === 0) return sourceImageUrl;

    await bucket.put(`${PRODUCT_IMAGE_KEY_PREFIX}${hash}.${ext}`, bytes, { httpMetadata: { contentType } });
    return `${PRODUCT_IMAGE_ORIGIN}/api/product-images/${hash}.${ext}`;
  } catch (error) {
    console.error(`[product-image-rehost] failed, keeping raw hotlink: ${error instanceof Error ? error.message : String(error)}`);
    return sourceImageUrl;
  }
}
