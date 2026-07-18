import { readNonNegativeInteger, readSafeUrl, sanitizeShortText } from "../../../lib/backend-contract";
import { isAllowedProductImageUrl } from "../../../lib/product-link-preview";
import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";
import { ensureSharedGhostItemsTable } from "../../../lib/shared-ghost-items";

const SHARE_ID_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "no-store");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}

function shareId() {
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  return Array.from(bytes, (byte) => SHARE_ID_ALPHABET[byte % SHARE_ID_ALPHABET.length]).join("");
}

function fallbackProductImage(title: string, category: string, origin: string): string | null {
  const value = `${title} ${category}`.toLowerCase();
  const file = value.includes("latte") || value.includes("coffee")
    ? "latte.png"
    : value.includes("burger") || value.includes("meal")
      ? "burger.png"
      : value.includes("perfume") || value.includes("cologne")
        ? "perfume.png"
        : value.includes("sneaker") || value.includes("shoe")
          ? "sneaker.png"
          : value.includes("earbud") || value.includes("headphone")
            ? "earbuds.png"
            : value.includes("smartwatch") || value.includes("watch")
              ? "smartwatch.png"
              : value.includes("tablet")
                ? "tablet.png"
                : null;
  return file ? new URL(`/products/${file}`, origin).toString() : null;
}

export async function POST(request: Request) {
  try {
    const actorHash = await requestActorHash(request);
    const rateLimit = await consumeRateLimit({
      namespace: "share-items",
      actorHash,
      cost: 1,
      limit: 40,
      windowSeconds: 60 * 60,
    });
    if (!rateLimit.allowed) {
      return json({ error: "Too many share links. Try again later." }, { status: 429 });
    }

    const payload = (await request.json()) as Record<string, unknown>;
    const title = sanitizeShortText(payload.title, "title", 160, { required: true });
    const category = sanitizeShortText(payload.category ?? "Almost-buy", "category", 80, { required: true });
    const price = readNonNegativeInteger(payload.priceCents ?? 0, "priceCents");
    const image = readSafeUrl(payload.imageUrl, "imageUrl");
    const source = readSafeUrl(payload.sourceUrl, "sourceUrl");
    const error = title.error || category.error || price.error || image.error || source.error;
    if (error) return json({ error }, { status: 400 });
    if (image.value && !isAllowedProductImageUrl(image.value)) {
      return json({ error: "Product image must use a safe public HTTPS URL" }, { status: 400 });
    }

    const id = shareId();
    const now = new Date();
    const expiresAt = new Date(now.getTime() + 180 * 24 * 60 * 60 * 1000).toISOString();
    const imageUrl = image.value ?? fallbackProductImage(title.value!, category.value!, new URL(request.url).origin);
    const db = await ensureSharedGhostItemsTable();
    await db
      .prepare(
        `INSERT INTO shared_ghost_items
          (id, title, category, price_cents, image_url, source_url, created_at, expires_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      )
      .bind(id, title.value, category.value, price.value ?? 0, imageUrl, source.value ?? null, now.toISOString(), expiresAt)
      .run();

    const origin = new URL(request.url).origin;
    return json({ id, url: `${origin}/ghost?s=${id}` }, { status: 201 });
  } catch {
    return json({ error: "Unable to create a short Ghost link" }, { status: 503 });
  }
}
