-- ============================================
-- 建筑能源数字孪生管理系统 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS energy_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE energy_management;

-- ============================================
-- 建筑信息表
-- ============================================
CREATE TABLE IF NOT EXISTS buildings (
    building_id VARCHAR(50) PRIMARY KEY,
    building_name VARCHAR(100),
    building_type VARCHAR(50),
    floor_count INT,
    total_area_m2 DOUBLE,
    location VARCHAR(200),
    construction_year INT,
    energy_rating VARCHAR(10),
    ac_system_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 能耗数据表
-- ============================================
CREATE TABLE IF NOT EXISTS energy_consumption (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id VARCHAR(50) NOT NULL,
    building_type VARCHAR(50),
    timestamp DATETIME NOT NULL,
    power_consumption_kwh DOUBLE,
    water_consumption_m3 DOUBLE,
    carbon_emission_kg DOUBLE,
    ac_consumption_kwh DOUBLE,
    lighting_consumption_kwh DOUBLE,
    other_consumption_kwh DOUBLE,
    temperature_celsius DOUBLE,
    humidity_percent DOUBLE,
    occupancy_rate DOUBLE,
    status VARCHAR(20) DEFAULT 'normal',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_building_id (building_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_building_time (building_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 插入建筑基础数据
-- ============================================
INSERT IGNORE INTO buildings VALUES
('B001', '科技园A栋', '办公楼', 12, 15000.0, '深圳市南山区科技园', 2015, 'A', '中央空调', 'active', NOW()),
('B002', '科技园B栋', '办公楼', 10, 12000.0, '深圳市南山区科技园', 2016, 'B', '中央空调', 'active', NOW()),
('B003', '研发中心', '研发楼', 8,  9500.0,  '深圳市南山区科技园', 2018, 'A', '变频中央空调', 'active', NOW()),
('B004', '综合服务楼', '综合楼', 6, 8000.0,  '深圳市南山区科技园', 2017, 'C', '分体空调', 'active', NOW()),
('B005', '数据中心', '数据中心', 4, 5000.0,  '深圳市南山区科技园', 2020, 'A+', '精密空调', 'active', NOW()),
('B006', '员工食堂', '餐饮楼', 3, 3500.0,   '深圳市南山区科技园', 2016, 'B', '分体空调', 'active', NOW());

-- ============================================
-- 插入近12个月能耗测试数据（B001 ~ B006）
-- ============================================
-- 使用存储过程批量生成测试数据
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_energy_data$$
CREATE PROCEDURE generate_energy_data()
BEGIN
    DECLARE v_building VARCHAR(10);
    DECLARE v_btype VARCHAR(20);
    DECLARE v_day_offset INT DEFAULT 0;
    DECLARE v_hour INT;
    DECLARE v_base_power DOUBLE;
    DECLARE v_power DOUBLE;
    DECLARE v_water DOUBLE;
    DECLARE v_carbon DOUBLE;
    DECLARE v_ac DOUBLE;
    DECLARE v_lighting DOUBLE;
    DECLARE v_other DOUBLE;
    DECLARE v_temp DOUBLE;
    DECLARE v_humidity DOUBLE;
    DECLARE v_status VARCHAR(20);
    DECLARE v_ts DATETIME;

    -- 为每栋楼生成180天 * 每天4个时间点的数据
    SET @buildings = 'B001,B002,B003,B004,B005,B006';
    SET @btypes = '办公楼,办公楼,研发楼,综合楼,数据中心,餐饮楼';
    SET @base_powers = '280,220,180,150,350,120';

    SET @bidx = 1;
    WHILE @bidx <= 6 DO
        SET v_building = SUBSTRING_INDEX(SUBSTRING_INDEX(@buildings, ',', @bidx), ',', -1);
        SET v_btype = SUBSTRING_INDEX(SUBSTRING_INDEX(@btypes, ',', @bidx), ',', -1);
        SET v_base_power = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(@base_powers, ',', @bidx), ',', -1) AS DECIMAL(10,2));

        SET v_day_offset = 0;
        WHILE v_day_offset < 180 DO
            SET v_hour = 8;
            WHILE v_hour <= 22 DO
                SET v_ts = DATE_SUB(NOW(), INTERVAL (180 - v_day_offset) DAY) + INTERVAL v_hour HOUR;

                -- 根据时段计算能耗
                IF v_hour BETWEEN 9 AND 11 OR v_hour BETWEEN 14 AND 17 THEN
                    SET v_power = v_base_power * (0.85 + RAND() * 0.4);
                ELSEIF v_hour BETWEEN 12 AND 13 THEN
                    SET v_power = v_base_power * (0.5 + RAND() * 0.2);
                ELSEIF v_hour >= 20 THEN
                    SET v_power = v_base_power * (0.2 + RAND() * 0.15);
                ELSE
                    SET v_power = v_base_power * (0.6 + RAND() * 0.25);
                END IF;

                -- 数据中心全天高负载
                IF v_building = 'B005' THEN
                    SET v_power = v_base_power * (0.9 + RAND() * 0.2);
                END IF;

                -- 偶发性异常（约5%概率）
                IF RAND() < 0.05 THEN
                    SET v_power = v_power * (1.6 + RAND() * 0.5);
                    SET v_status = 'warning';
                ELSE
                    SET v_status = 'normal';
                END IF;

                SET v_ac = v_power * (0.40 + RAND() * 0.15);
                SET v_lighting = v_power * (0.15 + RAND() * 0.08);
                SET v_other = v_power - v_ac - v_lighting;
                SET v_water = v_power * (0.008 + RAND() * 0.004);
                SET v_carbon = v_power * 0.785;
                SET v_temp = 20 + RAND() * 12;
                SET v_humidity = 45 + RAND() * 35;

                INSERT INTO energy_consumption
                    (building_id, building_type, timestamp, power_consumption_kwh,
                     water_consumption_m3, carbon_emission_kg, ac_consumption_kwh,
                     lighting_consumption_kwh, other_consumption_kwh,
                     temperature_celsius, humidity_percent, occupancy_rate, status)
                VALUES (
                    v_building, v_btype, v_ts,
                    ROUND(v_power, 2), ROUND(v_water, 2),
                    ROUND(v_carbon, 2), ROUND(v_ac, 2),
                    ROUND(v_lighting, 2), ROUND(GREATEST(v_other, 0), 2),
                    ROUND(v_temp, 1), ROUND(v_humidity, 1),
                    ROUND(0.5 + RAND() * 0.5, 2), v_status
                );

                SET v_hour = v_hour + 4;
            END WHILE;
            SET v_day_offset = v_day_offset + 1;
        END WHILE;
        SET @bidx = @bidx + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_energy_data();
DROP PROCEDURE IF EXISTS generate_energy_data;

SELECT '数据初始化完成' AS message;
SELECT building_id, COUNT(*) as record_count, ROUND(SUM(power_consumption_kwh),2) as total_kwh
FROM energy_consumption
GROUP BY building_id;
