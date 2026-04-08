CREATE DATABASE IF NOT EXISTS inventory_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE inventory_seckill;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `image_url` VARCHAR(300) DEFAULT NULL COMMENT '商品图片URL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `seckill_order` (
    `id` BIGINT NOT NULL COMMENT '订单ID（雪花算法生成）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
    `price` DECIMAL(10,2) NOT NULL COMMENT '下单价格',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已取消 3-支付中',
    `expire_time` DATETIME DEFAULT NULL COMMENT '支付截止时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

CREATE TABLE IF NOT EXISTS `transaction_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `message_id` VARCHAR(64) NOT NULL COMMENT '业务消息唯一ID',
    `topic` VARCHAR(100) NOT NULL COMMENT 'Kafka Topic',
    `message_body` TEXT NOT NULL COMMENT '消息内容JSON',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待发送 1-已发送 2-已确认 3-发送失败',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry` INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务消息表（本地消息表模式）';

CREATE TABLE IF NOT EXISTS `tcc_transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '事务ID',
    `tx_id` VARCHAR(64) NOT NULL COMMENT '事务唯一标识',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'TRYING' COMMENT '事务状态: TRYING/CONFIRMED/CANCELLED',
    `tx_type` VARCHAR(50) NOT NULL COMMENT '事务类型: ORDER_PAY',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tx_id` (`tx_id`),
    KEY `idx_status` (`status`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TCC事务记录表';

INSERT IGNORE INTO `product` (name, description, price, stock, image_url) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB', 8999.00, 1000, '/static/img/iphone.png'),
('MacBook Pro 14', 'Apple MacBook Pro 14 M3 Pro', 16999.00, 500, '/static/img/macbook.png'),
('AirPods Pro 2', 'Apple AirPods Pro 第二代', 1899.00, 2000, '/static/img/airpods.png'),
('iPad Air', 'Apple iPad Air M1 256GB', 5499.00, 800, '/static/img/ipad.png'),
('Apple Watch S9', 'Apple Watch Series 9 45mm', 3299.00, 1500, '/static/img/watch.png'),
('Sony PS5', 'Sony PlayStation 5 光驱版', 3899.00, 600, '/static/img/ps5.png'),
('Nintendo Switch OLED', 'Nintendo Switch OLED 白色款', 2399.00, 1200, '/static/img/switch.png'),
('DJI Mini 4 Pro', 'DJI Mini 4 Pro 航拍无人机', 5788.00, 300, '/static/img/dji.png'),
('Samsung Galaxy S24 Ultra', 'Samsung Galaxy S24 Ultra 512GB', 9699.00, 700, '/static/img/samsung.png'),
('Dyson V15', 'Dyson V15 Detect 无线吸尘器', 4990.00, 400, '/static/img/dyson.png'),
('Bose QC Ultra', 'Bose QuietComfort Ultra 降噪耳机', 2999.00, 900, '/static/img/bose.png'),
('Steam Deck OLED', 'Valve Steam Deck OLED 1TB', 4599.00, 350, '/static/img/steamdeck.png'),
('Kindle Scribe', 'Amazon Kindle Scribe 10.2 英寸', 2499.00, 500, '/static/img/kindle.png'),
('GoPro Hero 12', 'GoPro Hero 12 Black 运动相机', 3398.00, 600, '/static/img/gopro.png'),
('Xiaomi 14 Pro', 'Xiaomi 14 Pro 骁龙8 Gen3 512GB', 4999.00, 1000, '/static/img/xiaomi.png');
