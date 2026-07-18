import { ensureSharedGhostItemsTable } from "../../../../lib/shared-ghost-items";

type SharedItemRow = {
  id: string;
  title: string;
  category: string;
  priceCents: number;
  imageUrl: string | null;
  sourceUrl: string | null;
};

function json(data: unknown, init?: ResponseInit) {
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "public, max-age=60, s-maxage=300");
  headers.set("X-Content-Type-Options", "nosniff");
  return new Response(JSON.stringify(data), { ...init, headers });
}

export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  const { id } = await context.params;
  if (!/^[23456789A-HJ-NP-Za-km-z]{8}$/.test(id)) {
    return json({ error: "Shared item not found" }, { status: 404 });
  }
  try {
    const db = await ensureSharedGhostItemsTable();
    const item = await db
      .prepare(
        `SELECT id, title, category, price_cents AS priceCents,
                image_url AS imageUrl, source_url AS sourceUrl
         FROM shared_ghost_items
         WHERE id = ? AND expires_at > ?
         LIMIT 1`,
      )
      .bind(id, new Date().toISOString())
      .first<SharedItemRow>();
    return item ? json({ item }) : json({ error: "Shared item not found" }, { status: 404 });
  } catch {
    return json({ error: "Shared item is temporarily unavailable" }, { status: 503 });
  }
}
