import { desc, eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { almostBuys, users } from "../../../../db/schema";
import { requireAdminApiUser } from "../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The almost_buys table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

// Every user's ghosted/almost-bought items, newest-updated first, with the
// owning user's email attached - there is no other admin-facing view of
// this table today.
export async function GET() {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const db = getDb();
    const rows = await db
      .select({
        id: almostBuys.id,
        userId: almostBuys.userId,
        userEmail: users.email,
        title: almostBuys.title,
        category: almostBuys.category,
        state: almostBuys.state,
        sourceKind: almostBuys.sourceKind,
        currencyCode: almostBuys.currencyCode,
        almostSpentCents: almostBuys.almostSpentCents,
        confirmedMoneyKeptCents: almostBuys.confirmedMoneyKeptCents,
        capturedAt: almostBuys.capturedAt,
        updatedAt: almostBuys.updatedAt,
      })
      .from(almostBuys)
      .innerJoin(users, eq(almostBuys.userId, users.id))
      .orderBy(desc(almostBuys.updatedAt))
      .limit(500);

    return Response.json({ ghostActivity: rows }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
