ALTER TABLE `users` ADD `apple_subject` text;--> statement-breakpoint
CREATE UNIQUE INDEX `users_apple_subject_unique` ON `users` (`apple_subject`);