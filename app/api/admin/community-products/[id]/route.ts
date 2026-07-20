import { eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { communityProducts } from "../../../../../db/schema";
import { requireAdminApiUser } from "../../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The community_products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

const STATUSES = new Set(["visible", "pending", "hidden"]);

// Moderation: hide/unhide a community product without deleting its history.
export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id } = await params;

  try {
    const payload = (await request.json()) as { status?: unknown };
    if (typeof payload.status !== "string" || !STATUSES.has(payload.status)) {
      return Response.json(
        { error: "status must be one of visible, pending, hidden" },
        { status: 400 },
      );
    }

    const db = getDb();
    const [product] = await db
      .update(communityProducts)
      .set({ status: payload.status, updatedAt: new Date().toISOString() })
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
