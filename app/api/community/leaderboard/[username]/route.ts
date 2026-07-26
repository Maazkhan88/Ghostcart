import { getD1 } from "../../../../../db";
import { jsonNoStore } from "../../../../../lib/almost-buy-api";

type RouteContext = { params: Promise<{ username: string }> };

type LeaderboardRow = {
  userId: number;
  username: string;
  avatarKey: string | null;
  avatarPresetId: string | null;
  moneyKeptCents: number;
  savedCount: number;
  ghostedCount: number;
  ghostedAmountCents: number;
  coolingCount: number;
  showRecentActivityPublicly: number;
};

type RecentItemRow = {
  name: string;
  category: string;
  almostSpentCents: number;
  capturedAt: string;
  imageUrl: string | null;
};

type ActivityRow = {
  toState: string;
  itemName: string;
  amountCents: number;
  createdAt: string;
};

type MonthStatsRow = {
  ghostedCount: number;
  savedCents: number;
  coolingCount: number;
};

// Achievement criteria are documented here since they aren't defined
// anywhere else - adjust freely, this is a first pass, not a fixed spec:
// - Top 1: currently rank #1 on the leaderboard.
// - 7 Day Streak: at least 7 consecutive days (including today or
//   yesterday, to allow for time zones) with at least one item Ghosted.
// - Deal Dodger: has confirmed at least one Money Kept skip, ever.
// - Impulse Slayer: has Ghosted 10 or more items total.
function computeAchievements(args: {
  rank: number;
  currentStreakDays: number;
  moneyKeptCents: number;
  ghostedCount: number;
}): string[] {
  const badges: string[] = [];
  if (args.rank === 1) badges.push("Top 1");
  if (args.currentStreakDays >= 7) badges.push("7 Day Streak");
  if (args.moneyKeptCents > 0) badges.push("Deal Dodger");
  if (args.ghostedCount >= 10) badges.push("Impulse Slayer");
  return badges;
}

function computeStreakDays(distinctDatesDesc: string[]): number {
  if (distinctDatesDesc.length === 0) return 0;
  const today = new Date();
  const toDateOnly = (d: Date) => d.toISOString().slice(0, 10);
  const dates = new Set(distinctDatesDesc);
  let cursor = new Date(today);
  // Allow the streak to still count as "current" if today has no entry yet
  // but yesterday does (the user just hasn't Ghosted anything today yet).
  if (!dates.has(toDateOnly(cursor))) {
    cursor.setUTCDate(cursor.getUTCDate() - 1);
    if (!dates.has(toDateOnly(cursor))) return 0;
  }
  let streak = 0;
  while (dates.has(toDateOnly(cursor))) {
    streak += 1;
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  }
  return streak;
}

function humanizeActivity(row: ActivityRow): { label: string; amountCents: number } {
  switch (row.toState) {
    case "cooling":
      return { label: `Ghosted ${row.itemName}`, amountCents: row.amountCents };
    case "resolved_skipped":
      return { label: `Saved from ${row.itemName}`, amountCents: row.amountCents };
    case "resolved_bought":
      return { label: `Bought ${row.itemName}`, amountCents: 0 };
    case "snoozed":
      return { label: `Gave ${row.itemName} more time`, amountCents: 0 };
    default:
      return { label: `Updated ${row.itemName}`, amountCents: 0 };
  }
}

