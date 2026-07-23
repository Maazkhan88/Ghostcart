CREATE TABLE `simulated_orders` (
	`id` text PRIMARY KEY NOT NULL,
	`user_id` integer NOT NULL,
	`order_id` text NOT NULL,
	`total_cents` integer DEFAULT 0 NOT NULL,
	`item_count` integer DEFAULT 1 NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON UPDATE no action ON DELETE cascade,
	CONSTRAINT "simulated_orders_total_cents_check" CHECK("simulated_orders"."total_cents" >= 0),
	CONSTRAINT "simulated_orders_item_count_check" CHECK("simulated_orders"."item_count" >= 1)
);
--> statement-breakpoint
CREATE UNIQUE INDEX `simulated_orders_user_order_unique` ON `simulated_orders` (`user_id`,`order_id`);--> statement-breakpoint
CREATE INDEX `simulated_orders_user_idx` ON `simulated_orders` (`user_id`);