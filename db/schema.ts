import { relations, sql } from "drizzle-orm";
import {
  check,
  index,
  integer,
  sqliteTable,
  text,
  uniqueIndex,
} from "drizzle-orm/sqlite-core";

// Ghost Cart stores catalog and behavioral-control data only. Prices are
// integer minor units used to describe an almost-buy; no row represents money
// held, transferred, charged, or deposited.

export const merchants = sqliteTable("merchants", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  name: text("name").notNull(),
  slug: text("slug").notNull().unique(),
  category: text("category").notNull(),
  logoUrl: text("logo_url"),
  description: text("description").notNull().default(""),
  isSponsored: integer("is_sponsored", { mode: "boolean" })
    .notNull()
    .default(false),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const products = sqliteTable("products", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  merchantId: integer("merchant_id")
    .notNull()
    .references(() => merchants.id, { onDelete: "cascade" }),
  name: text("name").notNull(),
  slug: text("slug").notNull().unique(),
  description: text("description").notNull().default(""),
  category: text("category").notNull(),
  priceCents: integer("price_cents").notNull().default(0),
  imageUrl: text("image_url"),
  isFlashDeal: integer("is_flash_deal", { mode: "boolean" })
    .notNull()
    .default(false),
  isMostGhosted: integer("is_most_ghosted", { mode: "boolean" })
    .notNull()
    .default(false),
  isActive: integer("is_active", { mode: "boolean" }).notNull().default(true),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const users = sqliteTable("users", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  email: text("email").notNull().unique(),
  passwordHash: text("password_hash").notNull(),
  passwordSalt: text("password_salt").notNull(),
  displayName: text("display_name"),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

// Android/iOS/web clients receive the opaque token. Only its SHA-256 digest is
// persisted, so a D1 read cannot be used directly as an active session.
export const userSessions = sqliteTable(
  "user_sessions",
  {
    id: text("id").primaryKey(),
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    tokenHash: text("token_hash").notNull(),
    clientLabel: text("client_label"),
    expiresAt: text("expires_at").notNull(),
    revokedAt: text("revoked_at"),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    lastSeenAt: text("last_seen_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    uniqueIndex("user_sessions_token_hash_unique").on(table.tokenHash),
    index("user_sessions_user_expires_idx").on(table.userId, table.expiresAt),
  ],
);

export const userPreferences = sqliteTable("user_preferences", {
  userId: integer("user_id")
    .primaryKey()
    .references(() => users.id, { onDelete: "cascade" }),
  locale: text("locale").notNull().default("en-AE"),
  timeZone: text("time_zone").notNull().default("Asia/Dubai"),
  coolingNotifications: integer("cooling_notifications", { mode: "boolean" })
    .notNull()
    .default(true),
  lunchReminder: integer("lunch_reminder", { mode: "boolean" })
    .notNull()
    .default(false),
  lunchReminderMinute: integer("lunch_reminder_minute").notNull().default(780),
  dinnerReminder: integer("dinner_reminder", { mode: "boolean" })
    .notNull()
    .default(false),
  dinnerReminderMinute: integer("dinner_reminder_minute").notNull().default(1200),
  weeklyReview: integer("weekly_review", { mode: "boolean" })
    .notNull()
    .default(true),
  reminderPausedUntil: text("reminder_paused_until"),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const almostBuys = sqliteTable(
  "almost_buys",
  {
    id: text("id").primaryKey(),
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    productId: integer("product_id").references(() => products.id, {
      onDelete: "set null",
    }),
    title: text("title").notNull(),
    category: text("category").notNull().default("Other"),
    imageUrl: text("image_url"),
    sourceUrl: text("source_url"),
    sourceKind: text("source_kind").notNull().default("manual"),
    trigger: text("trigger"),
    notes: text("notes").notNull().default(""),
    state: text("state").notNull().default("captured"),
    currencyCode: text("currency_code").notNull().default("AED"),
    almostSpentCents: integer("almost_spent_cents").notNull().default(0),
    confirmedMoneyKeptCents: integer("confirmed_money_kept_cents")
      .notNull()
      .default(0),
    coolOffUntil: text("cool_off_until"),
    snoozedUntil: text("snoozed_until"),
    resolvedAt: text("resolved_at"),
    capturedAt: text("captured_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    version: integer("version").notNull().default(1),
  },
  (table) => [
    check(
      "almost_buys_state_check",
      sql`${table.state} IN ('captured', 'cooling', 'resolved_skipped', 'resolved_bought', 'snoozed', 'expired')`,
    ),
    check(
      "almost_buys_source_kind_check",
      sql`${table.sourceKind} IN ('manual', 'catalog', 'url', 'share', 'screenshot')`,
    ),
    check(
      "almost_buys_amounts_non_negative_check",
      sql`${table.almostSpentCents} >= 0 AND ${table.confirmedMoneyKeptCents} >= 0`,
    ),
    index("almost_buys_user_state_updated_idx").on(
      table.userId,
      table.state,
      table.updatedAt,
    ),
    index("almost_buys_user_cooling_idx").on(table.userId, table.coolOffUntil),
  ],
);

// Immutable lifecycle entries preserve how an almost-buy reached its current
// state. detail_json contains app-control metadata only, never payment data.
export const almostBuyEvents = sqliteTable(
  "almost_buy_events",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    almostBuyId: text("almost_buy_id")
      .notNull()
      .references(() => almostBuys.id, { onDelete: "cascade" }),
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    eventType: text("event_type").notNull(),
    fromState: text("from_state"),
    toState: text("to_state").notNull(),
    detailJson: text("detail_json").notNull().default("{}"),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    index("almost_buy_events_item_created_idx").on(
      table.almostBuyId,
      table.createdAt,
    ),
    index("almost_buy_events_user_created_idx").on(table.userId, table.createdAt),
  ],
);

// Public trend input. Every new event must resolve to an active catalog
// product. actor_hash is a one-way, server-side pseudonym used solely for
// privacy thresholds and abuse controls; no email, price, or raw IP is stored.
export const ghostEvents = sqliteTable(
  "ghost_events",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    eventKey: text("event_key").notNull(),
    productKey: text("product_key").notNull(),
    productId: integer("product_id").references(() => products.id, {
      onDelete: "cascade",
    }),
    actorHash: text("actor_hash"),
    eventDate: text("event_date").notNull().default(sql`(DATE('now'))`),
    source: text("source").notNull().default("unknown"),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    uniqueIndex("ghost_events_event_key_unique").on(table.eventKey),
    uniqueIndex("ghost_events_daily_actor_product_unique").on(
      table.eventDate,
      table.actorHash,
      table.productId,
    ),
    index("ghost_events_date_product_idx").on(table.eventDate, table.productId),
    index("ghost_events_date_actor_idx").on(table.eventDate, table.actorHash),
  ],
);

// Fixed-window D1 counters. Keys contain a hashed actor and time bucket; raw
// network identifiers are never stored.
export const apiRateLimits = sqliteTable("api_rate_limits", {
  bucketKey: text("bucket_key").primaryKey(),
  count: integer("count").notNull().default(0),
  expiresAt: text("expires_at").notNull(),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const merchantsRelations = relations(merchants, ({ many }) => ({
  products: many(products),
}));

export const productsRelations = relations(products, ({ one, many }) => ({
  merchant: one(merchants, {
    fields: [products.merchantId],
    references: [merchants.id],
  }),
  almostBuys: many(almostBuys),
  ghostEvents: many(ghostEvents),
}));

export const usersRelations = relations(users, ({ many, one }) => ({
  sessions: many(userSessions),
  preferences: one(userPreferences),
  almostBuys: many(almostBuys),
}));

export const userSessionsRelations = relations(userSessions, ({ one }) => ({
  user: one(users, {
    fields: [userSessions.userId],
    references: [users.id],
  }),
}));

export const userPreferencesRelations = relations(userPreferences, ({ one }) => ({
  user: one(users, {
    fields: [userPreferences.userId],
    references: [users.id],
  }),
}));

export const almostBuysRelations = relations(almostBuys, ({ one, many }) => ({
  user: one(users, {
    fields: [almostBuys.userId],
    references: [users.id],
  }),
  product: one(products, {
    fields: [almostBuys.productId],
    references: [products.id],
  }),
  events: many(almostBuyEvents),
}));

export const almostBuyEventsRelations = relations(almostBuyEvents, ({ one }) => ({
  almostBuy: one(almostBuys, {
    fields: [almostBuyEvents.almostBuyId],
    references: [almostBuys.id],
  }),
  user: one(users, {
    fields: [almostBuyEvents.userId],
    references: [users.id],
  }),
}));

export const ghostEventsRelations = relations(ghostEvents, ({ one }) => ({
  product: one(products, {
    fields: [ghostEvents.productId],
    references: [products.id],
  }),
}));
