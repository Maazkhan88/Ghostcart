import { desc, eq, isNull, or } from "drizzle-orm";
import { getDb } from "../../../../db";
import { notifications } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { sanitizeShortText } from "../../../../lib/backend-contract";
import { createNotification, type NotificationType } from "../../../../lib/notifications";
import { requireApiSession } from "../../../../lib/session-auth";

const CLIENT_WRITABLE_TYPES = new Set<NotificationType>(["delivery"]);

// Returns this user's personal notifications plus every global/broadcast
// row (announcements), newest first. There is no server-side read state -
// clients derive unread/read from a locally stored "last seen" cursor.
export async function GET(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const db = getDb();
    const rows = await db
      .select()
      .from(notifications)
      .where(or(eq(notifications.userId, session.userId), isNull(notifications.userId)))
      .orderBy(desc(notifications.createdAt))
      .limit(50);
    return jsonNoStore({ notifications: rows });
  } catch {
    return jsonNoStore({ error: "Unable to load notifications right now" }, { status: 500 });
  }
}

// Lets a signed-in client record its own personal notification entries.
// Only "delivery" is accepted here - Ghost Delivery stage updates are
// computed and fired client-side (WorkManager), so the client is the only
// party that knows one just happened. "gift" and "announcement" rows are
// always written server-side, at the point the server itself knows about
// the event, not accepted from a client payload.
export async function POST(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as {
      type?: string;
      title?: string;
      body?: string;
      link?: string;
    };

    if (!CLIENT_WRITABLE_TYPES.has(payload.type as NotificationType)) {
      return jsonNoStore({ error: "type must be one of: delivery" }, { status: 400 });
    }
    const title = sanitizeShortText(payload.title, "title", 120, { required: true });
    if (title.error) return jsonNoStore({ error: title.error }, { status: 400 });
    const body = sanitizeShortText(payload.body, "body", 300, { required: true });
    if (body.error) return jsonNoStore({ error: body.error }, { status: 400 });
    const link = sanitizeShortText(payload.link, "link", 200, { required: false });

    await createNotification({
      userId: session.userId,
      type: payload.type as NotificationType,
      title: title.value!,
      body: body.value!,
      link: link.value ?? null,
    });
    return jsonNoStore({ ok: true }, { status: 201 });
  } catch {
    return jsonNoStore({ error: "Unable to save this notification right now" }, { status: 500 });
  }
}
