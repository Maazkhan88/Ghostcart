CREATE TABLE `ghost_gifts` (
	`id` text PRIMARY KEY NOT NULL,
	`sender_user_id` integer NOT NULL,
	`almost_buy_id` text NOT NULL,
	`recipient_email_hash` text NOT NULL,
	`token_hash` text NOT NULL,
	`status` text DEFAULT 'pending' NOT NULL,
	`email_sent_at` text,
	`revealed_at` text,
	`withdrawn_at` text,
	`expires_at` text NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`sender_user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`almost_buy_id`) REFERENCES `almost_buys`(`id`) ON UPDATE no action ON DELETE cascade,
	CONSTRAINT `ghost_gifts_status_check` CHECK(`status` IN ('pending', 'revealed', 'withdrawn', 'expired', 'reported'))
);
--> statement-breakpoint
CREATE UNIQUE INDEX `ghost_gifts_almost_buy_unique` ON `ghost_gifts` (`almost_buy_id`);
--> statement-breakpoint
CREATE UNIQUE INDEX `ghost_gifts_token_hash_unique` ON `ghost_gifts` (`token_hash`);
--> statement-breakpoint
CREATE INDEX `ghost_gifts_sender_created_idx` ON `ghost_gifts` (`sender_user_id`,`created_at`);
--> statement-breakpoint
CREATE INDEX `ghost_gifts_recipient_created_idx` ON `ghost_gifts` (`recipient_email_hash`,`created_at`);
--> statement-breakpoint
CREATE INDEX `ghost_gifts_status_expires_idx` ON `ghost_gifts` (`status`,`expires_at`);
