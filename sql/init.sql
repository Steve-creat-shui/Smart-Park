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
REPLACE INTO buildings VALUES
('B001', '行政楼',     '行政楼', 8,  10000.0, '校区中心',   2010, 'A',  '中央空调',     'active', NOW()),
('B002', '图书馆',     '图书馆', 5,  8000.0,  '校区东区',   2012, 'A',  '变频中央空调', 'active', NOW()),
('B003', '实验楼',     '实验楼', 6,  9500.0,  '校区西区',   2015, 'B',  '变频中央空调', 'active', NOW()),
('B004', '教学楼',     '教学楼', 10, 15000.0, '校区中心',   2008, 'B',  '中央空调',     'active', NOW()),
('B005', '宿舍楼',     '宿舍楼', 12, 20000.0, '校区北区',   2009, 'C',  '分体空调',     'active', NOW()),
('B006', '餐厅',       '餐厅',   3,  4000.0,  '校区南区',   2011, 'B',  '分体空调',     'active', NOW()),
('B007', '室内体育馆', '体育馆', 2,  12000.0, '校区东南区', 2018, 'A',  '中央空调',     'active', NOW());

-- ============================================
-- 插入近12个月能耗测试数据（B001 ~ B006）
-- ============================================
-- 使用存储过程批量生成测试数据
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_energy_data$$
CREATE PROCEDURE generate_energy_data()
BEGIN
    DECLARE v_bidx       INT DEFAULT 1;
    DECLARE v_building   VARCHAR(10);
    DECLARE v_base_power DOUBLE;
    DECLARE v_day        INT DEFAULT 0;
    DECLARE v_hour       INT;
    DECLARE v_month      INT;
    DECLARE v_season_factor DOUBLE;
    DECLARE v_ac_ratio   DOUBLE;
    DECLARE v_power      DOUBLE;
    DECLARE v_water      DOUBLE;
    DECLARE v_carbon     DOUBLE;
    DECLARE v_ac         DOUBLE;
    DECLARE v_lighting   DOUBLE;
    DECLARE v_other      DOUBLE;
    DECLARE v_temp       DOUBLE;
    DECLARE v_humidity   DOUBLE;
    DECLARE v_status     VARCHAR(20);
    DECLARE v_ts         DATETIME;

    TRUNCATE TABLE energy_consumption;

    WHILE v_bidx <= 7 DO
        CASE v_bidx
            WHEN 1 THEN SET v_building = 'B001'; SET v_base_power = 200;
            WHEN 2 THEN SET v_building = 'B002'; SET v_base_power = 180;
            WHEN 3 THEN SET v_building = 'B003'; SET v_base_power = 250;
            WHEN 4 THEN SET v_building = 'B004'; SET v_base_power = 300;
            WHEN 5 THEN SET v_building = 'B005'; SET v_base_power = 150;
            WHEN 6 THEN SET v_building = 'B006'; SET v_base_power = 120;
            WHEN 7 THEN SET v_building = 'B007'; SET v_base_power = 160;
        END CASE;

        SET v_day = 0;
        WHILE v_day < 365 DO
            SET v_ts    = DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 364 DAY), INTERVAL v_day DAY);
            SET v_month = MONTH(v_ts);

            CASE
                WHEN v_month IN (6,7,8) THEN
                    SET v_season_factor = 1.4; SET v_ac_ratio = 0.55;
                    SET v_temp = 28 + RAND()*8;  SET v_humidity = 65 + RAND()*20;
                WHEN v_month IN (12,1,2) THEN
                    SET v_season_factor = 1.2; SET v_ac_ratio = 0.15;
                    SET v_temp = 2  + RAND()*10; SET v_humidity = 40 + RAND()*20;
                WHEN v_month IN (3,4,5) THEN
                    SET v_season_factor = 1.0; SET v_ac_ratio = 0.25;
                    SET v_temp = 12 + RAND()*10; SET v_humidity = 55 + RAND()*20;
                ELSE
                    SET v_season_factor = 1.1; SET v_ac_ratio = 0.30;
                    SET v_temp = 16 + RAND()*10; SET v_humidity = 50 + RAND()*20;
            END CASE;

            SET v_hour = 0;
            WHILE v_hour < 24 DO
                SET v_ts = DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 364 DAY), INTERVAL v_day DAY), INTERVAL v_hour HOUR);

                IF v_hour BETWEEN 8 AND 11 OR v_hour BETWEEN 14 AND 17 THEN
                    SET v_power = v_base_power * v_season_factor * (0.85 + RAND()*0.30);
                ELSEIF v_hour BETWEEN 12 AND 13 THEN
                    SET v_power = v_base_power * v_season_factor * (0.50 + RAND()*0.15);
                ELSEIF v_hour BETWEEN 18 AND 21 THEN
                    SET v_power = v_base_power * v_season_factor * (0.40 + RAND()*0.20);
                ELSEIF v_hour >= 22 OR v_hour <= 5 THEN
                    SET v_power = v_base_power * v_season_factor * (0.10 + RAND()*0.10);
                ELSE
                    SET v_power = v_base_power * v_season_factor * (0.60 + RAND()*0.20);
                END IF;

                IF v_building = 'B005' AND (v_hour >= 22 OR v_hour <= 5) THEN
                    SET v_power = v_base_power * v_season_factor * (0.30 + RAND()*0.15);
                END IF;

                IF v_building = 'B006' AND v_hour IN (7,8,11,12,17,18) THEN
                    SET v_power = v_base_power * v_season_factor * (1.20 + RAND()*0.30);
                END IF;

                IF RAND() < 0.05 THEN
                    SET v_power  = v_power * (1.6 + RAND()*0.5);
                    SET v_status = 'warning';
                ELSE
                    SET v_status = 'normal';
                END IF;

                SET v_ac       = v_power * v_ac_ratio;
                SET v_lighting = v_power * (0.12 + RAND()*0.06);
                SET v_other    = GREATEST(v_power - v_ac - v_lighting, 0);
                SET v_water    = v_power * (0.006 + RAND()*0.004);
                SET v_carbon   = v_power * 0.785;

                INSERT INTO energy_consumption
                    (building_id, building_type, timestamp, power_consumption_kwh,
                     water_consumption_m3, carbon_emission_kg, ac_consumption_kwh,
                     lighting_consumption_kwh, other_consumption_kwh,
                     temperature_celsius, humidity_percent, occupancy_rate, status)
                VALUES (
                    v_building,
                    (SELECT building_type FROM buildings WHERE building_id = v_building),
                    v_ts,
                    ROUND(v_power, 2),   ROUND(v_water, 2),
                    ROUND(v_carbon, 2),  ROUND(v_ac, 2),
                    ROUND(v_lighting,2), ROUND(v_other, 2),
                    ROUND(v_temp, 1),    ROUND(v_humidity, 1),
                    ROUND(0.4 + RAND()*0.6, 2), v_status
                );

                SET v_hour = v_hour + 2;
            END WHILE;
            SET v_day = v_day + 1;
        END WHILE;
        SET v_bidx = v_bidx + 1;
    END WHILE;
END$$

DELIMITER ;

CALL generate_energy_data();
DROP PROCEDURE IF EXISTS generate_energy_data;

SELECT '数据初始化完成' AS message;
SELECT building_id, COUNT(*) as record_count, ROUND(SUM(power_consumption_kwh),2) as total_kwh
FROM energy_consumption
GROUP BY building_id;
