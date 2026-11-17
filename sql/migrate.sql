USE energy_management;

ALTER TABLE buildings ADD COLUMN enterprise_id BIGINT DEFAULT NULL;
ALTER TABLE devices ADD COLUMN enterprise_id BIGINT DEFAULT NULL;
ALTER TABLE alerts ADD COLUMN enterprise_id BIGINT DEFAULT NULL;
ALTER TABLE work_orders ADD COLUMN enterprise_id BIGINT DEFAULT NULL;

UPDATE users SET user_type = 'platform_admin' WHERE role = 'admin' AND (user_type IS NULL OR user_type = 'normal');
UPDATE users SET user_type = 'enterprise' WHERE role = 'enterprise' AND (user_type IS NULL OR user_type = 'normal');

INSERT IGNORE INTO enterprises (id, enterprise_name, enterprise_code, industry, contact_name, status, created_at)
VALUES (1, '演示企业（平台数据）', 'DEMO000000', '教育', '系统', 'active', NOW());
