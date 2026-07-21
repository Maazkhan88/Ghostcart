import { sendPush } from "./fcm";

type ExpiredCooldown = { id: string; userId: number; title: string };
type DeviceTokenRow = { token: string };

const BATCH_LIMIT = 200;

// Runs on the Worker's cron trigger (wrangler.ghostcart-app.jsonc `triggers`).
// Finds cooling items whose timer has passed and haven't been pushed yet,
// sends one FCM push per registered device for that user, then marks the
// item as swept - regardless of whether a token existed or the send
// succeeded, so a user with no device registered (or a transient FCM outage)
// doesn't get re-queried and re-attempted every run forever. This is a
// best-effort notify, not a guaranteed-delivery queue.
export async function sweepExpiredCooldowns(
  db: D1Database,
  fcmServiceAccountJson: string | undefined,
): Promise<{ swept: number; pushed: number; prunedTokens: number }> {
  const now = new Date().toISOString();
  const expired = await db
    .prepare(
      `SELECT id, user_id AS userId, title FROM almost_buys
       WHERE state = 'cooling' AND cool_off_until <= ? AND push_sent_at IS NULL
       ORDER BY cool_off_until ASC
       LIMIT ?`,
    )
    .bind(now, BATCH_LIMIT)
    .all<ExpiredCooldown>();

  const rows = expired.results ?? [];
  let pushed = 0;
  let prunedTokens = 0;

  for (const row of rows) {
    const tokens = await db
      .prepare(`SELECT token FROM device_tokens WHERE user_id = ?`)
      .bind(row.userId)
      .all<DeviceTokenRow>();

    for (const { token } of tokens.results ?? []) {
      const result = await sendPush(fcmServiceAccountJson, {
        token,
        title: "Cooling complete 👻",
        body: `${row.title} has cooled off. Do you still want it?`,
        data: { cooldownId: row.id, type: "cooldown_resolved" },
      });
      if (result.ok) pushed += 1;
      if (result.shouldRemoveToken) {
        await db.prepare(`DELETE FROM device_tokens WHERE token = ?`).bind(token).run();
        prunedTokens += 1;
      }
    }

    await db
      .prepare(`UPDATE almost_buys SET push_sent_at = ? WHERE id = ?`)
      .bind(new Date().toISOString(), row.id)
      .run();
  }

  return { swept: rows.length, pushed, prunedTokens };
}
