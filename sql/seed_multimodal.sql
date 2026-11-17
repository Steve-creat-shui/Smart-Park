-- ============================================
-- 多模态监控模块 - 区域与占用数据种子
-- 为7栋演示建筑生成 building_zones + zone_occupancy
-- ============================================

USE energy_management;

-- ============================================
-- 清理旧的演示区域数据（仅演示建筑）
-- ============================================
DELETE FROM zone_occupancy WHERE building_id IN ('B001','B002','B003','B004','B005','B006','B007');
DELETE FROM building_zones WHERE building_id IN ('B001','B002','B003','B004','B005','B006','B007');

-- ============================================
-- 插入建筑区域数据
-- ============================================
INSERT INTO building_zones (building_id, zone_name, zone_type, floor_num, capacity, has_camera, has_sensor) VALUES
-- B001 行政楼（8层）
('B001', 'B001-1F大厅', 'lobby', 1, 50, true, true),
('B001', 'B001-2F办公区A', 'office', 2, 40, true, true),
('B001', 'B001-3F办公区B', 'office', 3, 40, false, true),
('B001', 'B001-4F会议中心', 'meeting', 4, 60, true, true),
('B001', 'B001-5F领导办公', 'office', 5, 20, true, true),
('B001', 'B001-6F档案室', 'storage', 6, 10, false, true),
('B001', 'B001-7F数据中心', 'server_room', 7, 5, true, true),
('B001', 'B001-8F天台设备', 'mechanical', 8, 3, true, true),
-- B002 图书馆（5层）
('B002', 'B002-1F借阅大厅', 'lobby', 1, 80, true, true),
('B002', 'B002-2F阅览室A', 'reading', 2, 100, true, true),
('B002', 'B002-3F阅览室B', 'reading', 3, 100, false, true),
('B002', 'B002-4F电子阅览', 'computer', 4, 60, true, true),
('B002', 'B002-5F自习室', 'study', 5, 80, false, true),
-- B003 实验楼（6层）
('B003', 'B003-1F化学实验室', 'lab', 1, 30, true, true),
('B003', 'B003-2F物理实验室', 'lab', 2, 30, true, true),
('B003', 'B003-3F生物实验室', 'lab', 3, 25, true, true),
('B003', 'B003-4F精密仪器室', 'lab', 4, 15, true, true),
('B003', 'B003-5F计算实验室', 'computer', 5, 40, false, true),
('B003', 'B003-6F科研办公', 'office', 6, 20, false, true),
-- B004 教学楼（10层）
('B004', 'B004-1F阶梯教室', 'classroom', 1, 200, true, true),
('B004', 'B004-2F普通教室A', 'classroom', 2, 60, false, true),
('B004', 'B004-3F普通教室B', 'classroom', 3, 60, false, true),
('B004', 'B004-4F多媒体教室', 'classroom', 4, 80, true, true),
('B004', 'B004-5F语音教室', 'classroom', 5, 50, false, true),
('B004', 'B004-6F教师休息室', 'office', 6, 20, false, true),
-- B005 宿舍楼（12层）
('B005', 'B005-1F门厅', 'lobby', 1, 30, true, true),
('B005', 'B005-2F-6F宿舍区', 'dormitory', 2, 200, false, true),
('B005', 'B005-7F-12F宿舍区', 'dormitory', 7, 200, false, true),
('B005', 'B005-公共洗衣房', 'utility', 1, 15, false, true),
-- B006 餐厅（3层）
('B006', 'B006-1F食堂A', 'canteen', 1, 300, true, true),
('B006', 'B006-2F食堂B', 'canteen', 2, 200, true, true),
('B006', 'B006-3F教工餐厅', 'canteen', 3, 80, false, true),
('B006', 'B006-后厨', 'kitchen', 1, 20, true, true),
-- B007 室内体育馆（2层）
('B007', 'B007-1F篮球场', 'sports', 1, 100, true, true),
('B007', 'B007-1F游泳馆', 'pool', 1, 60, true, true),
('B007', 'B007-2F健身房', 'gym', 2, 40, true, true),
('B007', 'B007-2F乒乓球室', 'sports', 2, 30, false, true);

