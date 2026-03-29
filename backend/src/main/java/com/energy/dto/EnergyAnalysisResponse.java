package com.energy.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyAnalysisResponse {
    private String buildingId;
    private String analysisType;
    private Double totalPowerConsumption;
    private Double totalWaterConsumption;
    private Double totalCarbonEmission;
    private Double totalAcConsumption;
    private Double avgPowerConsumption;
    private Double energyEfficiencyScore;
    private String energyRating;
    private List<Map<String, Object>> trendData;
    private List<Map<String, Object>> anomalies;
    private String aiAnalysisResult;
    private String recommendations;
    private Map<String, Object> comparisonData;
}
