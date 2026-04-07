CREATE DATABASE IF NOT EXISTS seckill_order_1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seckill_order_1;

CREATE TABLE IF NOT EXISTS `seckill_order_0` (
    `id` BIGINT NOT NULL COMMENT '订单ID（雪花算法生成）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '下单价格',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表_0';

CREATE TABLE IF NOT EXISTS `seckill_order_1` (
    `id` BIGINT NOT NULL COMMENT '订单ID（雪花算法生成）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '下单价格',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表_1';
