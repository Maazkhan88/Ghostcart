import { sendPush } from "./fcm";
import { sendCooldownResolvedEmail, type EmailBinding } from "./email";

type ExpiredCooldown = { id: string; userId: number; title: string; userEmail: string };
type DeviceTokenRow = { token: string };

const BATCH_LIMIT = 200;

// Runs on the Worker's cron trigger (wrangler.ghostcart-app.jsonc `triggers`).
// Finds cooling items whose timer has passed and haven't been pushed yet,
// sends one FCM push per registered device plus one email to the owning
// account, then marks the item as swept - regardless of whether a token
// existed or either send succeeded, so a user with no device registered (or
// a transient FCM/email outage) doesn't get re-queried and re-attempted every
// run forever. This is a best-effort notify, not a guaranteed-delivery queue.
export async function sweepExpiredCooldowns(
  db: D1Database,
  fcmServiceAccountJson: string | undefined,
  email: EmailBinding | undefined,
  emailUnsubscribeSecret: string | undefined,
  resendApiKey?: string,
): Promise<{ swept: number; pushed: number; prunedTokens: number; emailed: number }> {
  const now = new Date().toISOString();
  const expired = await db
    .prepare(
      `SELECT almost_buys.id AS id, almost_buys.user_id AS userId, almost_buys.title AS title,
              users.email AS userEmail,
              COALESCE(user_preferences.email_notifications, 1) AS emailNotifications
       FROM almost_buys
       JOIN users ON users.id = almost_buys.user_id
       LEFT JOIN user_preferences ON user_preferences.user_id = almost_buys.user_id
       WHERE almost_buys.state = 'cooling' AND almost_buys.cool_off_until <= ?
         AND almost_buys.push_sent_at IS NULL
       ORDER BY almost_buys.cool_off_until ASC
       LIMIT ?`,
    )
    .bind(now, BATCH_LIMIT)
    .all<ExpiredCooldown & { emailNotifications: number }>();

  const rows = expired.results ?? [];
  let pushed = 0;
  let prunedTokens = 0;
  let emailed = 0;

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

    if (row.emailNotifications) {
      const emailResult = await sendCooldownResolvedEmail(
        email,
        row.userEmail,
        row.title,
        row.userId,
        row.id,
        emailUnsubscribeSecret,
        resendApiKey,
      );
      if (emailResult.ok) emailed += 1;
    }

    await db
      .prepare(`UPDATE almost_buys SET push_sent_at = ? WHERE id = ?`)
      .bind(new Date().toISOString(), row.id)
      .run();
  }

  return { swept: rows.length, pushed, prunedTokens, emailed };
}
