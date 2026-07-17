import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { userPreferences } from "../../../../db/schema";
import {
  readIsoDate,
  readMinuteOfDay,
  sanitizeShortText,
} from "../../../../lib/backend-contract";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { requireApiSession } from "../../../../lib/session-auth";

const BOOLEAN_FIELDS = [
  "coolingNotifications",
  "lunchReminder",
  "dinnerReminder",
  "weeklyReview",
] as const;

async function ensurePreferences(userId: number) {
  const db = getDb();
  await db
    .insert(userPreferences)
    .values({ userId })
    .onConflictDoNothing({ target: userPreferences.userId });
  const [preferences] = await db
    .select()
    .from(userPreferences)
    .where(eq(userPreferences.userId, userId))
    .limit(1);
  return preferences;
}

export async function GET(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const preferences = await ensurePreferences(session.userId);
    return jsonNoStore({ preferences });
  } catch {
    return jsonNoStore(
      { error: "Unable to load notification preferences" },
      { status: 500 },
    );
  }
}

export async function PATCH(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const current = await ensurePreferences(session.userId);
    const payload = (await request.json()) as Record<string, unknown>;
    for (const field of BOOLEAN_FIELDS) {
      if (payload[field] !== undefined && typeof payload[field] !== "boolean") {
        return jsonNoStore({ error: `${field} must be true or false` }, { status: 400 });
      }
    }
    const locale = sanitizeShortText(payload.locale, "locale", 20);
    const timeZone = sanitizeShortText(payload.timeZone, "timeZone", 80);
    const lunchMinute = readMinuteOfDay(
      payload.lunchReminderMinute,
      "lunchReminderMinute",
    );
    const dinnerMinute = readMinuteOfDay(
      payload.dinnerReminderMinute,
      "dinnerReminderMinute",
    );
    const pausedUntil = readIsoDate(
      payload.reminderPausedUntil,
      "reminderPausedUntil",
    );
    const validationError =
      locale.error || timeZone.error || lunchMinute.error || dinnerMinute.error ||
      pausedUntil.error;
    if (validationError) {
      return jsonNoStore({ error: validationError }, { status: 400 });
    }
    if (timeZone.value && !isSupportedTimeZone(timeZone.value)) {
      return jsonNoStore({ error: "timeZone is not supported" }, { status: 400 });
    }
    const knownFields = new Set<string>([
      ...BOOLEAN_FIELDS,
      "locale",
      "timeZone",
      "lunchReminderMinute",
      "dinnerReminderMinute",
      "reminderPausedUntil",
    ]);
    if (!Object.keys(payload).some((key) => knownFields.has(key))) {
      return jsonNoStore({ error: "no preference fields to update" }, { status: 400 });
    }

    const db = getDb();
    const [preferences] = await db
      .update(userPreferences)
      .set({
        locale: locale.value ?? current.locale,
        timeZone: timeZone.value ?? current.timeZone,
        coolingNotifications:
          (payload.coolingNotifications as boolean | undefined) ??
          current.coolingNotifications,
        lunchReminder:
          (payload.lunchReminder as boolean | undefined) ?? current.lunchReminder,
        lunchReminderMinute: lunchMinute.value ?? current.lunchReminderMinute,
        dinnerReminder:
          (payload.dinnerReminder as boolean | undefined) ?? current.dinnerReminder,
        dinnerReminderMinute: dinnerMinute.value ?? current.dinnerReminderMinute,
        weeklyReview:
          (payload.weeklyReview as boolean | undefined) ?? current.weeklyReview,
        reminderPausedUntil:
          pausedUntil.value === undefined
            ? current.reminderPausedUntil
            : pausedUntil.value,
        updatedAt: new Date().toISOString(),
      })
      .where(eq(userPreferences.userId, session.userId))
      .returning();
    return jsonNoStore({ preferences });
  } catch {
    return jsonNoStore(
      { error: "Unable to update notification preferences" },
      { status: 500 },
    );
  }
}

function isSupportedTimeZone(timeZone: string): boolean {
  try {
    new Intl.DateTimeFormat("en", { timeZone }).format();
    return true;
  } catch {
    return false;
  }
}
