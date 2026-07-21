import { desc, eq, sql } from "drizzle-orm";
import { getDb } from "../../../../db";
import { almostBuys, users } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";

const MAX_ENTRIES = 50;

// Public, unauthenticated: only users who explicitly opted in
// (communityConsent = true) appear here, and only their username/avatar -
// never email or any other account field. Withdrawing consent removes a
// user from this list immediately (see PATCH /api/me/profile).
export async function GET() {
  try {
    const db = getDb();
    const rows = await db
      .select({
        username: users.username,
        avatarKey: users.avatarKey,
        moneyKeptCents: sql<number>`coalesce(sum(${almostBuys.confirmedMoneyKeptCents}), 0)`,
        ghostedCount: sql<number>`count(case when ${almostBuys.state} = 'resolved_skipped' then 1 end)`,
      })
      .from(users)
      .leftJoin(almostBuys, eq(almostBuys.userId, users.id))
      .where(sql`${users.communityConsent} = 1 and ${users.username} is not null`)
      .groupBy(users.id)
      .orderBy(desc(sql`coalesce(sum(${almostBuys.confirmedMoneyKeptCents}), 0)`))
      .limit(MAX_ENTRIES);

    return jsonNoStore({
      leaderboard: rows.map((row) => ({
        username: row.username,
        avatarUrl: row.avatarKey ? `/api/content-blocks/image/${row.avatarKey}` : null,
        moneyKeptCents: Number(row.moneyKeptCents),
        ghostedCount: Number(row.ghostedCount),
      })),
    });
  } catch (error) {
    return jsonNoStore(
      { error: error instanceof Error ? error.message : "Leaderboard is temporarily unavailable." },
      { status: 500 },
    );
  }
}
