package com.energy.config;

import com.energy.entity.AnomalyAlert;
import com.energy.entity.Building;
import com.energy.entity.BuildingZone;
import com.energy.entity.ZoneOccupancy;
import com.energy.repository.AnomalyAlertRepository;
import com.energy.repository.BuildingRepository;
import com.energy.repository.BuildingZoneRepository;
import com.energy.repository.ZoneOccupancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Order(2)
@RequiredArgsConstructor
public class MultiModalDataInitializer implements CommandLineRunner {

    private final BuildingZoneRepository zoneRepo;
    private final ZoneOccupancyRepository occupancyRepo;
    private final AnomalyAlertRepository alertRepo;
    private final BuildingRepository buildingRepo;

    private static final String[] DEMO_BUILDINGS = {"B001", "B002", "B003", "B004", "B005", "B006", "B007"};

    @Override
    public void run(String... args) {
        // 1. 为演示建筑(B001-B007)生成预定义区域
        long existingZones = 0;
        for (String bid : DEMO_BUILDINGS) {
            existingZones += zoneRepo.countByBuildingId(bid);
        }
        if (existingZones == 0) {
            System.out.println("[多模态初始化] 开始生成演示建筑区域和占用数据...");
            List<BuildingZone> demoZones = createDemoZones();
            demoZones = zoneRepo.saveAll(demoZones);
            System.out.println("[多模态初始化] 已创建 " + demoZones.size() + " 个演示区域");
            generateOccupancyData(demoZones);
            generateSampleAlerts(demoZones);
        } else {
            System.out.println("[多模态初始化] 演示建筑已有 " + existingZones + " 个区域，跳过");
        }

        // 2. 为所有无区域的企业建筑自动生成通用区域
        List<Building> allBuildings = buildingRepo.findAll();
        List<BuildingZone> newEnterpriseZones = new ArrayList<>();
        for (Building b : allBuildings) {
            String bid = b.getBuildingId();
            if (Arrays.asList(DEMO_BUILDINGS).contains(bid)) continue;
            if (zoneRepo.countByBuildingId(bid) > 0) continue;

            // 根据建筑信息自动生成区域
            int floors = b.getFloorCount() != null ? b.getFloorCount() : 3;
            String bName = b.getBuildingName() != null ? b.getBuildingName() : bid;
            newEnterpriseZones.add(zone(bid, bid + "-1F大厅", "lobby", 1, 50, true));
            for (int f = 2; f <= Math.min(floors, 8); f++) {
                newEnterpriseZones.add(zone(bid, bid + "-" + f + "F办公区", "office", f, 40, f <= 3));
            }
            if (floors > 2) {
                newEnterpriseZones.add(zone(bid, bid + "-会议室", "meeting", 2, 30, true));
            }
        }
        if (!newEnterpriseZones.isEmpty()) {
            newEnterpriseZones = zoneRepo.saveAll(newEnterpriseZones);
            System.out.println("[多模态初始化] 为企业建筑补充生成 " + newEnterpriseZones.size() + " 个区域");
            generateOccupancyData(newEnterpriseZones);
            generateSampleAlerts(newEnterpriseZones);
        }

        System.out.println("[多模态初始化] 多模态监控数据初始化完成");
    }

