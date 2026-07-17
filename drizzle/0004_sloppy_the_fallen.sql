CREATE TABLE `almost_buy_events` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`almost_buy_id` text NOT NULL,
	`user_id` integer NOT NULL,
	`event_type` text NOT NULL,
	`from_state` text,
	`to_state` text NOT NULL,
	`detail_json` text DEFAULT '{}' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`almost_buy_id`) REFERENCES `almost_buys`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `almost_buy_events_item_created_idx` ON `almost_buy_events` (`almost_buy_id`,`created_at`);--> statement-breakpoint
CREATE INDEX `almost_buy_events_user_created_idx` ON `almost_buy_events` (`user_id`,`created_at`);--> statement-breakpoint
CREATE TABLE `almost_buys` (
	`id` text PRIMARY KEY NOT NULL,
	`user_id` integer NOT NULL,
	`product_id` integer,
	`title` text NOT NULL,
	`category` text DEFAULT 'Other' NOT NULL,
	`image_url` text,
	`source_url` text,
	`source_kind` text DEFAULT 'manual' NOT NULL,
	`trigger` text,
	`notes` text DEFAULT '' NOT NULL,
	`state` text DEFAULT 'captured' NOT NULL,
	`currency_code` text DEFAULT 'AED' NOT NULL,
	`almost_spent_cents` integer DEFAULT 0 NOT NULL,
	`confirmed_money_kept_cents` integer DEFAULT 0 NOT NULL,
	`cool_off_until` text,
	`snoozed_until` text,
	`resolved_at` text,
	`captured_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`version` integer DEFAULT 1 NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON UPDATE no action ON DELETE set null,
	CONSTRAINT "almost_buys_state_check" CHECK("almost_buys"."state" IN ('captured', 'cooling', 'resolved_skipped', 'resolved_bought', 'snoozed', 'expired')),
	CONSTRAINT "almost_buys_source_kind_check" CHECK("almost_buys"."source_kind" IN ('manual', 'catalog', 'url', 'share', 'screenshot')),
	CONSTRAINT "almost_buys_amounts_non_negative_check" CHECK("almost_buys"."almost_spent_cents" >= 0 AND "almost_buys"."confirmed_money_kept_cents" >= 0)
);
--> statement-breakpoint
CREATE INDEX `almost_buys_user_state_updated_idx` ON `almost_buys` (`user_id`,`state`,`updated_at`);--> statement-breakpoint
CREATE INDEX `almost_buys_user_cooling_idx` ON `almost_buys` (`user_id`,`cool_off_until`);--> statement-breakpoint
CREATE TABLE `api_rate_limits` (
	`bucket_key` text PRIMARY KEY NOT NULL,
	`count` integer DEFAULT 0 NOT NULL,
	`expires_at` text NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE TABLE `user_preferences` (
	`user_id` integer PRIMARY KEY NOT NULL,
	`locale` text DEFAULT 'en-AE' NOT NULL,
	`time_zone` text DEFAULT 'Asia/Dubai' NOT NULL,
	`cooling_notifications` integer DEFAULT true NOT NULL,
	`lunch_reminder` integer DEFAULT false NOT NULL,
	`lunch_reminder_minute` integer DEFAULT 780 NOT NULL,
	`dinner_reminder` integer DEFAULT false NOT NULL,
	`dinner_reminder_minute` integer DEFAULT 1200 NOT NULL,
	`weekly_review` integer DEFAULT true NOT NULL,
	`reminder_paused_until` text,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `user_sessions` (
	`id` text PRIMARY KEY NOT NULL,
	`user_id` integer NOT NULL,
	`token_hash` text NOT NULL,
	`client_label` text,
	`expires_at` text NOT NULL,
	`revoked_at` text,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`last_seen_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE UNIQUE INDEX `user_sessions_token_hash_unique` ON `user_sessions` (`token_hash`);--> statement-breakpoint
CREATE INDEX `user_sessions_user_expires_idx` ON `user_sessions` (`user_id`,`expires_at`);--> statement-breakpoint
DROP INDEX `ghost_events_date_product_idx`;--> statement-breakpoint
ALTER TABLE `ghost_events` ADD `product_id` integer REFERENCES products(id);--> statement-breakpoint
ALTER TABLE `ghost_events` ADD `actor_hash` text;--> statement-breakpoint
CREATE INDEX `ghost_events_date_actor_idx` ON `ghost_events` (`event_date`,`actor_hash`);--> statement-breakpoint
CREATE INDEX `ghost_events_date_product_idx` ON `ghost_events` (`event_date`,`product_id`);--> statement-breakpoint
CREATE UNIQUE INDEX `ghost_events_daily_actor_product_unique` ON `ghost_events` (`event_date`,`actor_hash`,`product_id`);--> statement-breakpoint
ALTER TABLE `users` ADD `display_name` text;--> statement-breakpoint
ALTER TABLE `users` ADD `updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL;
