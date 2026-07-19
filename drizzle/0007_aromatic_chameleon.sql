CREATE TABLE `content_blocks` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`type` text NOT NULL,
	`image_key` text NOT NULL,
	`link_type` text DEFAULT 'none' NOT NULL,
	`link_target_id` text,
	`sort_order` integer DEFAULT 0 NOT NULL,
	`is_active` integer DEFAULT true NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT "content_blocks_type_check" CHECK("content_blocks"."type" IN ('banner', 'story')),
	CONSTRAINT "content_blocks_link_type_check" CHECK("content_blocks"."link_type" IN ('none', 'product', 'category'))
);
--> statement-breakpoint
CREATE INDEX `content_blocks_type_active_sort_idx` ON `content_blocks` (`type`,`is_active`,`sort_order`);