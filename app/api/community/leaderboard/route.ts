import { getD1 } from "../../../../db";
import { jsonNoStore } from "../../../../lib/almost-buy-api";

const MAX_ENTRIES = 50;

type LeaderboardRow = {
  username: string;
  avatarKey: string | null;
  moneyKeptCents: number;
  savedCount: number;
  ghostedCount: number;
  ghostedAmountCents: number;
};

// Public, unauthenticated: only users who explicitly opted in
// (communityConsent = true) appear here, and only their username/avatar -
// never email or any other account field. Withdrawing consent removes a
// user from this list immediately (see PATCH /api/me/profile).
//
// Ghost Cart's own vocabulary: "cooled & saved" = an almost-buy explicitly
// resolved "skipped" after cooling off (this is what ranks the board - it's
// the impulse-resistance achievement). "Ghosted" = actually finishing a
// purchase - from two independent sources that must both count: (a)
// resolving an almost-buy as "bought intentionally" after cooling, and (b)
// completing a marketplace-cart simulated checkout (simulated_orders,
// recorded by POST /api/me/simulated-orders). Correlated subqueries, not
// joins, because joining both one-to-many tables at once would fan out and
// inflate every sum/count.
export async function GET() {
  try {
    const db = getD1();
    const result = await db
      .prepare(
        `SELECT
           u.username AS username,
           u.avatar_key AS avatarKey,
           (SELECT COALESCE(SUM(confirmed_money_kept_cents), 0) FROM almost_buys WHERE user_id = u.id) AS moneyKeptCents,
           (SELECT COUNT(*) FROM almost_buys WHERE user_id = u.id AND state = 'resolved_skipped') AS savedCount,
           (
             (SELECT COUNT(*) FROM almost_buys WHERE user_id = u.id AND state = 'resolved_bought')
             + (SELECT COUNT(*) FROM simulated_orders WHERE user_id = u.id)
           ) AS ghostedCount,
           (
             (SELECT COALESCE(SUM(almost_spent_cents), 0) FROM almost_buys WHERE user_id = u.id AND state = 'resolved_bought')
             + (SELECT COALESCE(SUM(total_cents), 0) FROM simulated_orders WHERE user_id = u.id)
           ) AS ghostedAmountCents
         FROM users u
         WHERE u.community_consent = 1 AND u.username IS NOT NULL
         ORDER BY moneyKeptCents DESC
         LIMIT ?`,
      )
      .bind(MAX_ENTRIES)
      .all<LeaderboardRow>();

    const rows = result.results ?? [];

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
