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
// Canonical vocabulary: an item is Ghosted when its cooldown begins. Every
// almost_buys row therefore contributes exactly once to Ghosted count,
// regardless of whether the later decision is skipped, bought, or restarted.
// "Cooled & saved" remains the narrower skipped outcome and Money Kept metric.
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
           (SELECT COUNT(*) FROM almost_buys WHERE user_id = u.id) AS ghostedCount,
           (SELECT COALESCE(SUM(almost_spent_cents), 0) FROM almost_buys WHERE user_id = u.id) AS ghostedAmountCents
         FROM users u
         WHERE u.community_consent = 1 AND u.username IS NOT NULL
         ORDER BY ghostedCount DESC, moneyKeptCents DESC
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
