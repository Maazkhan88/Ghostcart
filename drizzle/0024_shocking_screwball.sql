CREATE TABLE `app_release_info` (
	`id` integer PRIMARY KEY NOT NULL,
	`latest_version_code` integer NOT NULL,
	`latest_version_name` text NOT NULL,
	`minimum_supported_version_code` integer DEFAULT 1 NOT NULL,
	`release_notes` text DEFAULT '' NOT NULL,
	`apk_url` text NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
