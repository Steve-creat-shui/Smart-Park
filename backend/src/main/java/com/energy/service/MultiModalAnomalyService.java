package com.energy.service;

import com.energy.entity.*;
import com.energy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiModalAnomalyService {

    private final BuildingZoneRepository zoneRepo;
    private final ZoneOccupancyRepository occupancyRepo;
    private final AnomalyAlertRepository alertRepo;
    private final EnergyConsumptionRepository energyRepo;
    private final BuildingRepository buildingRepo;

    private static final double ELECTRICITY_PRICE = 0.8; // 元/kWh

    @Scheduled(fixedRate = 300000) // 每5分钟分析
    public void crossAnalyze() {
        List<Building> buildings = buildingRepo.findAll();
        for (Building building : buildings) {
            analyzeBuilding(building.getBuildingId());
        }
    }

    public void analyzeBuilding(String buildingId) {
        List<BuildingZone> zones = zoneRepo.findByBuildingId(buildingId);
        // 获取最新能耗
        List<EnergyConsumption> latest = energyRepo.findByBuildingIdOrderByTimestampDesc(buildingId, PageRequest.of(0, 1));
        if (latest.isEmpty()) return;
        EnergyConsumption energy = latest.get(0);

        int hour = LocalTime.now().getHour();
        DayOfWeek dow = LocalDateTime.now().getDayOfWeek();
        boolean isWorkHour = hour >= 8 && hour < 18 && dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        boolean isNight = hour >= 23 || hour < 5;

        for (BuildingZone zone : zones) {
            Optional<ZoneOccupancy> latestOcc = occupancyRepo.findTopByZoneIdOrderByRecordedAtDesc(zone.getId());
            if (latestOcc.isEmpty()) continue;

            ZoneOccupancy occ = latestOcc.get();
            boolean hasNobody = occ.getPersonCount() == 0;
            double totalPower = energy.getPowerConsumptionKwh() != null ? energy.getPowerConsumptionKwh() : 0;
            double hvacPower = energy.getAcConsumptionKwh() != null ? energy.getAcConsumptionKwh() : 0;
            double lightPower = energy.getLightingConsumptionKwh() != null ? energy.getLightingConsumptionKwh() : 0;
            // 按区域数分摊
            int zoneCount = Math.max(1, zones.size());
            double zonePower = totalPower / zoneCount;
            double zoneHvac = hvacPower / zoneCount;
            double zoneLight = lightPower / zoneCount;

            // 防重复：同一小时同一类型只告警一次
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            // 规则1: 无人照明
            if (hasNobody && zoneLight > 0.3) {
                if (!hasDuplicate(buildingId, zone.getId(), "无人照明", oneHourAgo)) {
                    createAlert(buildingId, zone, "empty_waste", "无人照明", "warning",
                            String.format("%s 无人，照明功率 %.1fkW 仍在运行", zone.getZoneName(), zoneLight),
                            occ.getPersonCount(), isWorkHour, zoneLight * 0.083, zoneLight);
                }
            }

            // 规则2: 无人空调（持续无人）
            if (hasNobody && zoneHvac > 0.8) {
                // 检查过去30分钟是否一直无人
                long emptyRecords = occupancyRepo.countEmptySince(zone.getId(), LocalDateTime.now().minusMinutes(30));
                if (emptyRecords >= 3) { // 至少3次检测无人(30分钟内)
                    if (!hasDuplicate(buildingId, zone.getId(), "无人空调", oneHourAgo)) {
                        createAlert(buildingId, zone, "empty_waste", "无人空调", "severe",
                                String.format("%s 已无人超30分钟，空调 %.1fkW 持续运行", zone.getZoneName(), zoneHvac),
                                occ.getPersonCount(), isWorkHour, zoneHvac * 0.5, zoneHvac);
                    }
                }
            }

            // 规则3: 非工作时间无人设备
            if (!isWorkHour && hasNobody && zonePower > 1.5) {
                if (!hasDuplicate(buildingId, zone.getId(), "无人设备运行", oneHourAgo)) {
                    createAlert(buildingId, zone, "empty_waste", "无人设备运行", "warning",
                            String.format("非工作时间 %s 无人，总功率 %.1fkW", zone.getZoneName(), zonePower),
                            occ.getPersonCount(), isWorkHour, zonePower * 0.083, zonePower);
                }
            }

            // 规则4: 夜间有人
            if (isNight && occ.getPersonCount() > 0 && !zone.getZoneType().equals("dormitory")) {
                if (!hasDuplicate(buildingId, zone.getId(), "夜间异常人员", oneHourAgo)) {
                    createAlert(buildingId, zone, "behavior", "夜间异常人员", "info",
                            String.format("深夜 %02d:%02d %s 检测到 %d 人", hour, LocalTime.now().getMinute(),
                                    zone.getZoneName(), occ.getPersonCount()),
                            occ.getPersonCount(), false, 0, 0);
                }
            }

            // 规则5: 能耗与人数不匹配（人少但能耗高）
            if (occ.getPersonCount() > 0 && occ.getPersonCount() <= 2 && zonePower > 5) {
                if (!hasDuplicate(buildingId, zone.getId(), "人少能耗高", oneHourAgo)) {
                    createAlert(buildingId, zone, "pattern", "人少能耗高", "warning",
                            String.format("%s 仅 %d 人但功率 %.1fkW，人均能耗异常偏高",
                                    zone.getZoneName(), occ.getPersonCount(), zonePower),
                            occ.getPersonCount(), isWorkHour, zonePower * 0.05, zonePower);
                }
            }
        }
    }

    private boolean hasDuplicate(String buildingId, Long zoneId, String type, LocalDateTime after) {
        return alertRepo.existsByBuildingIdAndZoneIdAndAnomalyTypeAndStatusAndCreatedAtAfter(
                buildingId, zoneId, type, "active", after);
    }

    private void createAlert(String buildingId, BuildingZone zone, String category, String type,
                             String severity, String evidence, int personCount, boolean isWorkHour,
                             double wasteKwh, double energyValue) {
        AnomalyAlert alert = new AnomalyAlert();
        alert.setBuildingId(buildingId);
        alert.setZoneId(zone.getId());
        alert.setZoneName(zone.getZoneName());
        alert.setAnomalyCategory(category);
        alert.setAnomalyType(type);
        alert.setSeverity(severity);
        alert.setEvidenceSummary(evidence);
        alert.setPersonCount(personCount);
        alert.setIsWorkHour(isWorkHour);
        alert.setEstimatedWasteKwh(Math.round(wasteKwh * 100.0) / 100.0);
        alert.setEstimatedCost(Math.round(wasteKwh * ELECTRICITY_PRICE * 100.0) / 100.0);
        alert.setEnergyValue(energyValue);
        alert.setStatus("active");
        alert.setCreatedAt(LocalDateTime.now());
        alertRepo.save(alert);
    }

    // ===== 查询 API =====

    public List<Map<String, Object>> getZoneStatus(String buildingId) {
        List<BuildingZone> zones = zoneRepo.findByBuildingId(buildingId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BuildingZone zone : zones) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("zoneId", zone.getId());
            m.put("zoneName", zone.getZoneName());
            m.put("zoneType", zone.getZoneType());
            m.put("floor", zone.getFloorNum());
            m.put("capacity", zone.getCapacity());
            Optional<ZoneOccupancy> latest = occupancyRepo.findTopByZoneIdOrderByRecordedAtDesc(zone.getId());
            m.put("personCount", latest.map(ZoneOccupancy::getPersonCount).orElse(0));
            m.put("isOccupied", latest.map(ZoneOccupancy::getIsOccupied).orElse(false));
            m.put("lastDetected", latest.map(o -> o.getRecordedAt().toString()).orElse(null));
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> getOccupancyHistory(Long zoneId, int hours) {
        LocalDateTime start = LocalDateTime.now().minusHours(hours);
        return occupancyRepo.findByZoneIdAndRecordedAtBetweenOrderByRecordedAtAsc(zoneId, start, LocalDateTime.now())
                .stream().map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("time", o.getRecordedAt().toString());
                    m.put("personCount", o.getPersonCount());
                    m.put("isOccupied", o.getIsOccupied());
                    return m;
                }).collect(Collectors.toList());
    }

    public List<AnomalyAlert> getAlerts(String buildingId, String status) {
        if (status != null && !status.isEmpty()) {
            return alertRepo.findByBuildingIdAndStatusOrderByCreatedAtDesc(buildingId, status);
        }
        return alertRepo.findByBuildingIdOrderByCreatedAtDesc(buildingId);
    }

    public void acknowledgeAlert(Long alertId) {
        alertRepo.findById(alertId).ifPresent(a -> { a.setStatus("acknowledged"); alertRepo.save(a); });
    }

    public void resolveAlert(Long alertId) {
        alertRepo.findById(alertId).ifPresent(a -> {
            a.setStatus("resolved");
            a.setResolvedAt(LocalDateTime.now());
            alertRepo.save(a);
        });
    }

    public Map<String, Object> getDashboardData(String buildingId) {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        // 区域状态
        data.put("zones", getZoneStatus(buildingId));

        // 当前总人数
        List<Map<String, Object>> zones = getZoneStatus(buildingId);
        int totalPeople = zones.stream().mapToInt(z -> (int) z.getOrDefault("personCount", 0)).sum();
        data.put("totalPeople", totalPeople);

        // 活跃告警数
        long activeAlerts = alertRepo.countByBuildingIdAndStatus(buildingId, "active");
        data.put("activeAlerts", activeAlerts);

        // 近24小时告警列表
        LocalDateTime last24h = now.minusHours(24);
        List<AnomalyAlert> todayAlerts = alertRepo.findByBuildingIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                buildingId, last24h, now);
        data.put("todayAlerts", todayAlerts);
        data.put("todayAlertCount", todayAlerts.size());

        // 本月浪费统计
        Double monthWaste = alertRepo.totalWasteSince(buildingId, monthStart);
        data.put("monthWasteKwh", Math.round(monthWaste * 100.0) / 100.0);
        data.put("monthWasteCost", Math.round(monthWaste * ELECTRICITY_PRICE * 100.0) / 100.0);

        // 告警类型分布
        List<Object[]> typeCounts = alertRepo.countByTypesSince(buildingId, monthStart);
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        for (Object[] row : typeCounts) {
            typeDistribution.put((String) row[0], (Long) row[1]);
        }
        data.put("alertTypeDistribution", typeDistribution);

        return data;
    }
}
