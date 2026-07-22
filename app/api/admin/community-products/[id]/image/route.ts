import { eq } from "drizzle-orm";
import { getDb } from "../../../../../../db";
import { communityProducts } from "../../../../../../db/schema";
import { requireAdminApiUser } from "../../../../../../lib/admin-auth";
import { uploadImageFile } from "../../../../../../lib/content-media";
import { toRouteErrorMessage } from "../../../../../../lib/api-helpers";

const MISSING_TABLE_HINT =
  "The community_products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

type RouteContext = { params: Promise<{ id: string }> };

export async function POST(request: Request, { params }: RouteContext) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  const { id } = await params;

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
      .update(communityProducts)
      .set({ imageUrl, updatedAt: new Date().toISOString() })
      .where(eq(communityProducts.id, id))
      .returning();

    if (!product) {
      return Response.json({ error: "community product not found" }, { status: 404 });
    }
    return Response.json({ communityProduct: product });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 },
    );
  }
}
