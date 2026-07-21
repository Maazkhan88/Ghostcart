import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { sanitizeShortText } from "../../../../lib/backend-contract";
import { requireApiSession } from "../../../../lib/session-auth";
import { canRenameUsername, validateUsernameFormat } from "../../../../lib/username-policy";

function serialize(user: {
  email: string;
  displayName: string | null;
  username: string | null;
  avatarKey: string | null;
  communityConsent: boolean;
}) {
  return {
    email: user.email,
    displayName: user.displayName,
    username: user.username,
    avatarUrl: user.avatarKey ? `/api/content-blocks/image/${user.avatarKey}` : null,
    communityConsent: user.communityConsent,
  };
}

export async function GET(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  const db = getDb();
  const [user] = await db
    .select({
      email: users.email,
      displayName: users.displayName,
      username: users.username,
      avatarKey: users.avatarKey,
      communityConsent: users.communityConsent,
    })
    .from(users)
    .where(eq(users.id, session.userId))
    .limit(1);

  if (!user) return jsonNoStore({ error: "account not found" }, { status: 404 });
  return jsonNoStore({ profile: serialize(user) });
}

// Two independent things can be updated here: displayName (always allowed),
// and opting in/out of the public leaderboard via username + communityConsent
// (separate from and never affecting the existing anonymous community-products
// feed's anonymity guarantee). Sending communityConsent: false leaves the
// username reserved so re-opting in later keeps the same identity.
export async function PATCH(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  let payload: Record<string, unknown>;
  try {
    payload = (await request.json()) as Record<string, unknown>;
  } catch {
    return jsonNoStore({ error: "invalid JSON body" }, { status: 400 });
  }

  const updates: Record<string, unknown> = {};

  if (payload.displayName !== undefined) {
    const result = sanitizeShortText(payload.displayName, "displayName", 80);
    if (result.error) return jsonNoStore({ error: result.error }, { status: 400 });
    updates.displayName = result.value || null;
  }

  if (payload.communityConsent !== undefined) {
    if (typeof payload.communityConsent !== "boolean") {
      return jsonNoStore({ error: "communityConsent must be a boolean" }, { status: 400 });
    }
    updates.communityConsent = payload.communityConsent;
  }

  if (payload.username !== undefined) {
    if (typeof payload.username !== "string") {
      return jsonNoStore({ error: "username must be a string" }, { status: 400 });
    }
    const username = payload.username.trim();
    const formatError = validateUsernameFormat(username);
    if (formatError) return jsonNoStore({ error: formatError }, { status: 400 });

    const db = getDb();
    const [current] = await db
      .select({ username: users.username, usernameUpdatedAt: users.usernameUpdatedAt })
      .from(users)
      .where(eq(users.id, session.userId))
      .limit(1);

    if (current?.username !== username) {
      const rename = canRenameUsername(current?.usernameUpdatedAt ?? null);
      if (!rename.allowed) {
        return jsonNoStore(
          { error: `You can change your username again after ${rename.retryAt}.` },
          { status: 429 },
        );
      }
      updates.username = username;
      updates.usernameUpdatedAt = new Date().toISOString();
    }
  }

  if (Object.keys(updates).length === 0) {
    return jsonNoStore({ error: "no fields to update" }, { status: 400 });
  }
  updates.updatedAt = new Date().toISOString();

  try {
    const db = getDb();
    const [user] = await db
      .update(users)
      .set(updates)
      .where(eq(users.id, session.userId))
      .returning({
        email: users.email,
        displayName: users.displayName,
        username: users.username,
        avatarKey: users.avatarKey,
        communityConsent: users.communityConsent,
      });
    return jsonNoStore({ profile: serialize(user) });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Could not update profile.";
    const isDuplicate = message.includes("UNIQUE constraint failed");
    return jsonNoStore(
      { error: isDuplicate ? "That username is already taken." : "Could not update profile." },
      { status: isDuplicate ? 409 : 500 },
    );
  }
}
