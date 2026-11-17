package com.energy.service;

import com.energy.dto.EnergyAnalysisRequest;
import com.energy.dto.EnergyAnalysisResponse;
import com.energy.entity.Building;
import com.energy.entity.EnergyConsumption;
import com.energy.repository.BuildingRepository;
import com.energy.repository.EnergyConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnergyService {

    private final EnergyConsumptionRepository energyRepo;
    private final BuildingRepository buildingRepo;
    private final CozeService cozeService;
    private final ClaudeService claudeService;

    public List<EnergyConsumption> getEnergyDataByBuilding(String buildingId) {
        return energyRepo.findByBuildingIdOrderByTimestampDesc(buildingId);
    }

    public List<EnergyConsumption> getEnergyDataByBuildingAndPeriod(
            String buildingId, LocalDateTime start, LocalDateTime end) {
        return energyRepo.findByBuildingIdAndTimestampBetweenOrderByTimestampAsc(buildingId, start, end);
    }

    public List<EnergyConsumption> getLatestEnergyData(String buildingId, int limit) {
        return energyRepo.findByBuildingIdOrderByTimestampDesc(buildingId, PageRequest.of(0, limit));
    }

    public List<EnergyConsumption> getAnomalies(String buildingId) {
        return energyRepo.findAnomaliesByBuilding(buildingId);
    }

    public List<String> getAllBuildingIds() {
        List<String> ids = buildingRepo.findDemoBuildings().stream()
                .map(Building::getBuildingId)
                .sorted()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return energyRepo.findAllBuildingIds();
        }
        return ids;
    }

    public List<String> getAvailableDates(String buildingId) {
        return energyRepo.findDistinctDatesByBuildingId(buildingId);
    }

    public List<Building> getAllBuildings() {
        return buildingRepo.findDemoBuildings();
    }

    public Optional<Building> getBuildingById(String buildingId) {
        return buildingRepo.findById(buildingId);
    }

    @Transactional
    public EnergyConsumption saveEnergyData(EnergyConsumption data) {
        return energyRepo.save(data);
    }

    @Transactional
    public Building saveBuilding(Building building) {
        return buildingRepo.save(building);
    }

    public EnergyAnalysisResponse analyzeEnergy(EnergyAnalysisRequest request) {
        LocalDateTime start = request.getStartTime() != null
                ? request.getStartTime() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = request.getEndTime() != null
                ? request.getEndTime() : LocalDateTime.now();

        List<EnergyConsumption> dataList = energyRepo
                .findByBuildingIdAndTimestampBetweenOrderByTimestampAsc(request.getBuildingId(), start, end);

        if (dataList.isEmpty()) {
            return EnergyAnalysisResponse.builder()
                    .buildingId(request.getBuildingId())
                    .analysisType(request.getAnalysisType())
                    .aiAnalysisResult("暂无该时段的能耗数据，请检查建筑ID或时间范围。")
                    .build();
        }

        double totalPower = dataList.stream()
                .mapToDouble(d -> d.getPowerConsumptionKwh() != null ? d.getPowerConsumptionKwh() : 0)
                .sum();
        double totalWater = dataList.stream()
                .mapToDouble(d -> d.getWaterConsumptionM3() != null ? d.getWaterConsumptionM3() : 0)
                .sum();
        double totalCarbon = dataList.stream()
                .mapToDouble(d -> d.getCarbonEmissionKg() != null ? d.getCarbonEmissionKg() : 0)
                .sum();
        double totalAc = dataList.stream()
                .mapToDouble(d -> d.getAcConsumptionKwh() != null ? d.getAcConsumptionKwh() : 0)
                .sum();
        double avgPower = totalPower / dataList.size();

        double efficiencyScore = calculateEfficiencyScore(avgPower, totalCarbon, dataList.size());
        String rating = calculateEnergyRating(efficiencyScore);

        List<Map<String, Object>> trendData = buildTrendData(dataList);
        List<Map<String, Object>> anomalies = detectAnomalies(dataList, avgPower);

        String aiResult = "";
        if (!request.isSkipAiAnalysis()) {
            String contextData = buildContextString(request.getBuildingId(), totalPower, totalWater,
                    totalCarbon, totalAc, avgPower, dataList.size(), anomalies.size());
            String query = request.getUserQuery() != null ? request.getUserQuery()
                    : String.format("分析建筑%s从%s到%s的能耗情况", request.getBuildingId(), start, end);
            aiResult = cozeService.isConfigured()
                    ? cozeService.analyzeEnergyData(contextData, query)
                    : claudeService.analyzeEnergyData(contextData, query);
        }

        return EnergyAnalysisResponse.builder()
                .buildingId(request.getBuildingId())
                .analysisType(request.getAnalysisType())
                .totalPowerConsumption(Math.round(totalPower * 100.0) / 100.0)
                .totalWaterConsumption(Math.round(totalWater * 100.0) / 100.0)
                .totalCarbonEmission(Math.round(totalCarbon * 100.0) / 100.0)
                .totalAcConsumption(Math.round(totalAc * 100.0) / 100.0)
                .avgPowerConsumption(Math.round(avgPower * 100.0) / 100.0)
                .energyEfficiencyScore(Math.round(efficiencyScore * 100.0) / 100.0)
                .energyRating(rating)
                .trendData(trendData)
                .anomalies(anomalies)
                .aiAnalysisResult(aiResult)
                .build();
    }

    public Map<String, Object> getDashboardStats() {
        List<String> buildingIds = getAllBuildingIds();
        long totalBuildingCount = buildingRepo.countDemoBuildings();
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        double totalMonthlyPower = 0;
        double totalMonthlyCarbon = 0;
        int alertCount = 0;

        for (String bid : buildingIds) {
            Double power = energyRepo.sumPowerConsumptionByBuildingAndPeriod(bid, monthStart, now);
            Double carbon = energyRepo.sumCarbonEmissionByBuildingAndPeriod(bid, monthStart, now);
            totalMonthlyPower += power != null ? power : 0;
            totalMonthlyCarbon += carbon != null ? carbon : 0;
            alertCount += energyRepo.findAnomaliesByBuilding(bid).size();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("buildingCount", totalBuildingCount);
        stats.put("monthlyPowerConsumption", Math.round(totalMonthlyPower * 100.0) / 100.0);
        stats.put("monthlyCarbonEmission", Math.round(totalMonthlyCarbon * 100.0) / 100.0);
        stats.put("alertCount", alertCount);
        stats.put("buildingIds", buildingIds);

        List<Object[]> rankings = energyRepo.findBuildingRankingByPower(monthStart, now);
        List<Map<String, Object>> rankingList = rankings.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("buildingId", row[0]);
            m.put("totalPower", row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 100.0) / 100.0 : 0);
            return m;
        }).collect(Collectors.toList());
        stats.put("buildingRanking", rankingList);

        return stats;
    }

    public Map<String, Object> getMonthlyTrend(String buildingId) {
        List<Object[]> monthlyData = energyRepo.findMonthlyStatsByBuilding(buildingId);
        List<String> months = new ArrayList<>();
        List<Double> powerList = new ArrayList<>();
        List<Double> waterList = new ArrayList<>();
        List<Double> carbonList = new ArrayList<>();
        List<Double> acList = new ArrayList<>();

        for (Object[] row : monthlyData) {
            months.add((String) row[0]);
            powerList.add(row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 100.0) / 100.0 : 0.0);
            waterList.add(row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 100.0) / 100.0 : 0.0);
            carbonList.add(row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 100.0) / 100.0 : 0.0);
            acList.add(row[4] != null ? Math.round(((Number) row[4]).doubleValue() * 100.0) / 100.0 : 0.0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildingId", buildingId);
        result.put("months", months);
        result.put("powerConsumption", powerList);
        result.put("waterConsumption", waterList);
        result.put("carbonEmission", carbonList);
        result.put("acConsumption", acList);
        return result;
    }

    private List<Map<String, Object>> buildTrendData(List<EnergyConsumption> dataList) {
        return dataList.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timestamp", d.getTimestamp() != null ? d.getTimestamp().toString() : "");
            m.put("power", d.getPowerConsumptionKwh());
            m.put("water", d.getWaterConsumptionM3());
            m.put("carbon", d.getCarbonEmissionKg());
            m.put("ac", d.getAcConsumptionKwh());
            m.put("status", d.getStatus());
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> detectAnomalies(List<EnergyConsumption> dataList, double avgPower) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        double avgWater = dataList.stream()
                .mapToDouble(d -> d.getWaterConsumptionM3() != null ? d.getWaterConsumptionM3() : 0).average().orElse(0);
        double avgCarbon = dataList.stream()
                .mapToDouble(d -> d.getCarbonEmissionKg() != null ? d.getCarbonEmissionKg() : 0).average().orElse(0);
        double avgAc = dataList.stream()
                .mapToDouble(d -> d.getAcConsumptionKwh() != null ? d.getAcConsumptionKwh() : 0).average().orElse(0);

        // 标准差用于更细粒度判断
        double sdPower = std(dataList, avgPower, d -> d.getPowerConsumptionKwh());
        double sdWater = std(dataList, avgWater, d -> d.getWaterConsumptionM3());
        double sdCarbon = std(dataList, avgCarbon, d -> d.getCarbonEmissionKg());
        double sdAc    = std(dataList, avgAc,    d -> d.getAcConsumptionKwh());

        for (EnergyConsumption d : dataList) {
            double power  = d.getPowerConsumptionKwh() != null ? d.getPowerConsumptionKwh() : 0;
            double water  = d.getWaterConsumptionM3()  != null ? d.getWaterConsumptionM3() : 0;
            double carbon = d.getCarbonEmissionKg()    != null ? d.getCarbonEmissionKg() : 0;
            double ac     = d.getAcConsumptionKwh()    != null ? d.getAcConsumptionKwh() : 0;
            int hour = d.getTimestamp() != null ? d.getTimestamp().getHour() : 12;
            String ts = d.getTimestamp() != null ? d.getTimestamp().toString() : "";

            // 计算每个维度的偏差倍数（z-score 风格）
            double zPower  = sdPower  > 0 ? (power  - avgPower)  / sdPower  : 0;
            double zWater  = sdWater  > 0 ? (water  - avgWater)  / sdWater  : 0;
            double zCarbon = sdCarbon > 0 ? (carbon - avgCarbon) / sdCarbon : 0;
            double zAc     = sdAc     > 0 ? (ac     - avgAc)     / sdAc     : 0;
            double acRatio = power > 0 ? ac / power : 0;
            boolean isNight = hour >= 0 && hour < 6;

            // 找到最突出的偏差维度
            String bestType = null;
            String severity = "warning";
            double excess = 0;
            String detail = "";

            // 夜间异常优先（特殊时段检测，阈值较低）
            if (isNight && zPower > 0.8) {
                bestType = "夜间异常";
                severity = zPower > 2 ? "warning" : "info";
                excess = Math.round((power - avgPower * 0.5) / Math.max(avgPower * 0.5, 1) * 100.0) / 100.0;
                detail = String.format("凌晨%d时电力%.1fkWh，疑似设备未关闭", hour, power);
            }
            // 能效偏低（空调占比过高，即使总电力不高）
            else if (acRatio > 0.7 && power > avgPower * 0.8 && zAc > 1.0) {
                bestType = "能效偏低";
                severity = acRatio > 0.85 ? "warning" : "info";
                excess = Math.round(acRatio * 100.0) / 100.0;
                detail = String.format("空调占比%.0f%%，空调能耗%.1fkWh，建议检查温控", acRatio * 100, ac);
            }
            // 其余按最大z-score分类
            else {
                double maxZ = Math.max(Math.max(zPower, zAc), Math.max(zWater, zCarbon));
                if (maxZ < 1.5) continue; // 不够异常，跳过

                if (zAc == maxZ && zAc > 1.5) {
                    bestType = "空调过载";
                    severity = zAc > 2.5 ? "severe" : "warning";
                    excess = avgAc > 0 ? Math.round((ac - avgAc) / avgAc * 100.0) / 100.0 : 0;
                    detail = String.format("空调能耗%.1fkWh，超出均值%.0f%%", ac, avgAc > 0 ? (ac - avgAc) / avgAc * 100 : 0);
                } else if (zWater == maxZ && zWater > 1.5) {
                    bestType = "用水异常";
                    severity = zWater > 2.5 ? "severe" : "warning";
                    excess = avgWater > 0 ? Math.round((water - avgWater) / avgWater * 100.0) / 100.0 : 0;
                    detail = String.format("用水量%.2fm³，超出均值%.0f%%", water, avgWater > 0 ? (water - avgWater) / avgWater * 100 : 0);
                } else if (zCarbon == maxZ && zCarbon > 1.5) {
                    bestType = "碳排超标";
                    severity = zCarbon > 2.5 ? "severe" : "warning";
                    excess = avgCarbon > 0 ? Math.round((carbon - avgCarbon) / avgCarbon * 100.0) / 100.0 : 0;
                    detail = String.format("碳排放%.1fkg，超出均值%.0f%%", carbon, avgCarbon > 0 ? (carbon - avgCarbon) / avgCarbon * 100 : 0);
                } else if (zPower >= 1.5) {
                    bestType = "高能耗异常";
                    severity = zPower > 2.5 ? "severe" : "warning";
                    excess = avgPower > 0 ? Math.round((power - avgPower) / avgPower * 100.0) / 100.0 : 0;
                    detail = String.format("电力消耗%.1fkWh，超出均值%.0f%%", power, avgPower > 0 ? (power - avgPower) / avgPower * 100 : 0);
                }
            }

            if (bestType != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("timestamp", ts);
                m.put("power", power);
                m.put("excess", excess);
                m.put("type", bestType);
                m.put("severity", severity);
                m.put("detail", detail);
                anomalies.add(m);
            }
        }

        anomalies.sort((a, b) -> {
            int typeCmp = String.valueOf(a.get("type")).compareTo(String.valueOf(b.get("type")));
            if (typeCmp != 0) return typeCmp;
            double ea = ((Number) a.get("excess")).doubleValue();
            double eb = ((Number) b.get("excess")).doubleValue();
            return Double.compare(eb, ea);
        });

        return anomalies;
    }

    private double std(List<EnergyConsumption> list, double mean,
                       java.util.function.Function<EnergyConsumption, Double> getter) {
        double sumSq = list.stream().mapToDouble(d -> {
            double v = getter.apply(d) != null ? getter.apply(d) : 0;
            return (v - mean) * (v - mean);
        }).sum();
        return Math.sqrt(sumSq / Math.max(list.size(), 1));
    }

    private double calculateEfficiencyScore(double avgPower, double totalCarbon, int dataPoints) {
        double baseScore = 100.0;
        if (avgPower > 500) baseScore -= 20;
        else if (avgPower > 300) baseScore -= 10;
        else if (avgPower > 100) baseScore -= 5;
        if (dataPoints > 0 && totalCarbon / dataPoints > 400) baseScore -= 15;
        else if (dataPoints > 0 && totalCarbon / dataPoints > 200) baseScore -= 8;
        return Math.max(0, Math.min(100, baseScore));
    }

    private String calculateEnergyRating(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "E";
    }

    private String buildContextString(String buildingId, double totalPower, double totalWater,
                                       double totalCarbon, double totalAc, double avgPower,
                                       int dataPoints, int anomalyCount) {
        return String.format(
                "建筑ID: %s\n数据记录数: %d\n总电力消耗: %.2f kWh\n" +
                "总水能耗: %.2f m3\n总碳排放: %.2f kg\n空调总能耗: %.2f kWh\n" +
                "平均电力消耗: %.2f kWh\n异常记录数: %d\n能效比(空调/总电力): %.1f%%",
                buildingId, dataPoints, totalPower, totalWater, totalCarbon,
                totalAc, avgPower, anomalyCount,
                totalPower > 0 ? totalAc / totalPower * 100 : 0);
    }

    // ==================== 企业数据隔离 ====================
    private static final Long DEMO_ENTERPRISE_ID = 1L;

    private boolean isDemoOrNull(Long eid) {
        // null → 未登录/无token → 演示数据
        // DEMO_ENTERPRISE_ID(1) → 演示企业 → 演示数据
        // 负数(-userId) → 个人用户 → 使用演示数据（7栋楼）
        return eid == null || eid.equals(DEMO_ENTERPRISE_ID) || eid < 0;
    }

    // 个人用户使用 -userId 作为虚拟企业ID
    // 负数 enterpriseId → 个人用户，显示演示数据
    // 正数 enterpriseId → 企业隔离数据
    // null 或 DEMO_ENTERPRISE_ID → 演示数据

    public List<String> getBuildingIdsByEnterprise(Long enterpriseId) {
        if (isDemoOrNull(enterpriseId)) return getAllBuildingIds();
        List<String> ids = energyRepo.findBuildingIdsByEnterpriseId(enterpriseId);
        if (ids.isEmpty()) {
            ids = buildingRepo.findByEnterpriseId(enterpriseId).stream()
                    .map(Building::getBuildingId).sorted().collect(Collectors.toList());
        }
        return ids;
    }

    public List<Building> getBuildingsByEnterprise(Long enterpriseId) {
        if (isDemoOrNull(enterpriseId)) return getAllBuildings();
        return buildingRepo.findByEnterpriseId(enterpriseId);
    }

    public List<EnergyConsumption> getEnergyDataByEnterprise(Long enterpriseId, String buildingId) {
        if (isDemoOrNull(enterpriseId)) return getEnergyDataByBuilding(buildingId);
        return energyRepo.findByEnterpriseIdAndBuildingIdOrderByTimestampDesc(enterpriseId, buildingId);
    }

    public List<EnergyConsumption> getEnergyDataByEnterprisePeriod(Long enterpriseId, String buildingId, LocalDateTime start, LocalDateTime end) {
        if (isDemoOrNull(enterpriseId)) return getEnergyDataByBuildingAndPeriod(buildingId, start, end);
        return energyRepo.findByEnterpriseIdAndBuildingIdAndTimestampBetweenOrderByTimestampAsc(enterpriseId, buildingId, start, end);
    }

    public Map<String, Object> getDashboardStatsByEnterprise(Long enterpriseId) {
        if (isDemoOrNull(enterpriseId)) return getDashboardStats();

        List<String> buildingIds = getBuildingIdsByEnterprise(enterpriseId);
        long totalBuildingCount = buildingRepo.countByEnterpriseId(enterpriseId);
        if (totalBuildingCount == 0) totalBuildingCount = buildingIds.size();
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        double totalMonthlyPower = 0;
        double totalMonthlyCarbon = 0;

        for (String bid : buildingIds) {
            Double power = energyRepo.sumPowerByEnterpriseBuildingPeriod(enterpriseId, bid, monthStart, now);
            Double carbon = energyRepo.sumCarbonByEnterpriseBuildingPeriod(enterpriseId, bid, monthStart, now);
            totalMonthlyPower += power != null ? power : 0;
            totalMonthlyCarbon += carbon != null ? carbon : 0;
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("buildingCount", totalBuildingCount);
        stats.put("monthlyPowerConsumption", Math.round(totalMonthlyPower * 100.0) / 100.0);
        stats.put("monthlyCarbonEmission", Math.round(totalMonthlyCarbon * 100.0) / 100.0);
        stats.put("alertCount", 0);
        stats.put("buildingIds", buildingIds);

        List<Object[]> rankings = energyRepo.findBuildingRankingByPowerAndEnterprise(enterpriseId, monthStart, now);
        List<Map<String, Object>> rankingList = rankings.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("buildingId", row[0]);
            m.put("totalPower", row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 100.0) / 100.0 : 0);
            return m;
        }).collect(Collectors.toList());
        stats.put("buildingRanking", rankingList);

        return stats;
    }

    public boolean enterpriseHasData(Long enterpriseId) {
        if (enterpriseId == null) return false;
        return energyRepo.countByEnterpriseId(enterpriseId) > 0;
    }
}
