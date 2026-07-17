CREATE TABLE `community_product_ghosts` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`community_product_id` text NOT NULL,
	`actor_hash` text NOT NULL,
	`source` text DEFAULT 'unknown' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`community_product_id`) REFERENCES `community_products`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE UNIQUE INDEX `community_product_ghost_actor_unique` ON `community_product_ghosts` (`community_product_id`,`actor_hash`);--> statement-breakpoint
CREATE INDEX `community_product_ghost_created_idx` ON `community_product_ghosts` (`created_at`);--> statement-breakpoint
CREATE TABLE `community_products` (
	`id` text PRIMARY KEY NOT NULL,
	`canonical_key` text NOT NULL,
	`canonical_url` text NOT NULL,
	`source_domain` text NOT NULL,
	`title` text NOT NULL,
	`category` text DEFAULT 'Other' NOT NULL,
	`image_url` text,
	`price_cents` integer DEFAULT 0 NOT NULL,
	`currency_code` text DEFAULT 'AED' NOT NULL,
	`ghost_count` integer DEFAULT 0 NOT NULL,
	`status` text DEFAULT 'visible' NOT NULL,
	`last_ghosted_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT "community_products_status_check" CHECK("community_products"."status" IN ('visible', 'pending', 'hidden')),
	CONSTRAINT "community_products_price_non_negative_check" CHECK("community_products"."price_cents" >= 0 AND "community_products"."ghost_count" >= 0)
);
--> statement-breakpoint
CREATE UNIQUE INDEX `community_products_canonical_key_unique` ON `community_products` (`canonical_key`);--> statement-breakpoint
CREATE INDEX `community_products_status_last_idx` ON `community_products` (`status`,`last_ghosted_at`);