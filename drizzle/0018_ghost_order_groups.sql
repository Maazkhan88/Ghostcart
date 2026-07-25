ALTER TABLE `almost_buys` ADD `order_group_id` text;
CREATE INDEX `almost_buys_user_order_group_idx` ON `almost_buys` (`user_id`,`order_group_id`);
