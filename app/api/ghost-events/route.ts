import { getD1 } from "../../../db";
import { toRouteErrorMessage } from "../../../lib/api-helpers";

const MAX_PRODUCTS_PER_CHECKOUT = 40;
const DEFAULT_RANKING_LIMIT = 12;
const MAX_RANKING_LIMIT = 30;
const SAFE_ID = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$/;
const MISSING_TABLE_HINT =
  "The ghost activity table is unavailable. Generate and deploy the latest D1 migration before using live rankings.";

type GhostEventPayload = {
  checkoutId?: string;
  productIds?: string[];
  source?: string;
};

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}

export async function GET(request: Request) {
  try {
    const requestedLimit = Number(new URL(request.url).searchParams.get("limit"));
    const limit = Number.isInteger(requestedLimit)
      ? Math.min(Math.max(requestedLimit, 1), MAX_RANKING_LIMIT)
      : DEFAULT_RANKING_LIMIT;

    const db = getD1();
    const [rankingResult, totalResult] = await db.batch([
      db.prepare(
        `SELECT product_key AS productId, COUNT(*) AS ghostCount
         FROM ghost_events
         WHERE event_date = DATE('now')
         GROUP BY product_key
         ORDER BY ghostCount DESC, product_key ASC
         LIMIT ?`,
      )
        .bind(limit),
      db.prepare(
        `SELECT COUNT(*) AS totalGhosts
         FROM ghost_events
         WHERE event_date = DATE('now')`,
      ),
    ]);

    const rankingRows = (rankingResult.results ?? []) as Array<{
      productId: string;
      ghostCount: number;
    }>;
    const totalRow = (totalResult.results?.[0] ?? { totalGhosts: 0 }) as {
      totalGhosts: number;
    };
    const rankings = rankingRows.map((row, index) => ({
      rank: index + 1,
      productId: row.productId,
      ghostCount: Number(row.ghostCount),
    }));

    return json(
      {
        period: "today",
        date: new Date().toISOString().slice(0, 10),
        totalGhosts: Number(totalRow.totalGhosts),
        rankings,
      },
      { headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    return json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as GhostEventPayload;
    const checkoutId = payload.checkoutId?.trim() ?? "";
    const productIds = Array.from(
      new Set((payload.productIds ?? []).map((productId) => productId.trim()).filter(Boolean)),
    );
    const source = payload.source?.trim().toLowerCase() ?? "unknown";

    if (!SAFE_ID.test(checkoutId)) {
      return json({ error: "checkoutId is invalid" }, { status: 400 });
    }
    if (!productIds.length || productIds.length > MAX_PRODUCTS_PER_CHECKOUT) {
      return json(
        { error: `productIds must contain between 1 and ${MAX_PRODUCTS_PER_CHECKOUT} unique items` },
        { status: 400 },
      );
    }
    if (productIds.some((productId) => !SAFE_ID.test(productId))) {
      return json({ error: "one or more productIds are invalid" }, { status: 400 });
    }
    if (!/^(android|web|ios|unknown)$/.test(source)) {
      return json({ error: "source is invalid" }, { status: 400 });
    }

    const db = getD1();
    const statements = productIds.map((productId) =>
      db
        .prepare(
          `INSERT INTO ghost_events (event_key, product_key, source)
           VALUES (?, ?, ?)
           ON CONFLICT(event_key) DO NOTHING`,
        )
        .bind(`${checkoutId}:${productId}`, productId, source),
    );
    const results = await db.batch(statements);
    const recorded = results.reduce(
      (total, result) => total + (Number(result.meta?.changes ?? 0) > 0 ? 1 : 0),
      0,
    );

    return json(
      { recorded, duplicateCount: productIds.length - recorded },
      { status: recorded > 0 ? 201 : 200, headers: { "Cache-Control": "no-store" } },
    );
  } catch (error) {
    return json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
