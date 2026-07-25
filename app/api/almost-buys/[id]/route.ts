import { getD1 } from "../../../../db";
import {
  ALMOST_BUY_SELECT,
  findOwnedAlmostBuy,
  jsonNoStore,
  serializeAlmostBuy,
  validAlmostBuyId,
  type AlmostBuyRecord,
} from "../../../../lib/almost-buy-api";
import {
  canMoveToCooling,
  type AlmostBuyState,
  readIsoDate,
  readNonNegativeInteger,
  readSafeUrl,
  sanitizeShortText,
} from "../../../../lib/backend-contract";
import { requireApiSession } from "../../../../lib/session-auth";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(request: Request, { params }: RouteContext) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const { id } = await params;
    if (!validAlmostBuyId(id)) {
      return jsonNoStore({ error: "invalid almost-buy id" }, { status: 400 });
    }
    const row = await findOwnedAlmostBuy(id, session.userId);
    if (!row) return jsonNoStore({ error: "almost-buy not found" }, { status: 404 });
    const history = await getD1()
      .prepare(
        `SELECT id, event_type AS eventType, from_state AS fromState,
                to_state AS toState, detail_json AS detailJson, created_at AS createdAt
         FROM almost_buy_events
         WHERE almost_buy_id = ? AND user_id = ?
         ORDER BY created_at ASC, id ASC`,
      )
      .bind(id, session.userId)
      .all();
    const historyRows = (history.results ?? []) as Array<{
      id: number;
      eventType: string;
      fromState: string | null;
      toState: string;
      detailJson: string;
      createdAt: string;
    }>;
    return jsonNoStore({
      almostBuy: serializeAlmostBuy(row),
      history: historyRows.map((event) => ({
        ...event,
        detail: safeJson(String(event.detailJson ?? "{}")),
        detailJson: undefined,
      })),
    });
  } catch {
    return jsonNoStore({ error: "Unable to load this almost-buy" }, { status: 500 });
  }
}

export async function PATCH(request: Request, { params }: RouteContext) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const { id } = await params;
    if (!validAlmostBuyId(id)) {
      return jsonNoStore({ error: "invalid almost-buy id" }, { status: 400 });
    }
    const current = await findOwnedAlmostBuy(id, session.userId);
    if (!current) return jsonNoStore({ error: "almost-buy not found" }, { status: 404 });

    const payload = (await request.json()) as Record<string, unknown>;
    const title = sanitizeShortText(payload.title, "title", 160);
    const category = sanitizeShortText(payload.category, "category", 80);
    const trigger = sanitizeShortText(payload.trigger, "trigger", 80);
    const notes = sanitizeShortText(payload.notes, "notes", 500);
    const amount = readNonNegativeInteger(payload.almostSpentCents, "almostSpentCents");
    const imageUrl = readSafeUrl(payload.imageUrl, "imageUrl");
    const sourceUrl = readSafeUrl(payload.sourceUrl, "sourceUrl");
    const coolOffUntil = readIsoDate(payload.coolOffUntil, "coolOffUntil", {
      future: payload.state === "cooling",
    });
    const validationError =
      title.error || category.error || trigger.error || notes.error || amount.error ||
      imageUrl.error || sourceUrl.error || coolOffUntil.error;
    if (validationError) {
      return jsonNoStore({ error: validationError }, { status: 400 });
    }
    if (payload.state !== undefined && payload.state !== "cooling") {
      return jsonNoStore(
        { error: "PATCH may only start or adjust cooling; use /resolve for outcomes" },
        { status: 400 },
      );
    }
    const nextState = payload.state === "cooling" ? "cooling" : current.state;
    if (
      nextState === "cooling" &&
      !canMoveToCooling(current.state as AlmostBuyState)
    ) {
      return jsonNoStore(
        { error: `a ${current.state} almost-buy cannot return to cooling` },
        { status: 409 },
      );
    }
    if (nextState === "cooling" && !coolOffUntil.value && !current.coolOffUntil) {
      return jsonNoStore(
        { error: "coolOffUntil is required to start cooling" },
        { status: 400 },
      );
    }
    const hasUpdate = [
      payload.title,
      payload.category,
      payload.trigger,
      payload.notes,
      payload.almostSpentCents,
      payload.imageUrl,
      payload.sourceUrl,
      payload.coolOffUntil,
      payload.state,
    ].some((value) => value !== undefined);
    if (!hasUpdate) {
      return jsonNoStore({ error: "no fields to update" }, { status: 400 });
    }

    const now = new Date().toISOString();
    const eventType = current.state !== nextState ? "cooling_started" : "updated";
    const db = getD1();
    const results = await db.batch([
      db
        .prepare(
          `UPDATE almost_buys SET
             title = ?, category = ?, image_url = ?, source_url = ?, trigger = ?,
             notes = ?, almost_spent_cents = ?, state = ?, cool_off_until = ?,
             snoozed_until = CASE WHEN ? = 'cooling' THEN NULL ELSE snoozed_until END,
             push_sent_at = CASE WHEN ? = 'cooling' THEN NULL ELSE push_sent_at END,
             updated_at = ?, version = version + 1
           WHERE id = ? AND user_id = ? AND version = ?`,
        )
        .bind(
          title.value ?? current.title,
          category.value ?? current.category,
          imageUrl.value === undefined ? current.imageUrl : imageUrl.value,
          sourceUrl.value === undefined ? current.sourceUrl : sourceUrl.value,
          trigger.value === undefined ? current.trigger : trigger.value || null,
          notes.value ?? current.notes,
          amount.value ?? current.almostSpentCents,
          nextState,
          coolOffUntil.value === undefined ? current.coolOffUntil : coolOffUntil.value,
          nextState,
          nextState,
          now,
          id,
          session.userId,
          current.version,
        ),
      db
        .prepare(
          `INSERT INTO almost_buy_events
             (almost_buy_id, user_id, event_type, from_state, to_state, detail_json, created_at)
           SELECT ?, ?, ?, ?, ?, '{}', ?
           WHERE EXISTS (
             SELECT 1 FROM almost_buys WHERE id = ? AND user_id = ? AND version = ?
           )`,
        )
        .bind(
          id,
          session.userId,
          eventType,
          current.state,
          nextState,
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
    return jsonNoStore({ error: "Unable to update this almost-buy" }, { status: 500 });
  }
}

function safeJson(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}
