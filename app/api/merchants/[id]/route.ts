import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { merchants } from "../../../../db/schema";
import { parseId, slugify, toRouteErrorMessage } from "../../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../../lib/admin-auth";

const MISSING_TABLE_HINT =
  "The merchants table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const db = getDb();
    const [merchant] = await db
      .select()
      .from(merchants)
      .where(eq(merchants.id, id))
      .limit(1);

    if (!merchant) {
      return Response.json({ error: "merchant not found" }, { status: 404 });
    }
    return Response.json({ merchant });
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
      name: string;
      category: string;
      description: string;
      logoUrl: string | null;
      isSponsored: boolean;
      slug: string;
    }>;

    const updates: Record<string, unknown> = {};
    if (payload.name !== undefined) updates.name = payload.name.trim();
    if (payload.category !== undefined) updates.category = payload.category.trim();
    if (payload.description !== undefined) updates.description = payload.description.trim();
    if (payload.logoUrl !== undefined) updates.logoUrl = payload.logoUrl?.trim() || null;
    if (payload.isSponsored !== undefined) updates.isSponsored = payload.isSponsored;
    if (payload.slug !== undefined) updates.slug = slugify(payload.slug);

    if (Object.keys(updates).length === 0) {
      return Response.json({ error: "no fields to update" }, { status: 400 });
    }
    updates.updatedAt = new Date().toISOString();

    const db = getDb();
    const [merchant] = await db
      .update(merchants)
      .set(updates)
      .where(eq(merchants.id, id))
      .returning();

    if (!merchant) {
      return Response.json({ error: "merchant not found" }, { status: 404 });
    }
    return Response.json({ merchant });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    return Response.json(
      { error: isDuplicate ? "a merchant with this slug already exists" : message },
      { status: isDuplicate ? 409 : 500 }
    );
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
    const [merchant] = await db
      .delete(merchants)
      .where(eq(merchants.id, id))
      .returning();

    if (!merchant) {
      return Response.json({ error: "merchant not found" }, { status: 404 });
    }
    return Response.json({ merchant });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 }
    );
  }
}
