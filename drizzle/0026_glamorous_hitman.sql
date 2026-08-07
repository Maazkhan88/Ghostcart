CREATE TABLE `product_reviews` (
	`id` text PRIMARY KEY NOT NULL,
	`product_id` text NOT NULL,
	`user_id` integer NOT NULL,
	`rating` integer NOT NULL,
	`text` text DEFAULT '' NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade,
	CONSTRAINT "product_reviews_rating_check" CHECK("product_reviews"."rating" BETWEEN 1 AND 5)
);
--> statement-breakpoint
CREATE INDEX `product_reviews_product_idx` ON `product_reviews` (`product_id`,`created_at`);--> statement-breakpoint
CREATE INDEX `product_reviews_user_idx` ON `product_reviews` (`user_id`);