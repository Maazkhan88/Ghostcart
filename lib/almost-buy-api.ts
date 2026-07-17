import { getD1 } from "../db";

export const ALMOST_BUY_SELECT = `
  SELECT
    id,
    user_id AS userId,
    product_id AS productId,
    title,
    category,
    image_url AS imageUrl,
    source_url AS sourceUrl,
    source_kind AS sourceKind,
    trigger,
    notes,
    state,
    currency_code AS currencyCode,
    almost_spent_cents AS almostSpentCents,
    confirmed_money_kept_cents AS confirmedMoneyKeptCents,
    cool_off_until AS coolOffUntil,
    snoozed_until AS snoozedUntil,
    resolved_at AS resolvedAt,
    captured_at AS capturedAt,
    updated_at AS updatedAt,
    version
  FROM almost_buys`;

export type AlmostBuyRecord = {
  id: string;
  userId: number;
  productId: number | null;
  title: string;
  category: string;
  imageUrl: string | null;
  sourceUrl: string | null;
  sourceKind: string;
  trigger: string | null;
  notes: string;
  state: string;
  currencyCode: string;
  almostSpentCents: number;
  confirmedMoneyKeptCents: number;
  coolOffUntil: string | null;
  snoozedUntil: string | null;
  resolvedAt: string | null;
  capturedAt: string;
  updatedAt: string;
  version: number;
};

export function serializeAlmostBuy(row: AlmostBuyRecord) {
  const coolingTarget = row.state === "snoozed" ? row.snoozedUntil : row.coolOffUntil;
  return {
    ...row,
    productId: row.productId === null ? null : Number(row.productId),
    userId: Number(row.userId),
    almostSpentCents: Number(row.almostSpentCents),
    confirmedMoneyKeptCents: Number(row.confirmedMoneyKeptCents),
    version: Number(row.version),
    coolingReady:
      Boolean(coolingTarget) && Date.parse(coolingTarget as string) <= Date.now(),
  };
}

export async function findOwnedAlmostBuy(
  id: string,
  userId: number,
): Promise<AlmostBuyRecord | null> {
  return getD1()
    .prepare(`${ALMOST_BUY_SELECT} WHERE id = ? AND user_id = ? LIMIT 1`)
    .bind(id, userId)
    .first<AlmostBuyRecord>();
}

export function validAlmostBuyId(id: string): boolean {
  return /^[a-f0-9-]{36}$/i.test(id);
}

export function jsonNoStore(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Cache-Control", "no-store");
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}
