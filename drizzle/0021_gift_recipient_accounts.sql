ALTER TABLE `ghost_gifts` ADD COLUMN `recipient_user_id` integer REFERENCES `users`(`id`) ON DELETE SET NULL;
--> statement-breakpoint
CREATE INDEX `ghost_gifts_recipient_user_created_idx` ON `ghost_gifts` (`recipient_user_id`,`created_at`);
