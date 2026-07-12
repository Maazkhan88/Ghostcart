import { desc } from "drizzle-orm";
import { getDb } from "../../../db";
import { merchants } from "../../../db/schema";
import { slugify, toRouteErrorMessage } from "../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../lib/admin-auth";

const MISSING_TABLE_HINT =
  "The merchants table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

export async function GET() {
  try {
    const db = getDb();
    const rows = await db
      .select()
      .from(merchants)
      .orderBy(desc(merchants.createdAt), desc(merchants.id));

    return Response.json({ merchants: rows });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as {
      name?: string;
      category?: string;
      description?: string;
      logoUrl?: string;
      isSponsored?: boolean;
      slug?: string;
    };

    const name = payload.name?.trim() ?? "";
    const category = payload.category?.trim() ?? "";

    if (!name) {
      return Response.json({ error: "name is required" }, { status: 400 });
    }
    if (!category) {
      return Response.json({ error: "category is required" }, { status: 400 });
    }

    const slug = slugify(payload.slug?.trim() || name);
    if (!slug) {
      return Response.json(
        { error: "could not derive a slug from name" },
        { status: 400 }
      );
    }

    const db = getDb();
    const [merchant] = await db
      .insert(merchants)
      .values({
        name,
        slug,
        category,
        description: payload.description?.trim() ?? "",
        logoUrl: payload.logoUrl?.trim() || null,
        isSponsored: payload.isSponsored ?? false,
      })
      .returning();

    return Response.json({ merchant }, { status: 201 });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    return Response.json(
      { error: isDuplicate ? "a merchant with this slug already exists" : message },
      { status: isDuplicate ? 409 : 500 }
    );
  }
}
