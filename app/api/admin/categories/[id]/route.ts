import { eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { categories } from "../../../../../db/schema";
import { requireAdminApiUser } from "../../../../../lib/admin-auth";
import { parseId, toRouteErrorMessage } from "../../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The categories table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

// Renaming here does not rewrite the (unrelated, plain-text) category field
// on any existing products/community products - it only changes what shows
// up as a picklist option going forward. Same reasoning as DELETE below.
export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const payload = (await request.json()) as { name?: unknown };
    const name = typeof payload.name === "string" ? payload.name.trim() : "";
    if (!name) {
      return Response.json({ error: "name is required" }, { status: 400 });
    }

    const db = getDb();
    const [category] = await db
      .update(categories)
      .set({ name })
      .where(eq(categories.id, id))
      .returning();

    if (!category) {
      return Response.json({ error: "category not found" }, { status: 404 });
    }
    return Response.json({ category });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    return Response.json(
      { error: isDuplicate ? "This category already exists" : message },
      { status: isDuplicate ? 409 : 500 },
    );
  }
}

// Deliberately does not touch products/community_products - removing a
// category from the picklist never deletes or reassigns products already
// using that text value, it just stops appearing as a future option.
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
    const [category] = await db.delete(categories).where(eq(categories.id, id)).returning();
    if (!category) {
      return Response.json({ error: "category not found" }, { status: 404 });
    }
    return Response.json({ category });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