    private List<BuildingZone> createDemoZones() {
        List<BuildingZone> zones = new ArrayList<>();
        // B001 行政楼
        zones.add(zone("B001", "B001-1F大厅", "lobby", 1, 50, true));
        zones.add(zone("B001", "B001-2F办公区A", "office", 2, 40, true));
        zones.add(zone("B001", "B001-3F办公区B", "office", 3, 40, false));
        zones.add(zone("B001", "B001-4F会议中心", "meeting", 4, 60, true));
        zones.add(zone("B001", "B001-5F领导办公", "office", 5, 20, true));
        zones.add(zone("B001", "B001-6F档案室", "storage", 6, 10, false));
        zones.add(zone("B001", "B001-7F数据中心", "server_room", 7, 5, true));
        zones.add(zone("B001", "B001-8F天台设备", "mechanical", 8, 3, true));
        // B002 图书馆
        zones.add(zone("B002", "B002-1F借阅大厅", "lobby", 1, 80, true));
        zones.add(zone("B002", "B002-2F阅览室A", "reading", 2, 100, true));
        zones.add(zone("B002", "B002-3F阅览室B", "reading", 3, 100, false));
        zones.add(zone("B002", "B002-4F电子阅览", "computer", 4, 60, true));
        zones.add(zone("B002", "B002-5F自习室", "study", 5, 80, false));
        // B003 实验楼
        zones.add(zone("B003", "B003-1F化学实验室", "lab", 1, 30, true));
        zones.add(zone("B003", "B003-2F物理实验室", "lab", 2, 30, true));
        zones.add(zone("B003", "B003-3F生物实验室", "lab", 3, 25, true));
        zones.add(zone("B003", "B003-4F精密仪器室", "lab", 4, 15, true));
        zones.add(zone("B003", "B003-5F计算实验室", "computer", 5, 40, false));
        zones.add(zone("B003", "B003-6F科研办公", "office", 6, 20, false));
        // B004 教学楼
        zones.add(zone("B004", "B004-1F阶梯教室", "classroom", 1, 200, true));
        zones.add(zone("B004", "B004-2F普通教室A", "classroom", 2, 60, false));
        zones.add(zone("B004", "B004-3F普通教室B", "classroom", 3, 60, false));
        zones.add(zone("B004", "B004-4F多媒体教室", "classroom", 4, 80, true));
        zones.add(zone("B004", "B004-5F语音教室", "classroom", 5, 50, false));
        zones.add(zone("B004", "B004-6F教师休息室", "office", 6, 20, false));
        // B005 宿舍楼
        zones.add(zone("B005", "B005-1F门厅", "lobby", 1, 30, true));
        zones.add(zone("B005", "B005-2F-6F宿舍区", "dormitory", 2, 200, false));
        zones.add(zone("B005", "B005-7F-12F宿舍区", "dormitory", 7, 200, false));
        zones.add(zone("B005", "B005-公共洗衣房", "utility", 1, 15, false));
        // B006 餐厅
        zones.add(zone("B006", "B006-1F食堂A", "canteen", 1, 300, true));
        zones.add(zone("B006", "B006-2F食堂B", "canteen", 2, 200, true));
        zones.add(zone("B006", "B006-3F教工餐厅", "canteen", 3, 80, false));
        zones.add(zone("B006", "B006-后厨", "kitchen", 1, 20, true));
        // B007 体育馆
        zones.add(zone("B007", "B007-1F篮球场", "sports", 1, 100, true));
        zones.add(zone("B007", "B007-1F游泳馆", "pool", 1, 60, true));
        zones.add(zone("B007", "B007-2F健身房", "gym", 2, 40, true));
        zones.add(zone("B007", "B007-2F乒乓球室", "sports", 2, 30, false));
        return zones;
    }

    private BuildingZone zone(String bid, String name, String type, int floor, int cap, boolean cam) {
        BuildingZone z = new BuildingZone();
        z.setBuildingId(bid);
        z.setZoneName(name);
        z.setZoneType(type);
        z.setFloorNum(floor);
        z.setCapacity(cap);
        z.setHasCamera(cam);
        z.setHasSensor(true);
        return z;
    }

