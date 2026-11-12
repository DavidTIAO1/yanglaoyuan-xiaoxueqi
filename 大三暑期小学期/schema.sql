-- ============================================
-- 社区药品配送服务 - 数据库建表脚本
-- 数据库: auto_medicine_order
-- ============================================

USE auto_medicine_order;

-- -------------------------------------------
-- 1. users - 用户表
-- -------------------------------------------
CREATE TABLE users (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    family_id   VARCHAR(10)  UNIQUE                     COMMENT '亲友标识(10位随机数字,注册时自动生成)',
    username    VARCHAR(50)  UNIQUE                     COMMENT '登录名',
    password    VARCHAR(255) NOT NULL                   COMMENT 'BCrypt加密密码',
    real_name   VARCHAR(50)                             COMMENT '真实姓名',
    phone       VARCHAR(20)  UNIQUE                     COMMENT '手机号',
    role        ENUM('USER','MERCHANT') NOT NULL        COMMENT '角色',
    address     VARCHAR(255)                            COMMENT '住址',
    latitude    DECIMAL(10,6)                           COMMENT '纬度(用于就近匹配)',
    longitude   DECIMAL(10,6)                           COMMENT '经度',
    status      TINYINT      DEFAULT 1                  COMMENT '1启用/0禁用',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -------------------------------------------
-- 2. merchants - 商家表
-- -------------------------------------------
CREATE TABLE merchants (
    id               BIGINT       PRIMARY KEY            COMMENT '关联users.id',
    shop_name        VARCHAR(100)                        COMMENT '药店名称',
    license_no       VARCHAR(100)                        COMMENT '营业执照号',
    address          VARCHAR(255)                        COMMENT '店铺地址',
    latitude         DECIMAL(10,6)                       COMMENT '纬度',
    longitude        DECIMAL(10,6)                       COMMENT '经度',
    phone            VARCHAR(20)                         COMMENT '联系电话',
    status           ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' COMMENT '审核状态',
    delivery_radius  INT          DEFAULT 3              COMMENT '配送半径(公里)',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_merchants_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';

-- -------------------------------------------
-- 3. medicines - 药品表
-- -------------------------------------------
CREATE TABLE medicines (
    id                   BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    merchant_id          BIGINT        NOT NULL                  COMMENT '所属商家',
    name                 VARCHAR(100)  NOT NULL                  COMMENT '药品名称',
    generic_name         VARCHAR(100)                            COMMENT '通用名',
    category             VARCHAR(50)                             COMMENT '分类(感冒/心血管/消化等)',
    price                DECIMAL(10,2) NOT NULL                  COMMENT '售价',
    stock                INT           DEFAULT 0                 COMMENT '库存',
    description          TEXT                                    COMMENT '说明',
    requires_prescription TINYINT(1)   DEFAULT 0                 COMMENT '是否处方药(0否/1是)',
    status               TINYINT       DEFAULT 1                 COMMENT '上架状态(1上架/0下架)',
    CONSTRAINT fk_medicines_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';

-- -------------------------------------------
-- 4. orders - 订单表
-- -------------------------------------------
CREATE TABLE orders (
    id               BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no         VARCHAR(32)   UNIQUE                     COMMENT '订单编号',
    user_id          BIGINT        NOT NULL                   COMMENT '下单用户',
    merchant_id      BIGINT                                  COMMENT '接单商家',
    payer_id         BIGINT                                  COMMENT '支付人(亲友代付时非空)',
    total_amount     DECIMAL(10,2) DEFAULT 0.00              COMMENT '总金额',
    status           ENUM('PENDING','WAITING_PAYMENT','PAID','ACCEPTED','DELIVERING','DELIVERED','COMPLETED')
                     DEFAULT 'PENDING'                       COMMENT '订单状态',
    delivery_address VARCHAR(255)                            COMMENT '配送地址',
    remark           TEXT                                    COMMENT '备注',
    paid_at          DATETIME                                COMMENT '支付时间',
    accepted_at      DATETIME                                COMMENT '商家接单时间',
    delivered_at     DATETIME                                COMMENT '送达时间',
    created_at       DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '订单创建时间',
    updated_at       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '订单更新时间',
    CONSTRAINT fk_orders_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_orders_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_orders_payer    FOREIGN KEY (payer_id)    REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- -------------------------------------------
-- 5. order_items - 订单明细表
-- -------------------------------------------
CREATE TABLE order_items (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT        NOT NULL                   COMMENT '关联订单',
    medicine_id   BIGINT        NOT NULL                   COMMENT '关联药品',
    medicine_name VARCHAR(100)  NOT NULL                   COMMENT '冗余药品名(防止改名丢失)',
    quantity      INT           NOT NULL                   COMMENT '数量',
    unit_price    DECIMAL(10,2) NOT NULL                   COMMENT '单价快照',
    subtotal      DECIMAL(10,2) NOT NULL                   COMMENT '小计',
    CONSTRAINT fk_items_order   FOREIGN KEY (order_id)    REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- -------------------------------------------
-- 6. family_relations - 亲友关系表
-- -------------------------------------------
CREATE TABLE family_relations (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    requester_id  BIGINT       NOT NULL                    COMMENT '发起绑定请求的用户ID',
    target_id     BIGINT       NOT NULL                    COMMENT '被邀请的用户ID',
    relationship  VARCHAR(20)                              COMMENT '关系描述(子女/配偶/其他)',
    status        ENUM('PENDING','ACCEPTED','REJECTED')
                  DEFAULT 'PENDING'                        COMMENT '绑定状态',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '发起时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
    CONSTRAINT fk_family_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_family_target    FOREIGN KEY (target_id)    REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='亲友关系表';

-- -------------------------------------------
-- 7. notifications - 通知表
-- -------------------------------------------
CREATE TABLE notifications (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    user_id         BIGINT       NOT NULL                    COMMENT '接收人',
    title           VARCHAR(100) NOT NULL                    COMMENT '标题',
    content         TEXT                                     COMMENT '内容',
    type            ENUM('NEW_ORDER','ORDER_STATUS','PAYMENT','FAMILY')
                   NOT NULL                                  COMMENT '通知类型',
    related_order_id BIGINT                                  COMMENT '关联订单',
    is_read         TINYINT(1)   DEFAULT 0                   COMMENT '已读标记(0未读/1已读)',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- -------------------------------------------
-- 8. order_bid_records - 订单抢单记录表
-- -------------------------------------------
CREATE TABLE order_bid_records (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    order_id    BIGINT       NOT NULL                    COMMENT '关联订单',
    merchant_id BIGINT       NOT NULL                    COMMENT '尝试接单的商家',
    action      ENUM('ACCEPTED','ABANDONED') NOT NULL    COMMENT '操作类型(接单/放弃)',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '操作时间',
    CONSTRAINT fk_bid_order    FOREIGN KEY (order_id)    REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单抢单记录表';
