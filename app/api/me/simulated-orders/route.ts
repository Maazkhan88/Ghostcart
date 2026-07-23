import { randomUUID } from "node:crypto";
import { getDb } from "../../../../db";
import { simulatedOrders } from "../../../../db/schema";
import { jsonNoStore } from "../../../../lib/almost-buy-api";
import { readNonNegativeInteger, sanitizeShortText } from "../../../../lib/backend-contract";
import { requireApiSession } from "../../../../lib/session-auth";

// Records a completed marketplace-cart simulated checkout against the
// signed-in account, so it can count toward that account's "Ghosted" stat
// on the Community Leaderboard. Deliberately separate from the anonymous
// POST /api/ghost-events (hashed actor, no user_id, used only for the
// privacy-gated "Most Ghosted Today" trend) - do not merge these two, one
// is intentionally anonymous and this one is intentionally attributable to
// the caller's own account only.
export async function POST(request: Request) {
  const session = await requireApiSession(request);
  if (session instanceof Response) return session;

  try {
    const payload = (await request.json()) as {
      orderId?: unknown;
      totalCents?: unknown;
      itemCount?: unknown;
    };
    const orderId = sanitizeShortText(payload.orderId, "orderId", 40, { required: true });
    if (orderId.error) return jsonNoStore({ error: orderId.error }, { status: 400 });
    const totalCents = readNonNegativeInteger(payload.totalCents, "totalCents");
    if (totalCents.error) return jsonNoStore({ error: totalCents.error }, { status: 400 });
    const itemCount = readNonNegativeInteger(payload.itemCount, "itemCount");
    if (itemCount.error) return jsonNoStore({ error: itemCount.error }, { status: 400 });

    const db = getDb();
    await db
      .insert(simulatedOrders)
      .values({
        id: randomUUID(),
        userId: session.userId,
        orderId: orderId.value!,
        totalCents: totalCents.value ?? 0,
        itemCount: Math.max(1, itemCount.value ?? 1),
      })
      .onConflictDoNothing({ target: [simulatedOrders.userId, simulatedOrders.orderId] });

    return jsonNoStore({ recorded: true }, { status: 201 });
  } catch {
    return jsonNoStore({ error: "Unable to record this order right now" }, { status: 500 });
  }
}
