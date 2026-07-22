PRAGMA foreign_keys=OFF;--> statement-breakpoint
CREATE TABLE `__new_content_blocks` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`type` text NOT NULL,
	`image_key` text NOT NULL,
	`media_type` text DEFAULT 'image' NOT NULL,
	`link_type` text DEFAULT 'none' NOT NULL,
	`link_target_id` text,
	`sort_order` integer DEFAULT 0 NOT NULL,
	`is_active` integer DEFAULT true NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT "content_blocks_type_check" CHECK("__new_content_blocks"."type" IN ('banner', 'story')),
	CONSTRAINT "content_blocks_media_type_check" CHECK("__new_content_blocks"."media_type" IN ('image', 'video')),
	CONSTRAINT "content_blocks_video_only_for_story_check" CHECK("__new_content_blocks"."media_type" = 'image' OR "__new_content_blocks"."type" = 'story'),
	CONSTRAINT "content_blocks_link_type_check" CHECK("__new_content_blocks"."link_type" IN ('none', 'product', 'category'))
);
--> statement-breakpoint
INSERT INTO `__new_content_blocks`("id", "type", "image_key", "link_type", "link_target_id", "sort_order", "is_active", "created_at", "updated_at") SELECT "id", "type", "image_key", "link_type", "link_target_id", "sort_order", "is_active", "created_at", "updated_at" FROM `content_blocks`;--> statement-breakpoint
DROP TABLE `content_blocks`;--> statement-breakpoint
ALTER TABLE `__new_content_blocks` RENAME TO `content_blocks`;--> statement-breakpoint
PRAGMA foreign_keys=ON;--> statement-breakpoint
CREATE INDEX `content_blocks_type_active_sort_idx` ON `content_blocks` (`type`,`is_active`,`sort_order`);