-- ============================================
-- 生成占用数据存储过程（最近24小时，每10分钟一条）
-- ============================================
DELIMITER $$

DROP PROCEDURE IF EXISTS generate_occupancy_data$$
CREATE PROCEDURE generate_occupancy_data()
BEGIN
    DECLARE v_zone_id BIGINT;
    DECLARE v_building_id VARCHAR(50);
    DECLARE v_zone_type VARCHAR(30);
    DECLARE v_capacity INT;
    DECLARE v_offset_min INT;
    DECLARE v_hour INT;
    DECLARE v_ts DATETIME;
    DECLARE v_person_count INT;
    DECLARE v_is_occupied BOOLEAN;
    DECLARE v_done INT DEFAULT 0;
    
    DECLARE zone_cursor CURSOR FOR 
        SELECT id, building_id, zone_type, capacity FROM building_zones 
        WHERE building_id IN ('B001','B002','B003','B004','B005','B006','B007');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;
    
    -- 清理旧占用数据
    DELETE FROM zone_occupancy WHERE building_id IN ('B001','B002','B003','B004','B005','B006','B007');
    
    OPEN zone_cursor;
    zone_loop: LOOP
        FETCH zone_cursor INTO v_zone_id, v_building_id, v_zone_type, v_capacity;
        IF v_done THEN LEAVE zone_loop; END IF;
        
        -- 过去24小时，每10分钟一条 = 144条/区域
        SET v_offset_min = 0;
        WHILE v_offset_min < 1440 DO
            SET v_ts = DATE_SUB(NOW(), INTERVAL (1440 - v_offset_min) MINUTE);
            SET v_hour = HOUR(v_ts);
            
            -- 根据区域类型和时间段生成合理人数
            CASE
                WHEN v_zone_type IN ('lobby', 'meeting') THEN
                    IF v_hour BETWEEN 8 AND 18 THEN
                        SET v_person_count = FLOOR(RAND() * v_capacity * 0.5);
                    ELSE
                        SET v_person_count = FLOOR(RAND() * 3);
                    END IF;
                WHEN v_zone_type = 'office' THEN
                    IF v_hour BETWEEN 9 AND 12 OR v_hour BETWEEN 14 AND 17 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.5 + RAND() * 0.4));
                    ELSEIF v_hour BETWEEN 12 AND 14 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.1 + RAND() * 0.2));
                    ELSE
                        SET v_person_count = FLOOR(RAND() * 2);
                    END IF;
                WHEN v_zone_type IN ('classroom', 'reading', 'study') THEN
                    IF v_hour BETWEEN 8 AND 12 OR v_hour BETWEEN 14 AND 18 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.4 + RAND() * 0.5));
                    ELSEIF v_hour BETWEEN 19 AND 21 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.2 + RAND() * 0.3));
                    ELSE
                        SET v_person_count = FLOOR(RAND() * 3);
                    END IF;
                WHEN v_zone_type = 'lab' THEN
                    IF v_hour BETWEEN 9 AND 17 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.3 + RAND() * 0.5));
                    ELSE
                        SET v_person_count = FLOOR(RAND() * 2);
                    END IF;
                WHEN v_zone_type = 'dormitory' THEN
                    IF v_hour >= 22 OR v_hour <= 6 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.7 + RAND() * 0.25));
                    ELSEIF v_hour BETWEEN 8 AND 18 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.05 + RAND() * 0.1));
                    ELSE
                        SET v_person_count = FLOOR(v_capacity * (0.3 + RAND() * 0.3));
                    END IF;
                WHEN v_zone_type IN ('canteen', 'kitchen') THEN
                    IF v_hour IN (7, 8) OR v_hour IN (11, 12) OR v_hour IN (17, 18) THEN
                        SET v_person_count = FLOOR(v_capacity * (0.5 + RAND() * 0.45));
                    ELSE
                        SET v_person_count = FLOOR(v_capacity * RAND() * 0.05);
                    END IF;
                WHEN v_zone_type IN ('sports', 'gym', 'pool') THEN
                    IF v_hour BETWEEN 7 AND 9 OR v_hour BETWEEN 16 AND 21 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.2 + RAND() * 0.5));
                    ELSE
                        SET v_person_count = FLOOR(RAND() * 5);
                    END IF;
                WHEN v_zone_type IN ('server_room', 'mechanical') THEN
                    SET v_person_count = IF(RAND() < 0.05, 1, 0);
                WHEN v_zone_type = 'computer' THEN
                    IF v_hour BETWEEN 8 AND 21 THEN
                        SET v_person_count = FLOOR(v_capacity * (0.3 + RAND() * 0.5));
                    ELSE
                        SET v_person_count = 0;
                    END IF;
                ELSE
                    SET v_person_count = FLOOR(RAND() * v_capacity * 0.3);
            END CASE;
            
            SET v_is_occupied = v_person_count > 0;
            
            INSERT INTO zone_occupancy (zone_id, building_id, person_count, is_occupied, detection_method, recorded_at)
            VALUES (v_zone_id, v_building_id, v_person_count, v_is_occupied, 'simulated', v_ts);
            
            SET v_offset_min = v_offset_min + 10;
        END WHILE;
    END LOOP;
    CLOSE zone_cursor;
