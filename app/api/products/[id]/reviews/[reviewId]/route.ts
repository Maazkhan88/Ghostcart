import { and, eq } from "drizzle-orm";
import { getDb } from "../../../../../../db";
import { productReviews } from "../../../../../../db/schema";
import { requireApiSession } from "../../../../../../lib/session-auth";

type RouteContext = { params: Promise<{ id: string; reviewId: string }> };

function noStore(body: unknown, status = 200) {
  return Response.json(body, { status, headers: { "Cache-Control": "no-store" } });
}

// Only the reviewId (owner-scoped) matters here - the productId in the URL
// (:id) is there for a RESTful, predictable path, not used as a filter.
export async function DELETE(request: Request, { params }: RouteContext) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  const { reviewId } = await params;
  try {
    const db = getDb();
    const deleted = await db
      .delete(productReviews)
      .where(and(eq(productReviews.id, reviewId), eq(productReviews.userId, session.userId)))
      .returning({ id: productReviews.id });
    if (deleted.length === 0) {
      return noStore({ error: "Review not found" }, 404);
    }
    return noStore({ ok: true });
  } catch (error) {
    console.error("product review delete failed", error);
    return noStore({ error: "Unable to delete this review right now" }, 500);
  }
}
