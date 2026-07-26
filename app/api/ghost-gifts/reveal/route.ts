import { getD1 } from "../../../../db";
import { hashGiftToken, hashRecipientEmail, isGiftToken } from "../../../../lib/ghost-gifts";
import { consumeRateLimit, requestActorHash } from "../../../../lib/rate-limit";
import { authenticateApiRequest } from "../../../../lib/session-auth";

function noStore(body: unknown, status = 200) {
  return Response.json(body, { status, headers: { "Cache-Control": "no-store" } });
}

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as Record<string, unknown>;
    const token = typeof payload.token === "string" ? payload.token.trim() : "";
    if (!isGiftToken(token)) return noStore({ error: "This gift link is invalid" }, 400);
    if (payload.acceptedPrivacy !== true) {
      return noStore({ error: "Accept the privacy notice to reveal this gift" }, 400);
    }
    const limit = await consumeRateLimit({
      namespace: "ghost-gift-reveal",
      actorHash: await requestActorHash(request),
      cost: 1,
      limit: 30,
      windowSeconds: 60 * 60,
    });
    if (!limit.allowed) return noStore({ error: "Too many reveal attempts. Try again later." }, 429);

    const db = getD1();
    const session = await authenticateApiRequest(request);
    const row = await db.prepare(
      `SELECT g.id, g.status, g.expires_at AS expiresAt,
              g.recipient_email_hash AS recipientEmailHash,
              a.title, a.category, a.image_url AS imageUrl,
              a.almost_spent_cents AS amountCents, u.display_name AS senderName
       FROM ghost_gifts g
       INNER JOIN almost_buys a ON a.id = g.almost_buy_id
       INNER JOIN users u ON u.id = g.sender_user_id
       WHERE g.token_hash = ? LIMIT 1`,
    ).bind(await hashGiftToken(token)).first<{
      id: string; status: string; expiresAt: string; recipientEmailHash: string; title: string;
      category: string; imageUrl: string | null; amountCents: number;
      senderName: string | null;
    }>();
    if (!row) return noStore({ error: "This gift is no longer available" }, 404);
    if (row.status === "withdrawn" || row.status === "reported") {
      return noStore({ error: "This gift is no longer available" }, 410);
    }
    if (row.status === "expired" || Date.parse(row.expiresAt) <= Date.now()) {
      await db.prepare(
        "UPDATE ghost_gifts SET status = 'expired', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
      ).bind(row.id).run();
      return noStore({ error: "This gift link has expired" }, 410);
    }
    if (row.status === "pending") {
      await db.prepare(
        `UPDATE ghost_gifts SET status = 'revealed', revealed_at = CURRENT_TIMESTAMP,
         updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'pending'`,
      ).bind(row.id).run();
    }
    if (session && await hashRecipientEmail(session.email) === row.recipientEmailHash) {
      await db.prepare(
        `UPDATE ghost_gifts SET recipient_user_id = ?, updated_at = CURRENT_TIMESTAMP
         WHERE id = ? AND (recipient_user_id IS NULL OR recipient_user_id = ?)`,
      ).bind(session.userId, row.id, session.userId).run();
    }
    return noStore({ ghostGift: {
      id: row.id,
      senderName: row.senderName?.trim() || "A Ghost Cart member",
      title: row.title,
      category: row.category,
      imageUrl: row.imageUrl,
      amountCents: Number(row.amountCents),
      currencyCode: "AED",
      disclosure: "Simulation only. No gift was purchased or sent. No real payment or delivery occurred.",
    } });
  } catch (error) {
    console.error("ghost gift reveal failed", error);
    return noStore({ error: "Unable to reveal this gift right now" }, 500);
  }
}
