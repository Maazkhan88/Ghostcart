import { getD1 } from "../../../db";
import {
  ALMOST_BUY_SELECT,
  jsonNoStore,
  serializeAlmostBuy,
  type AlmostBuyRecord,
} from "../../../lib/almost-buy-api";
import {
  isAlmostBuySourceKind,
  isAlmostBuyState,
  readIsoDate,
  readNonNegativeInteger,
  readSafeUrl,
  sanitizeShortText,
} from "../../../lib/backend-contract";
import { rehostProductImage } from "../../../lib/product-image-rehost";
import { requireApiSession } from "../../../lib/session-auth";

const MAX_PAGE_SIZE = 100;
const DEFAULT_PAGE_SIZE = 30;

type CatalogProduct = {
  id: number;
  name: string;
  category: string;
  imageUrl: string | null;
  priceCents: number;
};

export async function GET(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const url = new URL(request.url);
    const state = url.searchParams.get("state");
    if (state && !isAlmostBuyState(state)) {
      return jsonNoStore({ error: "state is invalid" }, { status: 400 });
    }
    const requestedLimit = Number(url.searchParams.get("limit"));
    const requestedOffset = Number(url.searchParams.get("offset"));
    const limit = Number.isInteger(requestedLimit)
      ? Math.min(Math.max(requestedLimit, 1), MAX_PAGE_SIZE)
      : DEFAULT_PAGE_SIZE;
    const offset = Number.isInteger(requestedOffset)
      ? Math.max(requestedOffset, 0)
      : 0;
    const stateClause = state ? " AND state = ?" : "";
    const listParams: Array<string | number> = [session.userId];
    if (state) listParams.push(state);
    listParams.push(limit, offset);

    const db = getD1();
    const [listResult, summaryResult, countResult] = await db.batch([
      db
        .prepare(
          `${ALMOST_BUY_SELECT}
           WHERE user_id = ?${stateClause}
           ORDER BY updated_at DESC, id DESC
           LIMIT ? OFFSET ?`,
        )
        .bind(...listParams),
      db
        .prepare(
          `SELECT
             COALESCE(SUM(almost_spent_cents), 0) AS totalAlmostSpentCents,
             COALESCE(SUM(CASE WHEN state IN ('captured', 'cooling', 'snoozed')
               THEN almost_spent_cents ELSE 0 END), 0) AS activeCoolingCents,
             COALESCE(SUM(CASE WHEN state = 'resolved_skipped'
               THEN confirmed_money_kept_cents ELSE 0 END), 0) AS confirmedMoneyKeptCents,
             COALESCE(SUM(CASE WHEN state = 'resolved_bought'
               THEN almost_spent_cents ELSE 0 END), 0) AS intentionallyBoughtCents
           FROM almost_buys WHERE user_id = ?`,
        )
        .bind(session.userId),
      db
        .prepare(`SELECT COUNT(*) AS total FROM almost_buys WHERE user_id = ?${stateClause}`)
        .bind(session.userId, ...(state ? [state] : [])),
    ]);

    const rows = (listResult.results ?? []) as AlmostBuyRecord[];
    const summary = summaryResult.results?.[0] as Record<string, number> | undefined;
    const total = Number((countResult.results?.[0] as { total?: number } | undefined)?.total ?? 0);
    return jsonNoStore({
      almostBuys: rows.map(serializeAlmostBuy),
      summary: {
        totalAlmostSpentCents: Number(summary?.totalAlmostSpentCents ?? 0),
        activeCoolingCents: Number(summary?.activeCoolingCents ?? 0),
        confirmedMoneyKeptCents: Number(summary?.confirmedMoneyKeptCents ?? 0),
        intentionallyBoughtCents: Number(summary?.intentionallyBoughtCents ?? 0),
        currencyCode: "AED",
      },
      pagination: { limit, offset, total, hasMore: offset + rows.length < total },
    });
  } catch {
    return jsonNoStore(
      { error: "Unable to load almost-buys right now" },
      { status: 500 },
    );
  }
}