    private void generateOccupancyData(List<BuildingZone> zones) {
        List<ZoneOccupancy> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (BuildingZone zone : zones) {
            // 最近24小时，每10分钟一条 = 144条
            for (int offsetMin = 0; offsetMin < 1440; offsetMin += 10) {
                LocalDateTime ts = now.minusMinutes(1440 - offsetMin);
                int hour = ts.getHour();
                int personCount = simulateOccupancy(zone.getZoneType(), zone.getCapacity(), hour, rng);

                ZoneOccupancy occ = new ZoneOccupancy();
                occ.setZoneId(zone.getId());
                occ.setBuildingId(zone.getBuildingId());
                occ.setPersonCount(personCount);
                occ.setIsOccupied(personCount > 0);
                occ.setDetectionMethod("simulated");
                occ.setRecordedAt(ts);
                batch.add(occ);

                // 分批保存避免内存溢出
                if (batch.size() >= 500) {
                    occupancyRepo.saveAll(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            occupancyRepo.saveAll(batch);
        }
        System.out.println("[多模态初始化] 已生成 " + (zones.size() * 144) + " 条占用记录");
    }

    private int simulateOccupancy(String zoneType, int capacity, int hour, ThreadLocalRandom rng) {
        if (zoneType == null) zoneType = "office";
        switch (zoneType) {
            case "lobby":
            case "meeting":
                return (hour >= 8 && hour <= 18)
                        ? rng.nextInt(0, (int)(capacity * 0.5) + 1)
                        : rng.nextInt(0, 3);
            case "office":
                if (hour >= 9 && hour <= 12 || hour >= 14 && hour <= 17)
                    return (int)(capacity * (0.5 + rng.nextDouble() * 0.4));
                if (hour >= 12 && hour < 14)
                    return (int)(capacity * (0.1 + rng.nextDouble() * 0.2));
                return rng.nextInt(0, 2);
            case "classroom":
            case "reading":
            case "study":
                if (hour >= 8 && hour <= 12 || hour >= 14 && hour <= 18)
                    return (int)(capacity * (0.4 + rng.nextDouble() * 0.5));
                if (hour >= 19 && hour <= 21)
                    return (int)(capacity * (0.2 + rng.nextDouble() * 0.3));
                return rng.nextInt(0, 3);
            case "lab":
                return (hour >= 9 && hour <= 17)
                        ? (int)(capacity * (0.3 + rng.nextDouble() * 0.5))
                        : rng.nextInt(0, 2);
            case "dormitory":
                if (hour >= 22 || hour <= 6)
                    return (int)(capacity * (0.7 + rng.nextDouble() * 0.25));
                if (hour >= 8 && hour <= 18)
                    return (int)(capacity * (0.05 + rng.nextDouble() * 0.1));
                return (int)(capacity * (0.3 + rng.nextDouble() * 0.3));
            case "canteen":
            case "kitchen":
                if (hour == 7 || hour == 8 || hour == 11 || hour == 12 || hour == 17 || hour == 18)
                    return (int)(capacity * (0.5 + rng.nextDouble() * 0.45));
                return (int)(capacity * rng.nextDouble() * 0.05);
            case "sports":
            case "gym":
            case "pool":
                if (hour >= 7 && hour <= 9 || hour >= 16 && hour <= 21)
                    return (int)(capacity * (0.2 + rng.nextDouble() * 0.5));
                return rng.nextInt(0, 5);
            case "server_room":
            case "mechanical":
                return rng.nextDouble() < 0.05 ? 1 : 0;
            case "computer":
                return (hour >= 8 && hour <= 21)
                        ? (int)(capacity * (0.3 + rng.nextDouble() * 0.5))
                        : 0;
            default:
                return rng.nextInt(0, (int)(capacity * 0.3) + 1);
        }
    }

    private void generateSampleAlerts(List<BuildingZone> zones) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<AnomalyAlert> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (BuildingZone zone : zones) {
            String type = zone.getZoneType();
            if (type == null) continue;

            // 办公/教室区域 — 无人照明告警
            if ((type.equals("office") || type.equals("classroom")) && rng.nextDouble() < 0.4) {
                AnomalyAlert a = new AnomalyAlert();
                a.setBuildingId(zone.getBuildingId());
                a.setZoneId(zone.getId());
                a.setZoneName(zone.getZoneName());
                a.setAnomalyCategory("empty_waste");
                a.setAnomalyType("无人照明");
                a.setSeverity("warning");
                a.setEnergyValue(2.0 + rng.nextDouble() * 4);
                a.setPersonCount(0);
                a.setIsWorkHour(true);
                a.setEvidenceSummary(zone.getZoneName() + " 无人，照明功率 " +
                        String.format("%.1f", a.getEnergyValue()) + "kW 仍在运行");
                a.setEstimatedWasteKwh(Math.round(a.getEnergyValue() * 0.083 * 100.0) / 100.0);
                a.setEstimatedCost(Math.round(a.getEstimatedWasteKwh() * 0.8 * 100.0) / 100.0);
                a.setStatus("active");
                a.setCreatedAt(now.minusHours(rng.nextInt(1, 12)));
                alerts.add(a);
            }

            // 无人空调
            if ((type.equals("office") || type.equals("lab") || type.equals("reading")) && rng.nextDouble() < 0.3) {
                AnomalyAlert a = new AnomalyAlert();
                a.setBuildingId(zone.getBuildingId());
                a.setZoneId(zone.getId());
                a.setZoneName(zone.getZoneName());
                a.setAnomalyCategory("empty_waste");
                a.setAnomalyType("无人空调");
                a.setSeverity("severe");
                a.setEnergyValue(5.0 + rng.nextDouble() * 8);
                a.setPersonCount(0);
                a.setIsWorkHour(true);
                a.setEvidenceSummary(zone.getZoneName() + " 已无人超30分钟，空调 " +
                        String.format("%.1f", a.getEnergyValue()) + "kW 持续运行");
                a.setEstimatedWasteKwh(Math.round(a.getEnergyValue() * 0.5 * 100.0) / 100.0);
                a.setEstimatedCost(Math.round(a.getEstimatedWasteKwh() * 0.8 * 100.0) / 100.0);
                a.setStatus("active");
                a.setCreatedAt(now.minusHours(rng.nextInt(1, 8)));
                alerts.add(a);
            }

            // 人少能耗高
            if ((type.equals("classroom") || type.equals("lab")) && rng.nextDouble() < 0.25) {
                AnomalyAlert a = new AnomalyAlert();
                a.setBuildingId(zone.getBuildingId());
                a.setZoneId(zone.getId());
                a.setZoneName(zone.getZoneName());
                a.setAnomalyCategory("pattern");
                a.setAnomalyType("人少能耗高");
                a.setSeverity("warning");
                a.setEnergyValue(8.0 + rng.nextDouble() * 10);
                a.setPersonCount(rng.nextInt(1, 3));
                a.setIsWorkHour(true);
                a.setEvidenceSummary(zone.getZoneName() + " 仅 " + a.getPersonCount() +
                        " 人但功率 " + String.format("%.1f", a.getEnergyValue()) + "kW，人均能耗异常偏高");
                a.setEstimatedWasteKwh(Math.round(a.getEnergyValue() * 0.05 * 100.0) / 100.0);
                a.setEstimatedCost(Math.round(a.getEstimatedWasteKwh() * 0.8 * 100.0) / 100.0);
                a.setStatus("active");
                a.setCreatedAt(now.minusHours(rng.nextInt(1, 10)));
                alerts.add(a);
            }
        }

        alertRepo.saveAll(alerts);
        System.out.println("[多模态初始化] 已生成 " + alerts.size() + " 条异常告警");
    }
}