END$$

DELIMITER ;

CALL generate_occupancy_data();
DROP PROCEDURE IF EXISTS generate_occupancy_data;

-- ============================================
-- 生成一些示例异常告警
-- ============================================
INSERT INTO anomaly_alerts (building_id, zone_id, zone_name, anomaly_category, anomaly_type, severity, energy_value, person_count, is_work_hour, evidence_summary, estimated_waste_kwh, estimated_cost, status, created_at) 
SELECT 
    bz.building_id, bz.id, bz.zone_name, 'empty_waste', '无人照明', 'warning',
    3.5, 0, true,
    CONCAT(bz.zone_name, ' 无人，照明功率 3.5kW 仍在运行'),
    0.29, 0.23, 'active', DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*12) HOUR)
FROM building_zones bz 
WHERE bz.zone_type IN ('office', 'classroom', 'meeting')
AND bz.building_id IN ('B001','B003','B004')
LIMIT 5;

INSERT INTO anomaly_alerts (building_id, zone_id, zone_name, anomaly_category, anomaly_type, severity, energy_value, person_count, is_work_hour, evidence_summary, estimated_waste_kwh, estimated_cost, status, created_at) 
SELECT 
    bz.building_id, bz.id, bz.zone_name, 'empty_waste', '无人空调', 'severe',
    8.2, 0, true,
    CONCAT(bz.zone_name, ' 已无人超30分钟，空调 8.2kW 持续运行'),
    4.1, 3.28, 'active', DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*6) HOUR)
FROM building_zones bz 
WHERE bz.zone_type IN ('office', 'lab', 'reading')
AND bz.building_id IN ('B001','B002','B003')
LIMIT 3;

INSERT INTO anomaly_alerts (building_id, zone_id, zone_name, anomaly_category, anomaly_type, severity, energy_value, person_count, is_work_hour, evidence_summary, estimated_waste_kwh, estimated_cost, status, created_at) 
SELECT 
    bz.building_id, bz.id, bz.zone_name, 'pattern', '人少能耗高', 'warning',
    12.5, 2, true,
    CONCAT(bz.zone_name, ' 仅 2 人但功率 12.5kW，人均能耗异常偏高'),
    0.63, 0.50, 'active', DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*8) HOUR)
FROM building_zones bz 
WHERE bz.zone_type IN ('classroom', 'lab')
AND bz.building_id IN ('B003','B004')
LIMIT 2;

SELECT '多模态监控种子数据初始化完成' AS message;
SELECT COUNT(*) AS zone_count FROM building_zones WHERE building_id LIKE 'B00%';
SELECT COUNT(*) AS occupancy_records FROM zone_occupancy WHERE building_id LIKE 'B00%';
SELECT COUNT(*) AS alert_count FROM anomaly_alerts WHERE building_id LIKE 'B00%';
