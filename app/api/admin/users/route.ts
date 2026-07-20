import { desc, sql } from "drizzle-orm";
import { getDb } from "../../../../db";
import { almostBuys, users } from "../../../../db/schema";
import { requireAdminApiUser } from "../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The users table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

export async function GET() {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const db = getDb();
    const rows = await db
      .select({
        id: users.id,
        email: users.email,
        displayName: users.displayName,
        isAdmin: users.isAdmin,
        createdAt: users.createdAt,
        almostBuyCount: sql<number>`(select count(*) from ${almostBuys} where ${almostBuys.userId} = ${users.id})`,
      })
      .from(users)
      .orderBy(desc(users.createdAt))
      .limit(500);

    return Response.json({ users: rows }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
