import { asc } from "drizzle-orm";
import { getDb } from "../../../../db";
import { categories } from "../../../../db/schema";
import { requireAdminApiUser } from "../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The categories table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

// Public read (the Products/Community forms' dropdowns need this without
// requiring an admin session), admin-gated write.
export async function GET() {
  try {
    const db = getDb();
    const rows = await db.select().from(categories).orderBy(asc(categories.name));
    return Response.json({ categories: rows }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

export async function POST(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as { name?: unknown };
    const name = typeof payload.name === "string" ? payload.name.trim() : "";
    if (!name) {
      return Response.json({ error: "name is required" }, { status: 400 });
    }
    if (name.length > 80) {
      return Response.json({ error: "name must be at most 80 characters" }, { status: 400 });
    }

    const db = getDb();
    const [category] = await db.insert(categories).values({ name }).returning();
    return Response.json({ category }, { status: 201 });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    return Response.json(
      { error: isDuplicate ? "This category already exists" : message },
      { status: isDuplicate ? 409 : 500 },
    );
  }
}
