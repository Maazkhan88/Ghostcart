import { relations, sql } from "drizzle-orm";
import { integer, sqliteTable, text } from "drizzle-orm/sqlite-core";

// Catalog data for the Ghost Cart marketplace simulation. Prices are stored
// as integer minor units (fils, i.e. price / 100 = AED) so the schema stays
// currency-symbol-agnostic — display formatting (including the official AED
// Dirham glyph) is a frontend concern, not a backend one. See AGENTS.md:
// this is catalog/content data only, never real payments.

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

export const merchantsRelations = relations(merchants, ({ many }) => ({
  products: many(products),
}));

export const productsRelations = relations(products, ({ one }) => ({
  merchant: one(merchants, {
    fields: [products.merchantId],
    references: [merchants.id],
  }),
}));
