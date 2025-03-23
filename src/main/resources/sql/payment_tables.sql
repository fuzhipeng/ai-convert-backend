-- 创建订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `product_id` varchar(64) NOT NULL COMMENT '产品ID',
  `variant_id` varchar(64) NOT NULL COMMENT '产品变体ID',
  `product_name` varchar(255) NOT NULL COMMENT '产品名称',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态: 0-未支付 1-已支付 2-支付失败 3-已取消',
  `checkout_url` varchar(255) DEFAULT NULL COMMENT '结账URL',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '支付方式',
  `metadata` text DEFAULT NULL COMMENT '元数据(JSON格式)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `expire_time` datetime DEFAULT NULL COMMENT '订阅过期时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建订阅表
CREATE TABLE IF NOT EXISTS `subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `subscription_id` varchar(64) NOT NULL COMMENT '订阅ID',
  `plan_id` varchar(64) NOT NULL COMMENT '订阅计划ID',
  `plan_name` varchar(255) NOT NULL COMMENT '订阅计划名称',
  `status` varchar(20) NOT NULL COMMENT '状态：active, canceled, expired',
  `start_time` datetime NOT NULL COMMENT '订阅开始时间',
  `end_time` datetime NOT NULL COMMENT '订阅结束时间',
  `order_id` varchar(64) DEFAULT NULL COMMENT '关联订单ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subscription_id` (`subscription_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; 