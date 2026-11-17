-- 双角色用户系统 - 用户表扩展
ALTER TABLE users MODIFY COLUMN nickname VARCHAR(100);

-- 用存储过程安全添加列
DELIMITER //
DROP PROCEDURE IF EXISTS safe_alter//
CREATE PROCEDURE safe_alter()
BEGIN
  DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;
  ALTER TABLE users ADD COLUMN employee_id VARCHAR(50);
  ALTER TABLE users ADD COLUMN real_name VARCHAR(50);
  ALTER TABLE users ADD COLUMN department VARCHAR(50);
  ALTER TABLE users ADD COLUMN `position` VARCHAR(50);
  ALTER TABLE users ADD COLUMN engineer_cert VARCHAR(100);
  ALTER TABLE users ADD COLUMN company_email VARCHAR(100);
  ALTER TABLE users ADD COLUMN id_card_last4 VARCHAR(4);
  ALTER TABLE users ADD COLUMN remark VARCHAR(500);
  ALTER TABLE users ADD COLUMN approved_by BIGINT;
  ALTER TABLE users ADD COLUMN approved_at DATETIME;
  ALTER TABLE users ADD COLUMN reject_reason VARCHAR(500);
END//
DELIMITER ;
CALL safe_alter();
DROP PROCEDURE IF EXISTS safe_alter;

-- 默认管理员账号（密码: admin123，BCrypt加密）
INSERT INTO users (phone, password, nickname, role, status, employee_id, real_name, department, created_at)
VALUES ('13800000000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2VHtDmJ9.s1AqXVLykQ9m6', '系统管理员', 'admin', 'active', 'ADMIN001', '管理员', '管理部', NOW())
ON DUPLICATE KEY UPDATE role = 'admin', status = 'active';
