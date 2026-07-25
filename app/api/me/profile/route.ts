import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { sanitizeShortText } from "../../../../lib/backend-contract";
import { requireApiSession } from "../../../../lib/session-auth";
import {
  candidateDefaultUsernames,
  canRenameUsername,
  validateUsernameFormat,
} from "../../../../lib/username-policy";

type ProfileRow = {
  email: string;
  displayName: string | null;
  username: string | null;
  avatarKey: string | null;
  communityConsent: boolean;
  gender: string | null;
  avatarPresetId: string | null;
};

const ALLOWED_GENDERS = new Set(["male", "female"]);

// The username-rename-cooldown error previously interpolated the raw
// Date.toISOString() value (e.g. "2026-08-06T16:00:51.382Z") directly into
// a user-facing message - correct but unreadable. Format it as a plain
// date in this app's UAE timezone instead; nobody needs cooldown precision
// down to the millisecond for a 14-day wait.
function formatRetryDate(isoDate: string | undefined): string {
  if (!isoDate) return "later";
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return "later";
  return new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "Asia/Dubai",
  }).format(date);
}

// Auto-enrollment is opt-out: an account can have communityConsent = true
// with no username yet (existing accounts backfilled by migration 0012, or
// a user who never opened Profile). Assigns one, lazily, the first time
// it's actually needed, rather than a bulk migration guessing usernames for
// accounts that may never be fetched again.
//
// Deliberately leaves usernameUpdatedAt untouched (null) here - this is a
// system fallback, not a user-chosen rename, so it must not start the
// 14-day rename cooldown (lib/username-policy.ts's canRenameUsername treats
// a null usernameUpdatedAt as "never renamed, free to change"). Setting it
// here once locked a real account out of editing its own just-auto-assigned
// name for 14 days.
async function ensureUsername(row: ProfileRow, userId: number): Promise<ProfileRow> {
  if (!row.communityConsent || row.username) return row;

  const db = getDb();
  for (const candidate of candidateDefaultUsernames()) {
    if (validateUsernameFormat(candidate)) continue;
    try {
      const [updated] = await db
        .update(users)
        .set({ username: candidate })
        .where(eq(users.id, userId))
        .returning({
          email: users.email,
          displayName: users.displayName,
          username: users.username,
          avatarKey: users.avatarKey,
          communityConsent: users.communityConsent,
          gender: users.gender,
          avatarPresetId: users.avatarPresetId,
        });
      return updated;
    } catch (error) {
      const message = error instanceof Error ? error.message : "";
      if (!message.includes("UNIQUE constraint failed")) throw error;
      // Taken - try the next candidate.
    }
  }
  return row;
}

function serialize(user: {
  email: string;
  displayName: string | null;
  username: string | null;
  avatarKey: string | null;
  communityConsent: boolean;
  gender: string | null;
  avatarPresetId: string | null;
}) {
  return {
    email: user.email,
    displayName: user.displayName,
    username: user.username,
    avatarUrl: user.avatarKey ? `/api/content-blocks/image/${user.avatarKey}` : null,
    communityConsent: user.communityConsent,
    gender: user.gender,
    avatarPresetId: user.avatarPresetId,
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
      gender: users.gender,
      avatarPresetId: users.avatarPresetId,
    })
    .from(users)
    .where(eq(users.id, session.userId))
    .limit(1);

  if (!user) return jsonNoStore({ error: "account not found" }, { status: 404 });
  const withUsername = await ensureUsername(user, session.userId);
  return jsonNoStore({ profile: serialize(withUsername) });
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

  if (payload.gender !== undefined) {
    if (payload.gender !== null && typeof payload.gender !== "string") {
      return jsonNoStore({ error: "gender must be a string or null" }, { status: 400 });
    }
    const normalized = typeof payload.gender === "string" ? payload.gender.trim().toLowerCase() : null;
    if (normalized !== null && !ALLOWED_GENDERS.has(normalized)) {
      return jsonNoStore({ error: "gender must be one of: male, female" }, { status: 400 });
    }
    updates.gender = normalized;
  }

  // No fixed set of valid ids yet - the preset avatar picker (19 images) isn't
  // built. Accept any short string for now so this field is ready to wire the
  // picker into once it exists, rather than guessing an enum ahead of time.
  if (payload.avatarPresetId !== undefined) {
    if (payload.avatarPresetId === null) {
      updates.avatarPresetId = null;
    } else {
      const result = sanitizeShortText(payload.avatarPresetId, "avatarPresetId", 80);
      if (result.error) return jsonNoStore({ error: result.error }, { status: 400 });
      updates.avatarPresetId = result.value || null;
    }
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
          { error: `You can change your username again after ${formatRetryDate(rename.retryAt)}.` },
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
        gender: users.gender,
        avatarPresetId: users.avatarPresetId,
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
