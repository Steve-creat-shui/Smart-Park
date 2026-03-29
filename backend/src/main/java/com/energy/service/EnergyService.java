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
        return energyRepo.findAllBuildingIds();
    }

    public List<Building> getAllBuildings() {
        return buildingRepo.findAll();
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

        String contextData = buildContextString(request.getBuildingId(), totalPower, totalWater,
                totalCarbon, totalAc, avgPower, dataList.size(), anomalies.size());
        String query = request.getUserQuery() != null ? request.getUserQuery()
                : String.format("分析建筑%s从%s到%s的能耗情况", request.getBuildingId(), start, end);
        String aiResult = claudeService.analyzeEnergyData(contextData, query);

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
        List<String> buildingIds = energyRepo.findAllBuildingIds();
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
        stats.put("buildingCount", buildingIds.size());
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

        for (Object[] row : monthlyData) {
            months.add((String) row[0]);
            powerList.add(row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 100.0) / 100.0 : 0.0);
            waterList.add(row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 100.0) / 100.0 : 0.0);
            carbonList.add(row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 100.0) / 100.0 : 0.0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildingId", buildingId);
        result.put("months", months);
        result.put("powerConsumption", powerList);
        result.put("waterConsumption", waterList);
        result.put("carbonEmission", carbonList);
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

    private List<Map<String, Object>> detectAnomalies(List<EnergyConsumption> dataList, double avg) {
        return dataList.stream()
                .filter(d -> d.getPowerConsumptionKwh() != null && d.getPowerConsumptionKwh() > avg * 1.5)
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("timestamp", d.getTimestamp() != null ? d.getTimestamp().toString() : "");
                    m.put("power", d.getPowerConsumptionKwh());
                    m.put("excess", Math.round((d.getPowerConsumptionKwh() - avg) / avg * 100.0) / 100.0);
                    m.put("type", "高能耗异常");
                    return m;
                })
                .collect(Collectors.toList());
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
}
