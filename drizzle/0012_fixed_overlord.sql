PRAGMA foreign_keys=OFF;--> statement-breakpoint
CREATE TABLE `__new_users` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`email` text NOT NULL,
	`password_hash` text NOT NULL,
	`password_salt` text NOT NULL,
	`display_name` text,
	`is_admin` integer DEFAULT false NOT NULL,
	`username` text,
	`username_updated_at` text,
	`avatar_key` text,
	`community_consent` integer DEFAULT true NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
INSERT INTO `__new_users`("id", "email", "password_hash", "password_salt", "display_name", "is_admin", "username", "username_updated_at", "avatar_key", "community_consent", "created_at", "updated_at") SELECT "id", "email", "password_hash", "password_salt", "display_name", "is_admin", "username", "username_updated_at", "avatar_key", "community_consent", "created_at", "updated_at" FROM `users`;--> statement-breakpoint
DROP TABLE `users`;--> statement-breakpoint
ALTER TABLE `__new_users` RENAME TO `users`;--> statement-breakpoint
PRAGMA foreign_keys=ON;--> statement-breakpoint
CREATE UNIQUE INDEX `users_email_unique` ON `users` (`email`);--> statement-breakpoint
CREATE UNIQUE INDEX `users_username_unique` ON `users` (`username`);--> statement-breakpoint
-- Auto-enroll existing accounts too, not just future signups (user's
-- explicit choice - flip from opt-in to opt-out-by-default). Usernames are
-- generated lazily on next profile fetch (GET /api/me/profile), not here.
UPDATE `users` SET `community_consent` = 1 WHERE `community_consent` = 0;