export async function POST(request: Request) {
  try {
    const session = await requireApiSession(request);
    if (session instanceof Response) return session;
    const payload = (await request.json()) as Record<string, unknown>;
    const productIdResult = readNonNegativeInteger(payload.productId, "productId");
    if (productIdResult.error || productIdResult.value === 0) {
      return jsonNoStore(
        { error: productIdResult.error || "productId must be a positive integer" },
        { status: 400 },
      );
    }

    const db = getD1();
    let catalogProduct: CatalogProduct | null = null;
    if (productIdResult.value !== undefined) {
      catalogProduct = await db
        .prepare(
          `SELECT id, name, category, image_url AS imageUrl, price_cents AS priceCents
           FROM products WHERE id = ? AND is_active = 1 LIMIT 1`,
        )
        .bind(productIdResult.value)
        .first<CatalogProduct>();
      if (!catalogProduct) {
        return jsonNoStore(
          { error: "productId does not reference an active catalog product" },
          { status: 400 },
        );
      }
    }

    const title = sanitizeShortText(
      payload.title ?? catalogProduct?.name,
      "title",
      160,
      { required: true },
    );
    const category = sanitizeShortText(
      payload.category ?? catalogProduct?.category ?? "Other",
      "category",
      80,
      { required: true },
    );
    const trigger = sanitizeShortText(payload.trigger, "trigger", 80);
    const notes = sanitizeShortText(payload.notes ?? "", "notes", 500);
    const amount = readNonNegativeInteger(
      payload.almostSpentCents ?? catalogProduct?.priceCents ?? 0,
      "almostSpentCents",
    );
    const imageUrl = readSafeUrl(payload.imageUrl ?? catalogProduct?.imageUrl, "imageUrl");
    const sourceUrl = readSafeUrl(payload.sourceUrl, "sourceUrl");
    const coolOffUntil = readIsoDate(payload.coolOffUntil, "coolOffUntil", {
      future: true,
    });
    const validationError =
      title.error || category.error || trigger.error || notes.error || amount.error ||
      imageUrl.error || sourceUrl.error || coolOffUntil.error;
    if (validationError) {
      return jsonNoStore({ error: validationError }, { status: 400 });
    }
    const sourceKind = payload.sourceKind ?? (catalogProduct ? "catalog" : "manual");
    if (!isAlmostBuySourceKind(sourceKind)) {
      return jsonNoStore({ error: "sourceKind is invalid" }, { status: 400 });
    }
    if (payload.currencyCode !== undefined && payload.currencyCode !== "AED") {
      return jsonNoStore(
        { error: "Ghost Cart v2 currently supports AED catalog values only" },
        { status: 400 },
      );
    }
    const orderGroupId = sanitizeShortText(payload.orderGroupId, "orderGroupId", 80);
    if (orderGroupId.error) {
      return jsonNoStore({ error: orderGroupId.error }, { status: 400 });
    }

    // A scraped Amazon/retailer hotlink can go dead, get hotlink-blocked, or
    // just change (a later re-scrape of the same listing might pick a
    // different image). A copy we own doesn't have any of those failure
    // modes - also what makes the link-preview reuse fallback
    // (findKnownProductImage in app/api/link-preview/route.ts) permanently
    // reliable rather than only as reliable as whatever Amazon still serves.
    // Never blocks capture on failure: rehostProductImage() falls back to
    // the original URL on any error, so a hiccup here degrades to the
    // pre-existing raw-hotlink behavior, not a lost image.
    const storedImageUrl = imageUrl.value ? await rehostProductImage(imageUrl.value) : imageUrl.value;

    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    const state = coolOffUntil.value ? "cooling" : "captured";
    await db.batch([
      db
        .prepare(
          `INSERT INTO almost_buys (
             id, user_id, product_id, title, category, image_url, source_url,
             source_kind, order_group_id, trigger, notes, state, currency_code,
             almost_spent_cents, confirmed_money_kept_cents, cool_off_until,
             captured_at, updated_at, version
           ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'AED', ?, 0, ?, ?, ?, 1)`,
        )
        .bind(
          id,
          session.userId,
          catalogProduct?.id ?? null,
          title.value,
          category.value,
          storedImageUrl ?? null,
          sourceUrl.value ?? null,
          sourceKind,
          orderGroupId.value || null,
          trigger.value || null,
          notes.value ?? "",
          state,
          amount.value ?? 0,
          coolOffUntil.value ?? null,
          now,
          now,
        ),
      db
        .prepare(
          `INSERT INTO almost_buy_events
             (almost_buy_id, user_id, event_type, from_state, to_state, detail_json, created_at)
           VALUES (?, ?, ?, NULL, ?, ?, ?)`,
        )
        .bind(
          id,
          session.userId,
          state === "cooling" ? "captured_with_cooling" : "captured",
          state,
          JSON.stringify({ sourceKind }),
          now,
        ),
    ]);
    const row = await db
      .prepare(`${ALMOST_BUY_SELECT} WHERE id = ? AND user_id = ? LIMIT 1`)
      .bind(id, session.userId)
      .first<AlmostBuyRecord>();
    return jsonNoStore(
      { almostBuy: row ? serializeAlmostBuy(row) : null },
      { status: 201 },
    );
  } catch {
    return jsonNoStore(
      { error: "Unable to capture this almost-buy right now" },
      { status: 500 },
    );
  }
}
