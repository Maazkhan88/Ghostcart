import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { products } from "../../../../db/schema";
import { parseId, slugify, toRouteErrorMessage } from "../../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../../lib/admin-auth";

const MISSING_TABLE_HINT =
  "The products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const db = getDb();
    const [product] = await db
      .select()
      .from(products)
      .where(eq(products.id, id))
      .limit(1);

    if (!product) {
      return Response.json({ error: "product not found" }, { status: 404 });
    }
    return Response.json({ product });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 }
    );
  }
}

export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const payload = (await request.json()) as Partial<{
      merchantId: number;
      name: string;
      category: string;
      description: string;
      priceCents: number;
      imageUrl: string | null;
      isFlashDeal: boolean;
      isMostGhosted: boolean;
      isActive: boolean;
      slug: string;
    }>;

    const updates: Record<string, unknown> = {};
    if (payload.merchantId !== undefined) {
      if (!Number.isInteger(payload.merchantId)) {
        return Response.json({ error: "merchantId must be an integer" }, { status: 400 });
      }
      updates.merchantId = payload.merchantId;
    }
    if (payload.name !== undefined) updates.name = payload.name.trim();
    if (payload.category !== undefined) updates.category = payload.category.trim();
    if (payload.description !== undefined) updates.description = payload.description.trim();
    if (payload.imageUrl !== undefined) updates.imageUrl = payload.imageUrl?.trim() || null;
    if (payload.isFlashDeal !== undefined) updates.isFlashDeal = payload.isFlashDeal;
    if (payload.isMostGhosted !== undefined) updates.isMostGhosted = payload.isMostGhosted;
    if (payload.isActive !== undefined) updates.isActive = payload.isActive;
    if (payload.slug !== undefined) updates.slug = slugify(payload.slug);
    if (payload.priceCents !== undefined) {
      if (!Number.isInteger(payload.priceCents) || payload.priceCents < 0) {
        return Response.json(
          { error: "priceCents must be a non-negative integer" },
          { status: 400 }
        );
      }
      updates.priceCents = payload.priceCents;
    }

    if (Object.keys(updates).length === 0) {
      return Response.json({ error: "no fields to update" }, { status: 400 });
    }
    updates.updatedAt = new Date().toISOString();

    const db = getDb();
    const [product] = await db
      .update(products)
      .set(updates)
      .where(eq(products.id, id))
      .returning();

    if (!product) {
      return Response.json({ error: "product not found" }, { status: 404 });
    }
    return Response.json({ product });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    const isBadMerchant = message.includes("FOREIGN KEY constraint failed");
    const status = isDuplicate ? 409 : isBadMerchant ? 400 : 500;
    const friendly = isDuplicate
      ? "a product with this slug already exists"
      : isBadMerchant
        ? "merchantId does not reference an existing merchant"
        : message;

    return Response.json({ error: friendly }, { status });
  }
}

export async function DELETE(_request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const db = getDb();
    const [product] = await db
      .delete(products)
      .where(eq(products.id, id))
      .returning();

    if (!product) {
      return Response.json({ error: "product not found" }, { status: 404 });
    }
    return Response.json({ product });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 }
    );
  }
}
