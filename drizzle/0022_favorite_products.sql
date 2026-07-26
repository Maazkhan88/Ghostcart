CREATE TABLE `favorite_products` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`user_id` integer NOT NULL,
	`product_id` text NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE UNIQUE INDEX `favorite_products_user_product_idx` ON `favorite_products` (`user_id`,`product_id`);--> statement-breakpoint
CREATE INDEX `favorite_products_user_idx` ON `favorite_products` (`user_id`);