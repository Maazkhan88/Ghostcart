import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { contentBlocks } from "../../../../db/schema";
import { parseId, toRouteErrorMessage } from "../../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../../lib/admin-auth";
import { deleteContentMedia } from "../../../../lib/content-media";

const MISSING_TABLE_HINT =
  "The content_blocks table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

const VALID_TYPES = new Set(["banner", "story"]);
const VALID_LINK_TYPES = new Set(["none", "product", "category"]);

type RouteContext = { params: Promise<{ id: string }> };

// Metadata-only: to change the image itself, delete this block and create a
// new one with a fresh upload (keeps the multipart-vs-JSON body handling
// simple - see app/api/content-blocks/route.ts for the upload path).
export async function PATCH(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const payload = (await request.json()) as Partial<{
      type: string;
      linkType: string;
      linkTargetId: string | null;
      sortOrder: number;
      isActive: boolean;
    }>;

    const updates: Record<string, unknown> = {};
    if (payload.type !== undefined) {
      if (!VALID_TYPES.has(payload.type)) {
        return Response.json({ error: "type must be 'banner' or 'story'" }, { status: 400 });
      }
      updates.type = payload.type;
    }
    if (payload.linkType !== undefined) {
      if (!VALID_LINK_TYPES.has(payload.linkType)) {
        return Response.json(
          { error: "linkType must be 'none', 'product', or 'category'" },
          { status: 400 },
        );
      }
      updates.linkType = payload.linkType;
    }
    if (payload.linkTargetId !== undefined) updates.linkTargetId = payload.linkTargetId;
    if (payload.sortOrder !== undefined) {
      if (!Number.isInteger(payload.sortOrder)) {
        return Response.json({ error: "sortOrder must be an integer" }, { status: 400 });
      }
      updates.sortOrder = payload.sortOrder;
    }
    if (payload.isActive !== undefined) updates.isActive = payload.isActive;

    const nextLinkType = (updates.linkType as string | undefined) ?? undefined;
    const nextLinkTargetId = updates.linkTargetId as string | null | undefined;
    if (nextLinkType && nextLinkType !== "none" && nextLinkTargetId === null) {
      return Response.json(
        { error: "linkTargetId is required when linkType is not 'none'" },
        { status: 400 },
      );
    }

    if (Object.keys(updates).length === 0) {
      return Response.json({ error: "no fields to update" }, { status: 400 });
    }
    updates.updatedAt = new Date().toISOString();

    const db = getDb();
    const [block] = await db
      .update(contentBlocks)
      .set(updates)
      .where(eq(contentBlocks.id, id))
      .returning();

    if (!block) {
      return Response.json({ error: "content block not found" }, { status: 404 });
    }
    return Response.json({ contentBlock: block });
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

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  try {
    const db = getDb();
    // D1 row removed first: once this succeeds nothing references imageKey
    // any more, so a subsequent R2-delete failure only orphans storage
    // rather than leaving a dangling reference visible to the app.
    const [block] = await db
      .delete(contentBlocks)
      .where(eq(contentBlocks.id, id))
      .returning();

    if (!block) {
      return Response.json({ error: "content block not found" }, { status: 404 });
    }

    await deleteContentMedia(block.imageKey);

    return Response.json({ contentBlock: block });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
