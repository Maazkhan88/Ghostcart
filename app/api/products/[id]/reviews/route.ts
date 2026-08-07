import { desc, eq } from "drizzle-orm";
import { getDb } from "../../../../../db";
import { productReviews, users } from "../../../../../db/schema";
import { sanitizeShortText } from "../../../../../lib/backend-contract";
import { consumeRateLimit, requestActorHash } from "../../../../../lib/rate-limit";
import { requireApiSession } from "../../../../../lib/session-auth";

type RouteContext = { params: Promise<{ id: string }> };

function noStore(body: unknown, status = 200) {
  return Response.json(body, { status, headers: { "Cache-Control": "no-store" } });
}

// productId is a plain string, not an FK - it spans both catalog (numeric,
// stringified) and community product ids, same convention as
// favorite_products.product_id (see db/schema.ts comment there).
export async function GET(request: Request, { params }: RouteContext) {
  const { id: productId } = await params;
  if (!productId) return noStore({ error: "productId is required" }, 400);

  try {
    const db = getDb();
    const rows = await db
      .select({
        id: productReviews.id,
        rating: productReviews.rating,
        text: productReviews.text,
        createdAt: productReviews.createdAt,
        userId: productReviews.userId,
        displayName: users.displayName,
      })
      .from(productReviews)
      .innerJoin(users, eq(users.id, productReviews.userId))
      .where(eq(productReviews.productId, productId))
      .orderBy(desc(productReviews.createdAt))
      .limit(100);

    const session = await requireApiSession(request).catch(() => null);
    const viewerId = session && !(session instanceof Response) ? session.userId : null;

    return noStore({
      reviews: rows.map((row) => ({
        id: row.id,
        rating: row.rating,
        text: row.text,
        createdAt: row.createdAt,
        authorName: row.displayName?.trim() || "A Ghost Cart member",
        isOwn: viewerId !== null && row.userId === viewerId,
      })),
    });
  } catch (error) {
    console.error("product reviews list failed", error);
    return noStore({ error: "Unable to load reviews right now" }, 500);
  }
}

export async function POST(request: Request, { params }: RouteContext) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  const { id: productId } = await params;
  if (!productId) return noStore({ error: "productId is required" }, 400);

  const rateLimit = await consumeRateLimit({
    namespace: "product-review-create",
    actorHash: await requestActorHash(request),
    cost: 1,
    limit: 20,
    windowSeconds: 24 * 60 * 60,
  });
  if (!rateLimit.allowed) {
    return noStore({ error: "Too many reviews submitted. Try again tomorrow." }, 429);
  }

  try {
    const payload = (await request.json()) as { rating?: unknown; text?: unknown };
    const rating = typeof payload.rating === "number" ? Math.round(payload.rating) : NaN;
    if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
      return noStore({ error: "rating must be an integer from 1 to 5" }, 400);
    }
    const textResult = sanitizeShortText(payload.text ?? "", "text", 2000);
    if (textResult.error) return noStore({ error: textResult.error }, 400);

    const db = getDb();
    const id = crypto.randomUUID();
    await db.insert(productReviews).values({
      id,
      productId,
      userId: session.userId,
      rating,
      text: textResult.value ?? "",
    });

    return noStore({ review: { id, productId, rating, text: textResult.value ?? "" } }, 201);
  } catch (error) {
    console.error("product review create failed", error);
    return noStore({ error: "Unable to submit this review right now" }, 500);
  }
}

