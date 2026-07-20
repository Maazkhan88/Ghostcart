import { desc } from "drizzle-orm";
import { getDb } from "../../../../db";
import { communityProducts } from "../../../../db/schema";
import { requireAdminApiUser } from "../../../../lib/admin-auth";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The community_products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

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
