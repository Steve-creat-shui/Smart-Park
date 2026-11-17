-- ============================================
-- 多租户改造 - 企业/成员/审批表
-- ============================================
USE energy_management;

-- ============================================
-- 企业表
-- ============================================
CREATE TABLE IF NOT EXISTS enterprises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_name VARCHAR(200) NOT NULL COMMENT '企业名称',
    enterprise_code VARCHAR(50) UNIQUE COMMENT '企业编码（统一社会信用代码）',
    industry VARCHAR(100) COMMENT '所属行业',
    contact_name VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    address VARCHAR(500) COMMENT '企业地址',
    logo_url VARCHAR(500) COMMENT 'Logo',
    description TEXT COMMENT '企业简介',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/suspended/deleted',
    created_by BIGINT COMMENT '创建人(用户ID)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 企业成员关联表
-- ============================================
CREATE TABLE IF NOT EXISTS enterprise_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT 'owner/admin/member',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_by BIGINT COMMENT '邀请人ID',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/removed',
    UNIQUE KEY uk_enterprise_user (enterprise_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_enterprise_id (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 企业注册申请表（审批流转）
-- ============================================
CREATE TABLE IF NOT EXISTS enterprise_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL COMMENT '申请人(用户ID)',
    enterprise_name VARCHAR(200) NOT NULL COMMENT '企业名称',
    enterprise_code VARCHAR(50) COMMENT '统一社会信用代码',
    industry VARCHAR(100) COMMENT '行业',
    contact_name VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    address VARCHAR(500) COMMENT '地址',
    description TEXT COMMENT '说明/备注',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    reviewed_by BIGINT COMMENT '审核人(管理员ID)',
    reviewed_at DATETIME COMMENT '审核时间',
    reject_reason VARCHAR(500) COMMENT '拒绝原因',
    enterprise_id BIGINT COMMENT '审核通过后创建的企业ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 告警规则表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT COMMENT '所属企业(NULL=平台级)',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: threshold/anomaly/schedule',
    metric VARCHAR(50) NOT NULL COMMENT '监测指标: power/temperature/humidity等',
    operator VARCHAR(10) NOT NULL COMMENT '>/</>=/<=/<>/==',
    threshold_value DOUBLE NOT NULL COMMENT '阈值',
    level VARCHAR(20) NOT NULL DEFAULT 'warning' COMMENT 'info/warning/error/critical',
    building_id VARCHAR(50) COMMENT '限定建筑',
    device_id INT COMMENT '限定设备',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 演示数据集配置表
-- ============================================
CREATE TABLE IF NOT EXISTS demo_data_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值(JSON)',
    description VARCHAR(200) COMMENT '说明',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 站内消息/通知表
-- ============================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收人ID',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(30) NOT NULL DEFAULT 'info' COMMENT 'info/warning/success/error',
    related_type VARCHAR(50) COMMENT '关联类型: application/alert/work_order',
    related_id BIGINT COMMENT '关联ID',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Excel导入历史表
-- ============================================
CREATE TABLE IF NOT EXISTS import_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT COMMENT '文件大小(bytes)',
    record_count INT DEFAULT 0 COMMENT '导入记录数',
    status VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT 'processing/completed/failed',
    error_message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    INDEX idx_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 修改现有表: buildings 增加 enterprise_id
-- ============================================
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS enterprise_id BIGINT DEFAULT NULL COMMENT '所属企业(NULL=演示数据)' AFTER building_id;
ALTER TABLE buildings ADD INDEX IF NOT EXISTS idx_enterprise (enterprise_id);

-- ============================================
-- 修改现有表: devices 增加 enterprise_id
-- ============================================
ALTER TABLE devices ADD COLUMN IF NOT EXISTS enterprise_id BIGINT DEFAULT NULL COMMENT '所属企业' AFTER building_id;
ALTER TABLE devices ADD INDEX IF NOT EXISTS idx_enterprise (enterprise_id);

-- ============================================
-- 修改现有表: alerts 增加 enterprise_id
-- ============================================
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS enterprise_id BIGINT DEFAULT NULL COMMENT '所属企业' AFTER device_id;
ALTER TABLE alerts ADD INDEX IF NOT EXISTS idx_enterprise (enterprise_id);

-- ============================================
-- 修改现有表: work_orders 增加 enterprise_id
-- ============================================
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS enterprise_id BIGINT DEFAULT NULL COMMENT '所属企业' AFTER building_id;
ALTER TABLE work_orders ADD INDEX IF NOT EXISTS idx_enterprise (enterprise_id);

-- ============================================
-- 修改现有表: users 增加 user_type
-- ============================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'normal' COMMENT 'normal/enterprise/platform_admin' AFTER role;
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_enterprise_id BIGINT DEFAULT NULL COMMENT '当前选中的企业ID' AFTER user_type;

-- 同步现有用户的 user_type
UPDATE users SET user_type = 'platform_admin' WHERE role = 'admin';
UPDATE users SET user_type = 'enterprise' WHERE role = 'enterprise';
UPDATE users SET user_type = 'normal' WHERE role = 'user';

-- ============================================
-- 插入演示数据配置
-- ============================================
INSERT IGNORE INTO demo_data_config (config_key, config_value, description) VALUES
('demo_buildings', '["B001","B002","B003","B004","B005","B006","B007"]', '演示数据包含的建筑ID'),
('demo_enabled', 'true', '是否启用演示模式'),
('demo_label', '演示数据 - 注册企业可接入真实数据', '演示标识文案');

-- ============================================
-- 插入演示企业（可选）
-- ============================================
INSERT IGNORE INTO enterprises (id, enterprise_name, enterprise_code, industry, contact_name, status, created_at) VALUES
(1, '演示企业（平台数据）', 'DEMO000000', '教育', '系统', 'active', NOW());
