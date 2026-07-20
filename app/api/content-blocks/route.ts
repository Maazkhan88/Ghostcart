import { desc } from "drizzle-orm";
import { getDb } from "../../../db";
import { contentBlocks } from "../../../db/schema";
import { toRouteErrorMessage } from "../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../lib/admin-auth";
import { generateContentMediaKey, putContentMedia } from "../../../lib/content-media";
import {
  IMAGE_EXTENSIONS,
  IMAGE_MIME_TYPES,
  MAX_IMAGE_DIMENSION,
  MAX_UPLOAD_BYTES,
  readImageDimensions,
  sniffImageType,
  stripImageMetadata,
} from "../../../lib/image-processing";

const MISSING_TABLE_HINT =
  "The content_blocks table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

const VALID_TYPES = new Set(["banner", "story"]);
const VALID_LINK_TYPES = new Set(["none", "product", "category"]);

export async function GET() {
  try {
    const db = getDb();
    const rows = await db
      .select()
      .from(contentBlocks)
      .orderBy(contentBlocks.type, contentBlocks.sortOrder, desc(contentBlocks.createdAt));

    return Response.json({ contentBlocks: rows });
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

  let form: FormData;
  try {
    form = await request.formData();
  } catch {
    return Response.json(
      { error: "Expected multipart/form-data with an image file." },
      { status: 400 },
    );
  }

  const type = String(form.get("type") ?? "");
  const linkType = String(form.get("linkType") ?? "none");
  const linkTargetIdRaw = form.get("linkTargetId");
  const linkTargetId = linkTargetIdRaw ? String(linkTargetIdRaw).trim() : null;
  const sortOrderRaw = form.get("sortOrder");
  const sortOrder = sortOrderRaw ? Number(sortOrderRaw) : 0;
  const isActiveRaw = form.get("isActive");
  const isActive = isActiveRaw === null ? true : isActiveRaw === "true";
  const file = form.get("file");

  if (!VALID_TYPES.has(type)) {
    return Response.json({ error: "type must be 'banner' or 'story'" }, { status: 400 });
  }
  if (!VALID_LINK_TYPES.has(linkType)) {
    return Response.json(
      { error: "linkType must be 'none', 'product', or 'category'" },
      { status: 400 },
    );
  }
  if (linkType !== "none" && !linkTargetId) {
    return Response.json(
      { error: "linkTargetId is required when linkType is not 'none'" },
      { status: 400 },
    );
  }
  if (!Number.isInteger(sortOrder)) {
    return Response.json({ error: "sortOrder must be an integer" }, { status: 400 });
  }
  if (!(file instanceof File)) {
    return Response.json({ error: "file is required" }, { status: 400 });
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return Response.json(
      { error: `file exceeds the ${MAX_UPLOAD_BYTES / 1_000_000}MB limit` },
      { status: 400 },
    );
  }

  const bytes = new Uint8Array(await file.arrayBuffer());

  // Content is inspected by its actual bytes, never by the claimed
  // Content-Type/filename - an admin uploading a renamed .svg or any other
  // disguised file is rejected here regardless of what the browser sent.
  const sniffedType = sniffImageType(bytes);
  if (!sniffedType) {
    return Response.json(
      { error: "file must be a genuine PNG or JPEG image (SVG and other formats are not accepted)" },
      { status: 400 },
    );
  }

  const dimensions = readImageDimensions(bytes, sniffedType);
  if (!dimensions) {
    return Response.json({ error: "could not read image dimensions" }, { status: 400 });
  }
  if (dimensions.width > MAX_IMAGE_DIMENSION || dimensions.height > MAX_IMAGE_DIMENSION) {
    return Response.json(
      { error: `image dimensions must not exceed ${MAX_IMAGE_DIMENSION}px per side` },
      { status: 400 },
    );
  }

  const stripped = stripImageMetadata(bytes, sniffedType);
  const key = generateContentMediaKey(IMAGE_EXTENSIONS[sniffedType]);

  try {
    await putContentMedia(key, stripped, IMAGE_MIME_TYPES[sniffedType]);
  } catch (error) {
    return Response.json(
      { error: error instanceof Error ? error.message : "could not store the uploaded image" },
      { status: 500 },
    );
  }

  try {
    const db = getDb();
    const [block] = await db
      .insert(contentBlocks)
      .values({
        type,
        imageKey: key,
        linkType,
        linkTargetId,
        sortOrder,
        isActive,
      })
      .returning();

    return Response.json({ contentBlock: block }, { status: 201 });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
