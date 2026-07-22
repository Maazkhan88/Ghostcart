import { eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { communityProducts } from "../../../../../db/schema";
import { requireAdminApiUser } from "../../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The community_products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

const STATUSES = new Set(["visible", "pending", "hidden"]);

// Supports both moderation (status only) and full edits (title/category/
// price/image) - same route, whichever fields are present in the body get
// updated. Never touches canonicalKey/canonicalUrl/sourceDomain/ghostCount,
// which are derived from the original share, not admin-editable.
export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id } = await params;

  try {
    const payload = (await request.json()) as Partial<{
      status: string;
      title: string;
      category: string;
      imageUrl: string | null;
      priceCents: number;
      currencyCode: string;
    }>;

    const updates: Record<string, unknown> = {};
    if (payload.status !== undefined) {
      if (typeof payload.status !== "string" || !STATUSES.has(payload.status)) {
        return Response.json(
          { error: "status must be one of visible, pending, hidden" },
          { status: 400 },
        );
      }
      updates.status = payload.status;
    }
    if (payload.title !== undefined) {
      const title = String(payload.title).trim();
      if (!title) return Response.json({ error: "title cannot be empty" }, { status: 400 });
      updates.title = title;
    }
    if (payload.category !== undefined) {
      updates.category = String(payload.category).trim() || "Other";
    }
    if (payload.imageUrl !== undefined) {
      updates.imageUrl = payload.imageUrl ? String(payload.imageUrl).trim() : null;
    }
    if (payload.priceCents !== undefined) {
      const priceCents = Number(payload.priceCents);
      if (!Number.isFinite(priceCents) || priceCents < 0) {
        return Response.json({ error: "priceCents must be a non-negative number" }, { status: 400 });
      }
      updates.priceCents = Math.round(priceCents);
    }
    if (payload.currencyCode !== undefined) {
      updates.currencyCode = String(payload.currencyCode).trim().toUpperCase() || "AED";
    }

    if (Object.keys(updates).length === 0) {
      return Response.json({ error: "no fields to update" }, { status: 400 });
    }
    updates.updatedAt = new Date().toISOString();

    const db = getDb();
    const [product] = await db
      .update(communityProducts)
      .set(updates)
      .where(eq(communityProducts.id, id))
      .returning();

    if (!product) {
      return Response.json({ error: "community product not found" }, { status: 404 });
    }
    return Response.json({ communityProduct: product });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

export async function DELETE(_request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id } = await params;

  try {
    const db = getDb();
    const [product] = await db
      .delete(communityProducts)
      .where(eq(communityProducts.id, id))
      .returning();

    if (!product) {
      return Response.json({ error: "community product not found" }, { status: 404 });
    }
    return Response.json({ communityProduct: product });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
