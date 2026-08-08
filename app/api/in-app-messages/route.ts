import { desc, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { inAppMessages } from "../../../db/schema";
import { toRouteErrorMessage } from "../../../lib/api-helpers";
import { getGhostCartAdminUser, requireAdminApiUser } from "../../../lib/admin-auth";
import { createNotification } from "../../../lib/notifications";

const MISSING_TABLE_HINT =
  "The in_app_messages table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

// Public callers (the Android client) only ever see active messages. An
// authenticated admin session sees everything, so the admin tab can manage
// (and re-activate) inactive messages too - no separate endpoint needed.
export async function GET() {
  try {
    const db = getDb();
    const admin = await getGhostCartAdminUser();
    const rows = admin
      ? await db.select().from(inAppMessages).orderBy(inAppMessages.sortOrder, desc(inAppMessages.createdAt))
      : await db
          .select()
          .from(inAppMessages)
          .where(eq(inAppMessages.isActive, true))
          .orderBy(inAppMessages.sortOrder, desc(inAppMessages.createdAt));

    return Response.json(
      { messages: rows },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

export async function POST(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as {
      title?: string;
      body?: string;
      imageUrl?: string;
      linkUrl?: string;
      sortOrder?: number;
      isActive?: boolean;
    };

    const title = payload.title?.trim() ?? "";
    const body = payload.body?.trim() ?? "";
    if (!title) return Response.json({ error: "title is required" }, { status: 400 });
    if (!body) return Response.json({ error: "body is required" }, { status: 400 });

    const sortOrder = payload.sortOrder ?? 0;
    if (!Number.isInteger(sortOrder)) {
      return Response.json({ error: "sortOrder must be an integer" }, { status: 400 });
    }

    const db = getDb();
    const [message] = await db
      .insert(inAppMessages)
      .values({
        id: crypto.randomUUID(),
        title,
        body,
        imageUrl: payload.imageUrl?.trim() || null,
        linkUrl: payload.linkUrl?.trim() || null,
        sortOrder,
        isActive: payload.isActive ?? true,
      })
      .returning();

    if (message.isActive) {
      // Best-effort: a failed feed write must never block publishing the
      // announcement itself, which is already live via the modal dialog path.
      createNotification({
        userId: null,
        type: "announcement",
        title: message.title,
        body: message.body,
        link: message.linkUrl,
      }).catch((error) => console.warn("announcement notification failed softly:", error));
    }

    return Response.json({ message }, { status: 201 });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