export async function GET(_request: Request, { params }: RouteContext) {
  const { username } = await params;

  try {
    const db = getD1();

    // Same ordering as the public list (GET /api/community/leaderboard) so
    // rank here always matches what a member sees on the list itself.
    const leaderboard = await db
      .prepare(
        `SELECT
           u.id AS userId,
           u.username AS username,
           u.avatar_key AS avatarKey,
           u.avatar_preset_id AS avatarPresetId,
           u.show_recent_activity_publicly AS showRecentActivityPublicly,
           (SELECT COALESCE(SUM(confirmed_money_kept_cents), 0) FROM almost_buys WHERE user_id = u.id) AS moneyKeptCents,
           (SELECT COUNT(*) FROM almost_buys WHERE user_id = u.id AND state = 'resolved_skipped') AS savedCount,
           (SELECT COUNT(*) FROM almost_buys WHERE user_id = u.id) AS ghostedCount,
           (SELECT COALESCE(SUM(almost_spent_cents), 0) FROM almost_buys WHERE user_id = u.id) AS ghostedAmountCents,
           (SELECT COUNT(*) FROM almost_buys WHERE user_id = u.id AND state IN ('captured','cooling','snoozed')) AS coolingCount
         FROM users u
         WHERE u.community_consent = 1 AND u.username IS NOT NULL
         ORDER BY ghostedCount DESC, moneyKeptCents DESC`,
      )
      .all<LeaderboardRow>();

    const rows = leaderboard.results ?? [];
    const rank = rows.findIndex((row) => row.username === username);
    if (rank === -1) {
      return jsonNoStore({ error: "This member is not on the public leaderboard." }, { status: 404 });
    }
    const entry = rows[rank];

    const publicProfile = {
      username: entry.username,
      avatarUrl: entry.avatarKey ? `/api/content-blocks/image/${entry.avatarKey}` : null,
      avatarPresetId: entry.avatarPresetId,
      rank: rank + 1,
      moneyKeptCents: Number(entry.moneyKeptCents),
      savedCount: Number(entry.savedCount),
      ghostedCount: Number(entry.ghostedCount),
      ghostedAmountCents: Number(entry.ghostedAmountCents),
      coolingCount: Number(entry.coolingCount),
    };

    if (!entry.showRecentActivityPublicly) {
      return jsonNoStore({ profile: publicProfile, activityPrivate: true });
    }

    const [datesResult, recentItemsResult, activityResult, thisMonthResult, lastMonthResult] = await Promise.all([
      db
        .prepare(`SELECT DISTINCT date(captured_at) AS day FROM almost_buys WHERE user_id = ? ORDER BY day DESC LIMIT 400`)
        .bind(entry.userId)
        .all<{ day: string }>(),
      db
        .prepare(
          `SELECT name, category, almost_spent_cents AS almostSpentCents, captured_at AS capturedAt, image_url AS imageUrl
           FROM almost_buys WHERE user_id = ? ORDER BY captured_at DESC LIMIT 3`,
        )
        .bind(entry.userId)
        .all<RecentItemRow>(),
      db
        .prepare(
          `SELECT e.to_state AS toState, ab.name AS itemName, ab.almost_spent_cents AS amountCents, e.created_at AS createdAt
           FROM almost_buy_events e
           JOIN almost_buys ab ON ab.id = e.almost_buy_id
           WHERE e.user_id = ?
           ORDER BY e.created_at DESC LIMIT 5`,
        )
        .bind(entry.userId)
        .all<ActivityRow>(),
      db
        .prepare(
          `SELECT COUNT(*) AS ghostedCount,
                  COALESCE(SUM(confirmed_money_kept_cents), 0) AS savedCents,
                  (SELECT COUNT(*) FROM almost_buys WHERE user_id = ? AND state IN ('captured','cooling','snoozed') AND strftime('%Y-%m', captured_at) = strftime('%Y-%m', 'now')) AS coolingCount
           FROM almost_buys WHERE user_id = ? AND strftime('%Y-%m', captured_at) = strftime('%Y-%m', 'now')`,
        )
        .bind(entry.userId, entry.userId)
        .all<MonthStatsRow>(),
      db
        .prepare(
          `SELECT COUNT(*) AS ghostedCount,
                  COALESCE(SUM(confirmed_money_kept_cents), 0) AS savedCents,
                  0 AS coolingCount
           FROM almost_buys WHERE user_id = ? AND strftime('%Y-%m', captured_at) = strftime('%Y-%m', 'now', '-1 month')`,
        )
        .bind(entry.userId)
        .all<MonthStatsRow>(),
    ]);

    const currentStreakDays = computeStreakDays((datesResult.results ?? []).map((row) => row.day));
    const thisMonth = thisMonthResult.results?.[0];
    const lastMonth = lastMonthResult.results?.[0];

    const percentVsLastMonth = (current: number, previous: number): number | null => {
      if (previous <= 0) return null;
      return Math.round(((current - previous) / previous) * 100);
    };

    return jsonNoStore({
      profile: publicProfile,
      activityPrivate: false,
      currentStreakDays,
      achievements: computeAchievements({
        rank: rank + 1,
        currentStreakDays,
        moneyKeptCents: publicProfile.moneyKeptCents,
        ghostedCount: publicProfile.ghostedCount,
      }),
      recentGhostedItems: (recentItemsResult.results ?? []).map((row) => ({
        name: row.name,
        category: row.category,
        amountCents: Number(row.almostSpentCents),
        capturedAt: row.capturedAt,
        imageUrl: row.imageUrl,
      })),
      recentActivity: (activityResult.results ?? []).map((row) => {
        const humanized = humanizeActivity(row);
        return { label: humanized.label, amountCents: humanized.amountCents, createdAt: row.createdAt };
      }),
      thisMonth: {
        ghostedCount: Number(thisMonth?.ghostedCount ?? 0),
        savedCents: Number(thisMonth?.savedCents ?? 0),
        coolingCount: Number(thisMonth?.coolingCount ?? 0),
        ghostedCountChangePercent: percentVsLastMonth(
          Number(thisMonth?.ghostedCount ?? 0),
          Number(lastMonth?.ghostedCount ?? 0),
        ),
        savedCentsChangePercent: percentVsLastMonth(
          Number(thisMonth?.savedCents ?? 0),
          Number(lastMonth?.savedCents ?? 0),
        ),
      },
    });
  } catch (error) {
    return jsonNoStore(
      { error: error instanceof Error ? error.message : "This member's details are temporarily unavailable." },
      { status: 500 },
    );
  }
}
