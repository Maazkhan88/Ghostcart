import { getD1 } from "../../../../../db";
import {
  ALMOST_BUY_SELECT,
  findOwnedAlmostBuy,
  jsonNoStore,
  serializeAlmostBuy,
  validAlmostBuyId,
  type AlmostBuyRecord,
} from "../../../../../lib/almost-buy-api";
import {
  canResolve,
  isResolutionOutcome,
  readIsoDate,
  readNonNegativeInteger,
  resolutionState,
  sanitizeShortText,
  type AlmostBuyState,
} from "../../../../../lib/backend-contract";
import { requireApiSession } from "../../../../../lib/session-auth";

type RouteContext = { params: Promise<{ id: string }> };

export async function POST(request: Request, { params }: RouteContext) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const { id } = await params;
    if (!validAlmostBuyId(id)) {
      return jsonNoStore({ error: "invalid almost-buy id" }, { status: 400 });
    }
    const current = await findOwnedAlmostBuy(id, session.userId);
    if (!current) return jsonNoStore({ error: "almost-buy not found" }, { status: 404 });
    if (!canResolve(current.state as AlmostBuyState)) {
      return jsonNoStore(
        { error: `this almost-buy is already ${current.state}` },
        { status: 409 },
      );
    }

    const payload = (await request.json()) as Record<string, unknown>;
    if (!isResolutionOutcome(payload.outcome)) {
      return jsonNoStore({ error: "outcome is invalid" }, { status: 400 });
    }
    const note = sanitizeShortText(payload.note ?? "", "note", 500);
    if (note.error) return jsonNoStore({ error: note.error }, { status: 400 });
    const kept = readNonNegativeInteger(
      payload.confirmedMoneyKeptCents,
      "confirmedMoneyKeptCents",
    );
    if (kept.error) return jsonNoStore({ error: kept.error }, { status: 400 });
    const snoozedUntil = readIsoDate(payload.snoozedUntil, "snoozedUntil", {
      required: payload.outcome === "snoozed",
      future: payload.outcome === "snoozed",
    });
    if (snoozedUntil.error) {
      return jsonNoStore({ error: snoozedUntil.error }, { status: 400 });
    }
    if (payload.outcome !== "skipped" && kept.value !== undefined) {
      return jsonNoStore(
        { error: "confirmedMoneyKeptCents is only valid for a skipped purchase" },
        { status: 400 },
      );
    }
    const confirmedMoneyKeptCents =
      payload.outcome === "skipped"
        ? (kept.value ?? current.almostSpentCents)
        : 0;
    if (confirmedMoneyKeptCents > current.almostSpentCents) {
      return jsonNoStore(
        { error: "confirmedMoneyKeptCents cannot exceed almostSpentCents" },
        { status: 400 },
      );
    }

    const nextState = resolutionState(payload.outcome);
    const now = new Date().toISOString();
    const resolvedAt = payload.outcome === "snoozed" ? null : now;
    const db = getD1();
    const results = await db.batch([
      db
        .prepare(
          `UPDATE almost_buys SET
             state = ?, confirmed_money_kept_cents = ?, snoozed_until = ?,
             cool_off_until = CASE WHEN ? = 'snoozed' THEN ? ELSE cool_off_until END,
             resolved_at = ?, updated_at = ?, version = version + 1
           WHERE id = ? AND user_id = ? AND version = ?`,
        )
        .bind(
          nextState,
          confirmedMoneyKeptCents,
          snoozedUntil.value ?? null,
          nextState,
          snoozedUntil.value ?? null,
          resolvedAt,
          now,
          id,
          session.userId,
          current.version,
        ),
      db
        .prepare(
          `INSERT INTO almost_buy_events
             (almost_buy_id, user_id, event_type, from_state, to_state, detail_json, created_at)
           SELECT ?, ?, 'resolved', ?, ?, ?, ?
           WHERE EXISTS (
             SELECT 1 FROM almost_buys WHERE id = ? AND user_id = ? AND version = ?
           )`,
        )
        .bind(
          id,
          session.userId,
          current.state,
          nextState,
          JSON.stringify({
            outcome: payload.outcome,
            note: note.value,
            confirmedMoneyKeptCents,
          }),
          now,
          id,
          session.userId,
          current.version + 1,
        ),
    ]);
    if (Number(results[0].meta?.changes ?? 0) === 0) {
      return jsonNoStore(
        { error: "This almost-buy changed elsewhere; reload and try again" },
        { status: 409 },
      );
    }
    const row = await db
      .prepare(`${ALMOST_BUY_SELECT} WHERE id = ? AND user_id = ? LIMIT 1`)
      .bind(id, session.userId)
      .first<AlmostBuyRecord>();
    return jsonNoStore({ almostBuy: row ? serializeAlmostBuy(row) : null });
  } catch {
    return jsonNoStore({ error: "Unable to resolve this almost-buy" }, { status: 500 });
  }
}
