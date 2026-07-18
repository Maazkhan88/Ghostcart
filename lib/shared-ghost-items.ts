import { getD1 } from "../db";

// Sites applies generated migrations during normal releases. This small guard
// also makes compact shares resilient when an existing D1 binding predates the
// migration, which is important because Android safely falls back to the
// legacy query-string handoff when this API is unavailable.
export async function ensureSharedGhostItemsTable() {
  const db = getD1();
  await db
    .prepare(
      `CREATE TABLE IF NOT EXISTS shared_ghost_items (
        id TEXT PRIMARY KEY NOT NULL,
        title TEXT NOT NULL,
        category TEXT DEFAULT 'Almost-buy' NOT NULL,
        price_cents INTEGER DEFAULT 0 NOT NULL,
        image_url TEXT,
        source_url TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP NOT NULL,
        expires_at TEXT NOT NULL,
        CONSTRAINT shared_ghost_items_price_non_negative CHECK(price_cents >= 0)
      )`,
    )
    .run();
  await db
    .prepare(
      "CREATE INDEX IF NOT EXISTS shared_ghost_items_expires_idx ON shared_ghost_items (expires_at)",
    )
    .run();
  return db;
}
