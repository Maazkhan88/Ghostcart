import { and, eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { favoriteProducts } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { sanitizeShortText } from "../../../../lib/backend-contract";
import { requireApiSession } from "../../../../lib/session-auth";

export async function GET(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const db = getDb();
    const rows = await db
      .select({ productId: favoriteProducts.productId })
      .from(favoriteProducts)
      .where(eq(favoriteProducts.userId, session.userId));
    return jsonNoStore({ productIds: rows.map((row) => row.productId) });
  } catch {
    return jsonNoStore({ error: "Unable to load favorites right now" }, { status: 500 });
  }
}

export async function POST(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as { productId?: string };
    const productId = sanitizeShortText(payload.productId, "productId", 200, { required: true });
    if (productId.error) return jsonNoStore({ error: productId.error }, { status: 400 });

    const db = getDb();
    await db
      .insert(favoriteProducts)
      .values({ userId: session.userId, productId: productId.value! })
      .onConflictDoNothing({
        target: [favoriteProducts.userId, favoriteProducts.productId],
      });

    return jsonNoStore({ favorited: true }, { status: 201 });
  } catch {
    return jsonNoStore({ error: "Unable to favorite this item right now" }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as { productId?: string };
    const productId = sanitizeShortText(payload.productId, "productId", 200, { required: true });
    if (productId.error) return jsonNoStore({ error: productId.error }, { status: 400 });

    const db = getDb();
    await db
      .delete(favoriteProducts)
      .where(
        and(
          eq(favoriteProducts.userId, session.userId),
          eq(favoriteProducts.productId, productId.value!),
        ),
      );

    return jsonNoStore({ removed: true });
  } catch {
    return jsonNoStore({ error: "Unable to remove this favorite right now" }, { status: 500 });
  }
}
