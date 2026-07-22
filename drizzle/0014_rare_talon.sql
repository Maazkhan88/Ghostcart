CREATE TABLE `device_tokens` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`user_id` integer NOT NULL,
	`token` text NOT NULL,
	`platform` text DEFAULT 'android' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade,
	CONSTRAINT "device_tokens_platform_check" CHECK("device_tokens"."platform" IN ('android', 'ios'))
);
--> statement-breakpoint
CREATE UNIQUE INDEX `device_tokens_token_unique` ON `device_tokens` (`token`);--> statement-breakpoint
CREATE INDEX `device_tokens_user_idx` ON `device_tokens` (`user_id`);--> statement-breakpoint
ALTER TABLE `almost_buys` ADD `push_sent_at` text;--> statement-breakpoint
CREATE INDEX `almost_buys_state_cool_off_push_idx` ON `almost_buys` (`state`,`cool_off_until`,`push_sent_at`);