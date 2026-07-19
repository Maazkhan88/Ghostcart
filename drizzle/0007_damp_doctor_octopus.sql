CREATE TABLE `in_app_messages` (
	`id` text PRIMARY KEY NOT NULL,
	`title` text NOT NULL,
	`body` text NOT NULL,
	`image_url` text,
	`link_url` text,
	`audience` text DEFAULT 'all' NOT NULL,
	`is_active` integer DEFAULT true NOT NULL,
	`sort_order` integer DEFAULT 0 NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT "in_app_messages_audience_check" CHECK("in_app_messages"."audience" IN ('all'))
);
--> statement-breakpoint
CREATE INDEX `in_app_messages_active_sort_idx` ON `in_app_messages` (`is_active`,`sort_order`);--> statement-breakpoint
CREATE TABLE `simulation_consent_acceptances` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`actor_key` text NOT NULL,
	`version` integer NOT NULL,
	`locale` text DEFAULT 'en-AE' NOT NULL,
	`accepted_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `simulation_consent_actor_version_unique` ON `simulation_consent_acceptances` (`actor_key`,`version`);--> statement-breakpoint
CREATE INDEX `simulation_consent_actor_idx` ON `simulation_consent_acceptances` (`actor_key`);--> statement-breakpoint
CREATE TABLE `simulation_consent_config` (
	`id` integer PRIMARY KEY NOT NULL,
	`version` integer DEFAULT 1 NOT NULL,
	`consent_text` text NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
