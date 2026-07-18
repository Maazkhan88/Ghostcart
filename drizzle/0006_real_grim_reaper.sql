CREATE TABLE `shared_ghost_items` (
	`id` text PRIMARY KEY NOT NULL,
	`title` text NOT NULL,
	`category` text DEFAULT 'Almost-buy' NOT NULL,
	`price_cents` integer DEFAULT 0 NOT NULL,
	`image_url` text,
	`source_url` text,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`expires_at` text NOT NULL,
	CONSTRAINT "shared_ghost_items_price_non_negative" CHECK("shared_ghost_items"."price_cents" >= 0)
);
--> statement-breakpoint
CREATE INDEX `shared_ghost_items_expires_idx` ON `shared_ghost_items` (`expires_at`);