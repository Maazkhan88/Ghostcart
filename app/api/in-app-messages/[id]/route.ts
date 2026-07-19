import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { inAppMessages } from "../../../../db/schema";
import { toRouteErrorMessage } from "../../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../../lib/admin-auth";

const MISSING_TABLE_HINT =
  "The in_app_messages table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id } = await params;

  try {
    const payload = (await request.json()) as Partial<{
      title: string;
      body: string;
      imageUrl: string | null;
      linkUrl: string | null;
      sortOrder: number;
      isActive: boolean;
    }>;

    const updates: Record<string, unknown> = {};
    if (payload.title !== undefined) updates.title = payload.title.trim();
    if (payload.body !== undefined) updates.body = payload.body.trim();
    if (payload.imageUrl !== undefined) updates.imageUrl = payload.imageUrl?.trim() || null;
    if (payload.linkUrl !== undefined) updates.linkUrl = payload.linkUrl?.trim() || null;
    if (payload.isActive !== undefined) updates.isActive = payload.isActive;
    if (payload.sortOrder !== undefined) {
      if (!Number.isInteger(payload.sortOrder)) {
        return Response.json({ error: "sortOrder must be an integer" }, { status: 400 });
      }
      updates.sortOrder = payload.sortOrder;
    }

    if (Object.keys(updates).length === 0) {
      return Response.json({ error: "no fields to update" }, { status: 400 });
    }
    updates.updatedAt = new Date().toISOString();

    const db = getDb();
    const [message] = await db
      .update(inAppMessages)
      .set(updates)
      .where(eq(inAppMessages.id, id))
      .returning();

    if (!message) {
      return Response.json({ error: "message not found" }, { status: 404 });
    }
    return Response.json({ message });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}

export async function DELETE(_request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id } = await params;

  try {
    const db = getDb();
    const [message] = await db
      .delete(inAppMessages)
      .where(eq(inAppMessages.id, id))
      .returning();

    if (!message) {
      return Response.json({ error: "message not found" }, { status: 404 });
    }
    return Response.json({ message });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
