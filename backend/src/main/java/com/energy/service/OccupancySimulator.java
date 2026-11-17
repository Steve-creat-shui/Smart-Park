package com.energy.service;

import com.energy.entity.AnomalyAlert;
import com.energy.entity.BuildingZone;
import com.energy.entity.ZoneOccupancy;
import com.energy.repository.AnomalyAlertRepository;
import com.energy.repository.BuildingRepository;
import com.energy.repository.BuildingZoneRepository;
import com.energy.repository.ZoneOccupancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OccupancySimulator implements CommandLineRunner {

    private final BuildingZoneRepository zoneRepo;
    private final ZoneOccupancyRepository occupancyRepo;
    private final AnomalyAlertRepository alertRepo;
    private final BuildingRepository buildingRepo;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        if (zoneRepo.count() == 0) {
            initZones();
            System.out.println("[多模态] 已初始化建筑区域数据");
        }
        // 清除旧数据，重新生成
        occupancyRepo.deleteAll();
        generateHistoricalData();
        System.out.println("[多模态] 已生成历史占用数据");
        // 清除旧告警，重新生成示例数据
        alertRepo.deleteAll();
        generateSampleAlerts();
        System.out.println("[多模态] 已生成示例告警数据");
    }

    private void initZones() {
        String[][] zoneConfigs = {
            {"B001", "行政楼1F-大厅", "lobby", "1", "20"},
            {"B001", "行政楼2F-办公A区", "office", "2", "40"},
            {"B001", "行政楼3F-办公B区", "office", "3", "35"},
            {"B001", "行政楼4F-会议区", "meeting", "4", "25"},
            {"B002", "图书馆1F-阅览室", "office", "1", "80"},
            {"B002", "图书馆2F-自习室", "office", "2", "60"},
            {"B002", "图书馆3F-电子阅览", "office", "3", "40"},
            {"B003", "实验楼1F-实验室A", "lab", "1", "30"},
            {"B003", "实验楼2F-实验室B", "lab", "2", "30"},
            {"B003", "实验楼3F-研究室", "office", "3", "20"},
            {"B004", "教学楼1F-教室区", "classroom", "1", "120"},
            {"B004", "教学楼2F-教室区", "classroom", "2", "120"},
            {"B004", "教学楼3F-多媒体", "classroom", "3", "80"},
            {"B005", "宿舍楼1-6F", "dormitory", "1", "200"},
            {"B005", "宿舍楼7-12F", "dormitory", "7", "200"},
            {"B006", "餐厅1F-餐饮区", "canteen", "1", "150"},
            {"B006", "餐厅2F-餐饮区", "canteen", "2", "100"},
            {"B007", "体育馆1F-场馆", "gym", "1", "100"},
            {"B007", "体育馆2F-看台", "gym", "2", "300"},
        };
        for (String[] cfg : zoneConfigs) {
            BuildingZone zone = new BuildingZone();
            zone.setBuildingId(cfg[0]);
            zone.setZoneName(cfg[1]);
            zone.setZoneType(cfg[2]);
            zone.setFloorNum(Integer.parseInt(cfg[3]));
            zone.setCapacity(Integer.parseInt(cfg[4]));
            zone.setHasCamera(true);
            zone.setHasSensor(true);
            zoneRepo.save(zone);
        }
    }

    private void generateHistoricalData() {
        List<BuildingZone> zones = zoneRepo.findAll();
        LocalDateTime now = LocalDateTime.now();
        // 过去24小时，每10分钟一条
        for (int i = 144; i >= 0; i--) {
            LocalDateTime time = now.minusMinutes(i * 10L);
            for (BuildingZone zone : zones) {
                int count = simulateCount(zone, time);
                ZoneOccupancy occ = new ZoneOccupancy();
                occ.setZoneId(zone.getId());
                occ.setBuildingId(zone.getBuildingId());
                occ.setPersonCount(count);
                occ.setIsOccupied(count > 0);
                occ.setDetectionMethod("simulated");
                occ.setRecordedAt(time);
                occupancyRepo.save(occ);
            }
        }
    }

    @Scheduled(fixedRate = 60000) // 每分钟模拟一次
    public void simulate() {
        List<BuildingZone> zones = zoneRepo.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (BuildingZone zone : zones) {
            int count = simulateCount(zone, now);
            ZoneOccupancy occ = new ZoneOccupancy();
            occ.setZoneId(zone.getId());
            occ.setBuildingId(zone.getBuildingId());
            occ.setPersonCount(count);
            occ.setIsOccupied(count > 0);
            occ.setDetectionMethod("simulated");
            occ.setRecordedAt(now);
            occupancyRepo.save(occ);
        }
    }

    private int simulateCount(BuildingZone zone, LocalDateTime time) {
        int hour = time.getHour();
        DayOfWeek dow = time.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        boolean isMorning = hour >= 8 && hour < 12;
        boolean isAfternoon = hour >= 13 && hour < 18;
        boolean isEvening = hour >= 18 && hour < 22;
        boolean isNight = hour >= 22 || hour < 6;
        String type = zone.getZoneType();
        int cap = zone.getCapacity();

        double ratio;
        if (isNight) {
            ratio = type.equals("dormitory") ? 0.85 : (random.nextDouble() < 0.08 ? 0.02 : 0);
        } else if (isWeekend) {
            if ("dormitory".equals(type)) ratio = 0.3 + random.nextDouble() * 0.3;
            else if ("canteen".equals(type)) ratio = isMorning || isAfternoon ? 0.2 : 0.05;
            else if ("gym".equals(type)) ratio = isAfternoon || isEvening ? 0.3 : 0.05;
            else if ("office".equals(type)) ratio = random.nextDouble() < 0.15 ? 0.05 : 0;
            else ratio = 0.05;
        } else {
            if ("office".equals(type) || "lab".equals(type)) {
                ratio = (isMorning || isAfternoon) ? 0.4 + random.nextDouble() * 0.4 : (isEvening ? 0.1 : 0);
            } else if ("classroom".equals(type)) {
                ratio = (isMorning || isAfternoon) ? 0.5 + random.nextDouble() * 0.4 : 0.05;
            } else if ("canteen".equals(type)) {
                boolean mealTime = (hour >= 7 && hour < 9) || (hour >= 11 && hour < 13) || (hour >= 17 && hour < 19);
                ratio = mealTime ? 0.6 + random.nextDouble() * 0.3 : 0.02;
            } else if ("lobby".equals(type)) {
                ratio = (isMorning || isAfternoon) ? 0.1 + random.nextDouble() * 0.2 : 0.02;
            } else if ("dormitory".equals(type)) {
                ratio = isEvening ? 0.7 : 0.15;
            } else if ("gym".equals(type)) {
                ratio = isEvening ? 0.3 + random.nextDouble() * 0.3 : (isAfternoon ? 0.2 : 0.05);
            } else {
                ratio = 0.1;
            }
        }
        return (int) (cap * ratio);
    }

    private void generateSampleAlerts() {
        List<BuildingZone> allZones = zoneRepo.findAll();
        LocalDateTime now = LocalDateTime.now();
        String[][] alertTemplates = {
            {"empty_waste", "无人照明", "warning", "%s 无人，照明功率 %.1fkW 仍在运行"},
            {"empty_waste", "无人空调", "severe", "%s 已无人超30分钟，空调 %.1fkW 持续运行"},
            {"empty_waste", "无人设备运行", "warning", "非工作时间 %s 无人，总功率 %.1fkW"},
            {"behavior", "夜间异常人员", "info", "深夜 %s 检测到 %d 人"},
            {"pattern", "人少能耗高", "warning", "%s 仅 2 人但功率 %.1fkW，人均能耗异常偏高"},
        };

        for (BuildingZone zone : allZones) {
            if ("dormitory".equals(zone.getZoneType())) continue;
            // 每个非宿舍区域生成2-5条告警
            int count = 2 + random.nextInt(4);
            for (int i = 0; i < count; i++) {
                String[] tpl = alertTemplates[random.nextInt(alertTemplates.length)];
                AnomalyAlert alert = new AnomalyAlert();
                alert.setBuildingId(zone.getBuildingId());
                alert.setZoneId(zone.getId());
                alert.setZoneName(zone.getZoneName());
                alert.setAnomalyCategory(tpl[0]);
                alert.setAnomalyType(tpl[1]);
                alert.setSeverity(tpl[2]);

                double power = 1.5 + random.nextDouble() * 4;
                if ("夜间异常人员".equals(tpl[1])) {
                    alert.setEvidenceSummary(String.format(tpl[3], zone.getZoneName(), 1 + random.nextInt(3)));
                    alert.setEstimatedWasteKwh(0.0);
                    alert.setEstimatedCost(0.0);
                    alert.setPersonCount(1 + random.nextInt(3));
                } else {
                    alert.setEvidenceSummary(String.format(tpl[3], zone.getZoneName(), power));
                    double waste = power * (0.5 + random.nextDouble() * 2);
                    alert.setEstimatedWasteKwh(Math.round(waste * 100.0) / 100.0);
                    alert.setEstimatedCost(Math.round(waste * 0.8 * 100.0) / 100.0);
                    alert.setPersonCount(0);
                }

                alert.setEnergyValue(power);
                alert.setIsWorkHour(random.nextBoolean());
                // 随机分布在过去24小时
                int minutesAgo = random.nextInt(1440);
                alert.setCreatedAt(now.minusMinutes(minutesAgo));
                // 30%已解决
                if (random.nextDouble() < 0.3) {
                    alert.setStatus("resolved");
                    alert.setResolvedAt(alert.getCreatedAt().plusMinutes(15 + random.nextInt(60)));
                } else {
                    alert.setStatus("active");
                }
                alertRepo.save(alert);
            }
        }
    }
}
