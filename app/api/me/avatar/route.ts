import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { users } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { uploadImageFile } from "../../../../lib/content-media";
import { requireApiSession } from "../../../../lib/session-auth";

// Same validated pipeline (magic-byte content sniffing, size/dimension
// limits, EXIF stripping) as every other image upload in the app - just
// gated on a normal user session instead of admin.
export async function POST(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  let form: FormData;
  try {
    form = await request.formData();
  } catch {
    return jsonNoStore({ error: "Expected multipart/form-data with an image file." }, { status: 400 });
  }

  const file = form.get("file");
  if (!(file instanceof File)) {
    return jsonNoStore({ error: "file is required" }, { status: 400 });
  }

  const uploaded = await uploadImageFile(file);
  if (!uploaded.ok) {
    return jsonNoStore({ error: uploaded.error }, { status: uploaded.status });
  }

  const db = getDb();
  await db
    .update(users)
    .set({ avatarKey: uploaded.key, updatedAt: new Date().toISOString() })
    .where(eq(users.id, session.userId));

  return jsonNoStore({ avatarUrl: `/api/content-blocks/image/${uploaded.key}` });
}
