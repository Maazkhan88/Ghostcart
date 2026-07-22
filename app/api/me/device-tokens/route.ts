import { getDb } from "../../../../db";
import { deviceTokens } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { sanitizeShortText } from "../../../../lib/backend-contract";
import { requireApiSession } from "../../../../lib/session-auth";

const PLATFORMS = new Set(["android", "ios"]);

// Registers (or refreshes) this device's FCM token against the signed-in
// user so the cooldown-expiry cron can find where to send a push. A token
// moving to a new account (reinstall, different sign-in) re-points the same
// row rather than leaving a stale duplicate under the old owner.
export async function POST(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as { token?: string; platform?: string };
    const token = sanitizeShortText(payload.token, "token", 4096, { required: true });
    if (token.error) return jsonNoStore({ error: token.error }, { status: 400 });
    const platform = typeof payload.platform === "string" && PLATFORMS.has(payload.platform)
      ? payload.platform
      : "android";

    const db = getDb();
    await db
      .insert(deviceTokens)
      .values({ userId: session.userId, token: token.value!, platform })
      .onConflictDoUpdate({
        target: deviceTokens.token,
        set: { userId: session.userId, platform, updatedAt: new Date().toISOString() },
      });

    return jsonNoStore({ registered: true }, { status: 201 });
  } catch {
    return jsonNoStore({ error: "Unable to register this device right now" }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as { token?: string };
    const token = sanitizeShortText(payload.token, "token", 4096, { required: true });
    if (token.error) return jsonNoStore({ error: token.error }, { status: 400 });

    const db = getDb();
    const { eq, and } = await import("drizzle-orm");
    await db
      .delete(deviceTokens)
      .where(and(eq(deviceTokens.token, token.value!), eq(deviceTokens.userId, session.userId)));

    return jsonNoStore({ removed: true });
  } catch {
    return jsonNoStore({ error: "Unable to remove this device right now" }, { status: 500 });
  }
}
