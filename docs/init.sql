-- =========================================================
-- 演出门票抢购系统 数据库初始化脚本
-- =========================================================

CREATE DATABASE IF NOT EXISTS ticket_buy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticket_buy;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 演出类型枚举表
CREATE TABLE IF NOT EXISTS `event_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '类型ID',
    `name` VARCHAR(32) NOT NULL COMMENT '类型名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '类型描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出类型表';

-- 演出活动表
CREATE TABLE IF NOT EXISTS `event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '演出ID',
    `title` VARCHAR(256) NOT NULL COMMENT '演出标题',
    `category_id` BIGINT NOT NULL COMMENT '类型ID',
    `venue` VARCHAR(256) NOT NULL COMMENT '演出场馆',
    `address` VARCHAR(512) DEFAULT NULL COMMENT '详细地址',
    `event_date` DATETIME NOT NULL COMMENT '演出日期',
    `end_date` DATETIME DEFAULT NULL COMMENT '结束日期',
    `sale_start_time` DATETIME NOT NULL COMMENT '开售时间',
    `sale_end_time` DATETIME NOT NULL COMMENT '停售时间',
    `description` TEXT DEFAULT NULL COMMENT '演出描述',
    `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图片',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-待上架 2-在售 3-已售罄 4-已结束 5-已取消',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_sale_time` (`sale_start_time`, `sale_end_time`),
    KEY `idx_event_date` (`event_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出活动表';

-- 票档表（不同票价区域）
CREATE TABLE IF NOT EXISTS `ticket_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '票档ID',
    `event_id` BIGINT NOT NULL COMMENT '演出ID',
    `name` VARCHAR(64) NOT NULL COMMENT '票档名称（如VIP、普通等）',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '票价',
    `total_stock` INT NOT NULL COMMENT '总库存',
    `available_stock` INT NOT NULL COMMENT '可用库存',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '票档描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-在售 0-停售',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票档表';

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `event_id` BIGINT NOT NULL COMMENT '演出ID',
    `ticket_type_id` BIGINT NOT NULL COMMENT '票档ID',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态 1-待支付 2-已支付 3-已取消 4-已退款 5-已关闭',
    `pay_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '支付宝交易号',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `pay_url` VARCHAR(512) DEFAULT NULL COMMENT '支付链接',
    `expire_time` DATETIME NOT NULL COMMENT '订单过期时间',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 发票表
CREATE TABLE IF NOT EXISTS `invoice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '发票ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `invoice_no` VARCHAR(64) DEFAULT NULL COMMENT '发票号码',
    `title_type` TINYINT NOT NULL COMMENT '抬头类型 1-个人 2-企业',
    `title` VARCHAR(128) NOT NULL COMMENT '发票抬头',
    `tax_no` VARCHAR(32) DEFAULT NULL COMMENT '税号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '接收邮箱',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT '发票金额',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-待开具 2-已开具 3-已发送 4-已作废',
    `issued_at` DATETIME DEFAULT NULL COMMENT '开具时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票表';

-- 初始化演出类型数据
INSERT INTO `event_category` (`id`, `name`, `description`, `sort_order`) VALUES
(1, '脱口秀', '脱口秀演出', 1),
(2, '音乐会', '音乐会演出', 2),
(3, '演唱会', '演唱会演出', 3),
(4, '话剧', '话剧演出', 4);

-- 初始化测试用户（密码: 123456 的BCrypt加密）
INSERT INTO `user` (`username`, `password`, `phone`, `status`) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', 1),
('testuser2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138001', 1);

-- 初始化测试演出数据
INSERT INTO `event` (`id`, `title`, `category_id`, `venue`, `address`, `event_date`, `sale_start_time`, `sale_end_time`, `description`, `status`) VALUES
(1, '笑果脱口秀大会', 1, '笑果工厂', '上海市黄浦区南京东路800号', '2026-05-01 19:30:00', '2026-04-01 10:00:00', '2026-04-30 23:59:59', '笑果文化出品，众多脱口秀演员联合演出', 2),
(2, '周杰伦嘉年华巡回演唱会', 3, '梅赛德斯奔驰文化中心', '上海市浦东新区世博大道1200号', '2026-06-15 19:00:00', '2026-04-01 10:00:00', '2026-06-14 23:59:59', '周杰伦2026嘉年华世界巡回演唱会上海站', 2),
(3, '国家大剧院交响音乐会', 2, '国家大剧院', '北京市西城区西长安街2号', '2026-05-20 19:30:00', '2026-04-01 10:00:00', '2026-05-19 23:59:59', '国家大剧院管弦乐团年度交响音乐会', 2),
(4, '雷雨 话剧', 4, '上海大剧院', '上海市黄浦区人民大道300号', '2026-05-10 14:00:00', '2026-04-01 10:00:00', '2026-05-09 23:59:59', '曹禺经典话剧《雷雨》复排演出', 2);

-- 初始化票档数据
INSERT INTO `ticket_type` (`event_id`, `name`, `price`, `total_stock`, `available_stock`, `description`) VALUES
(1, 'VIP前排', 580.00, 100, 100, '前排VIP座位，含互动环节'),
(1, '普通票', 280.00, 500, 500, '普通区域座位'),
(2, '内场VIP', 1980.00, 200, 200, '内场VIP区域，近距离体验'),
(2, '内场普通', 1280.00, 500, 500, '内场普通区域'),
(2, '看台票', 680.00, 3000, 3000, '看台区域'),
(3, '一等座', 880.00, 200, 200, '一楼一等座'),
(3, '二等座', 480.00, 400, 400, '一楼二等座'),
(3, '三等座', 280.00, 600, 600, '二楼三等座'),
(4, '一等座', 680.00, 150, 150, '前排一等座'),
(4, '二等座', 380.00, 350, 350, '中排二等座'),
(4, '三等座', 180.00, 500, 500, '后排三等座');
