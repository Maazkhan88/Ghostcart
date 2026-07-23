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
    // Ghost Cart's own vocabulary: "cooled & saved" = an almost-buy explicitly
    // resolved "skipped" after cooling off (this is what ranks the board -
    // it's the impulse-resistance achievement). "Ghosted" = the separate,
    // unrelated case of actually finishing checkout on an almost-buy
    // (resolved_bought) - completing the purchase, not resisting it. Do not
    // conflate the two; a prior version of this endpoint incorrectly labeled
    // the skip-count as "ghostedCount".
    const rows = await db
      .select({
        username: users.username,
        avatarKey: users.avatarKey,
        moneyKeptCents: sql<number>`coalesce(sum(${almostBuys.confirmedMoneyKeptCents}), 0)`,
        savedCount: sql<number>`count(case when ${almostBuys.state} = 'resolved_skipped' then 1 end)`,
        ghostedCount: sql<number>`count(case when ${almostBuys.state} = 'resolved_bought' then 1 end)`,
        ghostedAmountCents: sql<number>`coalesce(sum(case when ${almostBuys.state} = 'resolved_bought' then ${almostBuys.almostSpentCents} else 0 end), 0)`,
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
        savedCount: Number(row.savedCount),
        ghostedCount: Number(row.ghostedCount),
        ghostedAmountCents: Number(row.ghostedAmountCents),
      })),
    });
  } catch (error) {
    return jsonNoStore(
      { error: error instanceof Error ? error.message : "Leaderboard is temporarily unavailable." },
      { status: 500 },
    );
  }
}
