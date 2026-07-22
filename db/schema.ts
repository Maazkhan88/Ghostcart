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

// Managed category list for the Products/Community tabs' dropdowns, so admin
// picks from an existing name instead of free-typing a near-duplicate
// ("Coffee" vs "Coffee & Drinks"). Products/community products still store
// category as plain text (unchanged, no FK) - this table is a picklist, not
// a hard constraint, so existing rows and any future one-off value never
// become invalid just because they're not in this list.
export const categories = sqliteTable("categories", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  name: text("name").notNull().unique(),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

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

// Retailer pages explicitly shared into Ghost Cart can become anonymous
// community discovery cards. Source URLs are canonicalized server-side and are
// never returned by the public feed.
export const communityProducts = sqliteTable(
  "community_products",
  {
    id: text("id").primaryKey(),
    canonicalKey: text("canonical_key").notNull(),
    canonicalUrl: text("canonical_url").notNull(),
    sourceDomain: text("source_domain").notNull(),
    title: text("title").notNull(),
    category: text("category").notNull().default("Other"),
    imageUrl: text("image_url"),
    priceCents: integer("price_cents").notNull().default(0),
    currencyCode: text("currency_code").notNull().default("AED"),
    ghostCount: integer("ghost_count").notNull().default(0),
    status: text("status").notNull().default("visible"),
    lastGhostedAt: text("last_ghosted_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    uniqueIndex("community_products_canonical_key_unique").on(table.canonicalKey),
    check(
      "community_products_status_check",
      sql`${table.status} IN ('visible', 'pending', 'hidden')`,
    ),
    check(
      "community_products_price_non_negative_check",
      sql`${table.priceCents} >= 0 AND ${table.ghostCount} >= 0`,
    ),
    index("community_products_status_last_idx").on(table.status, table.lastGhostedAt),
  ],
);

export const communityProductGhosts = sqliteTable(
  "community_product_ghosts",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    communityProductId: text("community_product_id")
      .notNull()
      .references(() => communityProducts.id, { onDelete: "cascade" }),
    actorHash: text("actor_hash").notNull(),
    source: text("source").notNull().default("unknown"),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    uniqueIndex("community_product_ghost_actor_unique").on(
      table.communityProductId,
      table.actorHash,
    ),
    index("community_product_ghost_created_idx").on(table.createdAt),
  ],
);
export const users = sqliteTable("users", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  email: text("email").notNull().unique(),
  passwordHash: text("password_hash").notNull(),
  passwordSalt: text("password_salt").notNull(),
  displayName: text("display_name"),
  // Grants access to /admin and its APIs. Never settable via any user-facing
  // API - only flipped directly in D1 by whoever operates the deployment.
  isAdmin: integer("is_admin", { mode: "boolean" }).notNull().default(false),
  // Public leaderboard identity - opt-out (user's explicit choice): every
  // account defaults to communityConsent = true with a lazily auto-generated
  // username (see GET /api/me/profile) the first time their profile is
  // fetched, rather than requiring an explicit opt-in action. Withdrawing
  // consent (communityConsent -> false) removes them from
  // GET /api/community/leaderboard immediately without deleting the
  // username, so re-opting back in keeps the same identity. This is a
  // separate, visibly distinct surface from the anonymous community product
  // feed (community_products) and must never weaken that feed's anonymity
  // guarantee.
  username: text("username").unique(),
  usernameUpdatedAt: text("username_updated_at"),
  avatarKey: text("avatar_key"),
  communityConsent: integer("community_consent", { mode: "boolean" }).notNull().default(true),
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
    pushSentAt: text("push_sent_at"),
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
    index("almost_buys_state_cool_off_push_idx").on(
      table.state,
      table.coolOffUntil,
      table.pushSentAt,
    ),
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

// Compact, public handoff records behind /ghost?s=<id>. These rows contain
// product-display metadata only and expire automatically after six months.
export const sharedGhostItems = sqliteTable(
  "shared_ghost_items",
  {
    id: text("id").primaryKey(),
    title: text("title").notNull(),
    category: text("category").notNull().default("Almost-buy"),
    priceCents: integer("price_cents").notNull().default(0),
    imageUrl: text("image_url"),
    sourceUrl: text("source_url"),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    expiresAt: text("expires_at").notNull(),
  },
  (table) => [
    check("shared_ghost_items_price_non_negative", sql`${table.priceCents} >= 0`),
    index("shared_ghost_items_expires_idx").on(table.expiresAt),
  ],
);

// Anonymous Ghost Checkout activity. Catalog products retain their foreign key;
// imported and app-local items use only their validated product key.
// actor_hash is a one-way, server-side pseudonym used solely for
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

export const communityProductsRelations = relations(
  communityProducts,
  ({ many }) => ({ ghosts: many(communityProductGhosts) }),
);

export const communityProductGhostsRelations = relations(
  communityProductGhosts,
  ({ one }) => ({
    product: one(communityProducts, {
      fields: [communityProductGhosts.communityProductId],
      references: [communityProducts.id],
    }),
  }),
);
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

// Admin-composed messages shown on app launch (custom in-app messaging, not the
// Firebase In-App Messaging SDK). image_url/link_url reference plain external
// URLs, not Phase 4's R2 content_blocks table - the two features are independent.
export const inAppMessages = sqliteTable(
  "in_app_messages",
  {
    id: text("id").primaryKey(),
    title: text("title").notNull(),
    body: text("body").notNull(),
    imageUrl: text("image_url"),
    linkUrl: text("link_url"),
    audience: text("audience").notNull().default("all"),
    isActive: integer("is_active", { mode: "boolean" }).notNull().default(true),
    sortOrder: integer("sort_order").notNull().default(0),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    check("in_app_messages_audience_check", sql`${table.audience} IN ('all')`),
    index("in_app_messages_active_sort_idx").on(table.isActive, table.sortOrder),
  ],
);

// The single current "this app is a simulation" consent text/version. Only ever
// one row (id = 1) - publishing a new version bumps `version` and updates
// `consentText`, which re-requires acceptance from everyone who accepted an
// earlier version (see simulationConsentAcceptances).
export const simulationConsentConfig = sqliteTable("simulation_consent_config", {
  id: integer("id").primaryKey(),
  version: integer("version").notNull().default(1),
  consentText: text("consent_text").notNull(),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

// One row per (actor, version accepted). actor_key is "user:<id>" for a signed-in
// account or "install:<installation id>" for an anonymous device - never a raw
// email or other directly-identifying value. Presence of a row for the *current*
// config version is what "accepted" means; no separate boolean is needed.
export const simulationConsentAcceptances = sqliteTable(
  "simulation_consent_acceptances",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    actorKey: text("actor_key").notNull(),
    version: integer("version").notNull(),
    locale: text("locale").notNull().default("en-AE"),
    acceptedAt: text("accepted_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    uniqueIndex("simulation_consent_actor_version_unique").on(table.actorKey, table.version),
    index("simulation_consent_actor_idx").on(table.actorKey),
  ],
);

// Admin-managed editorial media (home banners, "Ghost Cart Stories" cards).
// image_key is a server-generated R2 object key - never a user-supplied
// filename - and is the only thing deleteRecord()-style handlers need to also
// remove from R2 so no dangling reference or orphaned object is left behind.
export const contentBlocks = sqliteTable(
  "content_blocks",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    type: text("type").notNull(),
    imageKey: text("image_key").notNull(),
    mediaType: text("media_type").notNull().default("image"),
    linkType: text("link_type").notNull().default("none"),
    linkTargetId: text("link_target_id"),
    sortOrder: integer("sort_order").notNull().default(0),
    isActive: integer("is_active", { mode: "boolean" }).notNull().default(true),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    check("content_blocks_type_check", sql`${table.type} IN ('banner', 'story')`),
    check("content_blocks_media_type_check", sql`${table.mediaType} IN ('image', 'video')`),
    check(
      "content_blocks_video_only_for_story_check",
      sql`${table.mediaType} = 'image' OR ${table.type} = 'story'`,
    ),
    check(
      "content_blocks_link_type_check",
      sql`${table.linkType} IN ('none', 'product', 'category')`,
    ),
    index("content_blocks_type_active_sort_idx").on(
      table.type,
      table.isActive,
      table.sortOrder,
    ),
  ],
);

export const waitlistSignups = sqliteTable("waitlist_signups", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  email: text("email").notNull().unique(),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

// FCM registration tokens for signed-in users. A push can only be sent to an
// account that's both signed in and has registered a token from this device;
// anonymous/offline almost-buys are never pushed to.
export const deviceTokens = sqliteTable(
  "device_tokens",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    token: text("token").notNull().unique(),
    platform: text("platform").notNull().default("android"),
    createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
    updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  },
  (table) => [
    check("device_tokens_platform_check", sql`${table.platform} IN ('android', 'ios')`),
    index("device_tokens_user_idx").on(table.userId),
  ],
);
