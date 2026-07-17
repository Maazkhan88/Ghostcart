CREATE TABLE `ghost_events` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`event_key` text NOT NULL,
	`product_key` text NOT NULL,
	`event_date` text DEFAULT (DATE('now')) NOT NULL,
	`source` text DEFAULT 'unknown' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `ghost_events_event_key_unique` ON `ghost_events` (`event_key`);--> statement-breakpoint
CREATE INDEX `ghost_events_date_product_idx` ON `ghost_events` (`event_date`,`product_key`);