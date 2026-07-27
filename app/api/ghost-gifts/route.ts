import { env } from "cloudflare:workers";
import { getD1 } from "../../../db";
import type { EmailBinding } from "../../../lib/email";
import { sendGhostGiftEmail } from "../../../lib/email";
import {
  createGiftToken,
  hashGiftToken,
  hashRecipientEmail,
  readRecipientEmail,
  readRecipientName,
  safeSenderName,
} from "../../../lib/ghost-gifts";
import { consumeRateLimit, requestActorHash } from "../../../lib/rate-limit";
import { requireApiSession } from "../../../lib/session-auth";

const GIFT_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000;

function noStore(body: unknown, status = 200) {
  return Response.json(body, { status, headers: { "Cache-Control": "no-store" } });
}

export async function POST(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const actorLimit = await consumeRateLimit({
      namespace: "ghost-gift-create",
      actorHash: await requestActorHash(request),
      cost: 1,
      limit: 20,
      windowSeconds: 24 * 60 * 60,
    });
    if (!actorLimit.allowed) {
      return noStore({ error: "Too many gift attempts. Try again tomorrow." }, 429);
    }

    const payload = (await request.json()) as Record<string, unknown>;
    const almostBuyId = typeof payload.almostBuyId === "string" ? payload.almostBuyId.trim() : "";
    const recipientName = readRecipientName(payload.recipientName);
    const recipientEmail = readRecipientEmail(payload.recipientEmail);
    if (!almostBuyId || almostBuyId.length > 80) {
      return noStore({ error: "Choose an item to send as a gift" }, 400);
    }
    if (!recipientName) return noStore({ error: "Recipient name is invalid" }, 400);
    if (!recipientEmail) return noStore({ error: "Recipient email is invalid" }, 400);
    if (payload.recipientConsentConfirmed !== true) {
      return noStore({ error: "Confirm that the recipient expects this email" }, 400);
    }

    const db = getD1();
    const item = await db.prepare(
      `SELECT id, image_url AS imageUrl FROM almost_buys
       WHERE id = ? AND user_id = ? AND state IN ('captured', 'cooling', 'snoozed') LIMIT 1`,
    ).bind(almostBuyId, session.userId).first<{ id: string; imageUrl: string | null }>();
    if (!item) return noStore({ error: "That almost-buy is not available for gifting" }, 404);
    if (!item.imageUrl) {
      return noStore({ error: "Add a product picture before sending this gift" }, 400);
    }

    const recipientEmailHash = await hashRecipientEmail(recipientEmail);
    const [senderDaily, recipientDaily] = await db.batch([
      db.prepare(
        `SELECT COUNT(*) AS count FROM ghost_gifts
         WHERE sender_user_id = ? AND created_at >= datetime('now', '-1 day')`,
      ).bind(session.userId),
      db.prepare(
        `SELECT COUNT(*) AS count FROM ghost_gifts
         WHERE recipient_email_hash = ? AND created_at >= datetime('now', '-1 day')`,
      ).bind(recipientEmailHash),
    ]);
    const senderCount = Number((senderDaily.results?.[0] as { count?: number } | undefined)?.count ?? 0);
    const recipientCount = Number((recipientDaily.results?.[0] as { count?: number } | undefined)?.count ?? 0);
    if (senderCount >= 10 || recipientCount >= 10) {
      return noStore({ error: "Gift daily limit reached" }, 429);
    }

    const token = createGiftToken();
    const giftId = crypto.randomUUID();
    const now = new Date().toISOString();
    const expiresAt = new Date(Date.now() + GIFT_EXPIRY_MS).toISOString();
    await db.prepare(
      `INSERT INTO ghost_gifts
       (id, sender_user_id, almost_buy_id, recipient_email_hash, token_hash,
        status, expires_at, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, 'pending', ?, ?, ?)`,
    ).bind(
      giftId,
      session.userId,
      almostBuyId,
      recipientEmailHash,
      await hashGiftToken(token),
      expiresAt,
      now,
      now,
    ).run();

    const sent = await sendGhostGiftEmail(
      env.EMAIL as EmailBinding | undefined,
      recipientEmail,
      recipientName,
      safeSenderName(session.displayName),
      token,
    );
    if (!sent.ok) {
      await db.prepare("DELETE FROM ghost_gifts WHERE id = ?").bind(giftId).run();
      return noStore({ error: "The gift email could not be sent. Try again later." }, 503);
    }
    await db.prepare(
      "UPDATE ghost_gifts SET email_sent_at = ?, updated_at = ? WHERE id = ?",
    ).bind(now, now, giftId).run();

    return noStore({ ghostGift: { id: giftId, status: "pending", expiresAt } }, 201);
  } catch (error) {
    console.error("ghost gift creation failed", error);
    return noStore({ error: "Unable to send this gift right now" }, 500);
  }
}

export async function GET(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const db = getD1();
    const [sentResult, receivedResult] = await db.batch([
      db.prepare(
      `SELECT g.id, g.status, g.created_at AS createdAt, g.expires_at AS expiresAt,
              g.revealed_at AS revealedAt, a.title AS itemTitle,
              a.category, a.image_url AS imageUrl, a.almost_spent_cents AS amountCents
       FROM ghost_gifts g INNER JOIN almost_buys a ON a.id = g.almost_buy_id
       WHERE g.sender_user_id = ? ORDER BY g.created_at DESC LIMIT 50`,
      ).bind(session.userId),
      db.prepare(
        `SELECT g.id, g.status, g.created_at AS createdAt, g.expires_at AS expiresAt,
                g.revealed_at AS revealedAt, a.title AS itemTitle,
                a.category, a.image_url AS imageUrl, a.almost_spent_cents AS amountCents,
                COALESCE(NULLIF(TRIM(u.display_name), ''), 'A Ghost Cart member') AS senderName
         FROM ghost_gifts g
         INNER JOIN almost_buys a ON a.id = g.almost_buy_id
         INNER JOIN users u ON u.id = g.sender_user_id
         WHERE g.recipient_user_id = ? AND g.status = 'revealed'
         ORDER BY g.revealed_at DESC LIMIT 50`,
      ).bind(session.userId),
    ]);
    const sentGifts = sentResult.results ?? [];
    const receivedGifts = receivedResult.results ?? [];
    return noStore({ ghostGifts: sentGifts, sentGifts, receivedGifts });
  } catch (error) {
    console.error("ghost gift list failed", error);
    return noStore({ error: "Unable to load gifts" }, 500);
  }
}
