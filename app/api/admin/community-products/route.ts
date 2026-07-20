import { desc, eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { communityProducts } from "../../../../db/schema";
import { requireAdminApiUser } from "../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";
import {
  readNonNegativeInteger,
  readSafeUrl,
  sanitizeShortText,
} from "../../../../lib/backend-contract";
import {
  canonicalizeRetailerUrl,
  isAllowedProductImageUrl,
} from "../../../../lib/product-link-preview";

const MISSING_TABLE_HINT =
  "The community_products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

// Admin-only, unfiltered view of every community product regardless of
// status ('visible'/'pending'/'hidden') - the public /api/community-products
// route only ever returns 'visible' rows and never exposes moderation state.
export async function GET() {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const db = getDb();
    const rows = await db
      .select()
      .from(communityProducts)
      .orderBy(desc(communityProducts.lastGhostedAt))
      .limit(500);

    return Response.json(
      { communityProducts: rows },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

// Admin-created community product - same validation as the user-facing
// POST /api/community-products (safe HTTPS link, sanitized text, non-
// negative price, allowed image host) but skips consent/rate-limiting,
// which only make sense for a real user's own share action.
export async function POST(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as Record<string, unknown>;

    let canonicalUrl: URL;
    try {
      canonicalUrl = canonicalizeRetailerUrl(
        typeof payload.sourceUrl === "string" ? payload.sourceUrl : "",
      );
    } catch (error) {
      return Response.json(
        { error: error instanceof Error ? error.message : "Invalid source URL" },
        { status: 400 },
      );
    }

    const title = sanitizeShortText(payload.title, "title", 160, { required: true });
    const category = sanitizeShortText(payload.category ?? "Other", "category", 80, { required: true });
    const price = readNonNegativeInteger(payload.priceCents ?? 0, "priceCents");
    const image = readSafeUrl(payload.imageUrl, "imageUrl");
    const fieldError = title.error || category.error || price.error || image.error;
    if (fieldError) return Response.json({ error: fieldError }, { status: 400 });
    if (image.value && !isAllowedProductImageUrl(image.value)) {
      return Response.json({ error: "Product image must use a safe public HTTPS URL" }, { status: 400 });
    }

    const canonicalKey = await sha256Hex(canonicalUrl.toString());
    const db = getDb();

    const [existing] = await db
      .select({ id: communityProducts.id })
      .from(communityProducts)
      .where(eq(communityProducts.canonicalKey, canonicalKey))
      .limit(1);
    if (existing) {
      return Response.json(
        { error: "A community product for this link already exists" },
        { status: 409 },
      );
    }

    const id = `ugc_${canonicalKey.slice(0, 24)}`;
    const [product] = await db
      .insert(communityProducts)
      .values({
        id,
        canonicalKey,
        canonicalUrl: canonicalUrl.toString(),
        sourceDomain: canonicalUrl.hostname,
        title: title.value,
        category: category.value,
        imageUrl: image.value ?? null,
        priceCents: price.value ?? 0,
        status: "visible",
      })
      .returning();

    return Response.json({ communityProduct: product }, { status: 201 });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    return Response.json(
      { error: isDuplicate ? "A community product for this link already exists" : message },
      { status: isDuplicate ? 409 : 500 },
    );
  }
}
