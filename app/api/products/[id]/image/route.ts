import { eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { products } from "../../../../../db/schema";
import { requireAdminApiUser } from "../../../../../lib/admin-auth";
import { parseId, toRouteErrorMessage } from "../../../../../lib/api-helpers";
import { uploadImageFile } from "../../../../../lib/content-media";

const MISSING_TABLE_HINT =
  "The products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

// Uploads a product photo (same validated pipeline as the Content tab's
// banner/story uploads - real magic-byte sniffing, size/dimension limits,
// EXIF stripping) and points products.imageUrl at it. Deliberately stores
// an absolute URL (not the relative path the admin UI's own <img> tags use)
// because /api/products is also read by the Android/iOS clients, which have
// no notion of "same origin" to resolve a relative path against.
export async function POST(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id: rawId } = await params;
  const id = parseId(rawId);
  if (id === null) {
    return Response.json({ error: "invalid id" }, { status: 400 });
  }

  let form: FormData;
  try {
    form = await request.formData();
  } catch {
    return Response.json(
      { error: "Expected multipart/form-data with an image file." },
      { status: 400 },
    );
  }

  const file = form.get("file");
  if (!(file instanceof File)) {
    return Response.json({ error: "file is required" }, { status: 400 });
  }

  const uploaded = await uploadImageFile(file);
  if (!uploaded.ok) {
    return Response.json({ error: uploaded.error }, { status: uploaded.status });
  }

  const imageUrl = `${new URL(request.url).origin}/api/content-blocks/image/${uploaded.key}`;

  try {
    const db = getDb();
    const [product] = await db
      .update(products)
      .set({ imageUrl, updatedAt: new Date().toISOString() })
      .where(eq(products.id, id))
      .returning();

    if (!product) {
      return Response.json({ error: "product not found" }, { status: 404 });
    }
    return Response.json({ product });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
