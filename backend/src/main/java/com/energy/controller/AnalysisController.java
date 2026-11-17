package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.dto.EnergyAnalysisRequest;
import com.energy.dto.EnergyAnalysisResponse;
import com.energy.service.ClaudeService;
import com.energy.service.CozeService;
import com.energy.service.EnergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final EnergyService energyService;
    private final CozeService cozeService;
    private final ClaudeService claudeService;

    @PostMapping("/energy-analysis")
    public ResponseEntity<ApiResponse<EnergyAnalysisResponse>> analyzeEnergy(
            @RequestBody EnergyAnalysisRequest request) {
        EnergyAnalysisResponse result = energyService.analyzeEnergy(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/ai-query")
    public ResponseEntity<ApiResponse<Map<String, String>>> aiQuery(
            @RequestBody Map<String, String> body) {
        String buildingId = body.getOrDefault("buildingId", "");
        String query = body.getOrDefault("query", "");
        String contextData = body.getOrDefault("contextData", "");
        String result = cozeService.isConfigured()
                ? cozeService.analyzeEnergyData(contextData, query)
                : claudeService.analyzeEnergyData(contextData, query);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "buildingId", buildingId,
                "query", query,
                "result", result
        )));
    }

    @GetMapping("/energy-analysis/{building_id}/issue")
    public ResponseEntity<ApiResponse<Map<String, String>>> analyzeIssue(
            @PathVariable("building_id") String buildingId,
            @RequestParam(defaultValue = "最近一个月") String timeRange) {
        EnergyAnalysisRequest req = new EnergyAnalysisRequest();
        req.setBuildingId(buildingId);
        EnergyAnalysisResponse analysis = energyService.analyzeEnergy(req);
        String contextData = String.format("建筑%s: 总电力%.2fkWh, 异常数%d条, 平均能耗%.2fkWh",
                buildingId,
                analysis.getTotalPowerConsumption() != null ? analysis.getTotalPowerConsumption() : 0,
                analysis.getAnomalies() != null ? analysis.getAnomalies().size() : 0,
                analysis.getAvgPowerConsumption() != null ? analysis.getAvgPowerConsumption() : 0);
        String result = cozeService.isConfigured()
                ? cozeService.getEnergyIssueAnalysis(buildingId, timeRange, contextData)
                : claudeService.getEnergyIssueAnalysis(buildingId, timeRange, contextData);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "buildingId", buildingId,
                "timeRange", timeRange,
                "analysis", result
        )));
    }
}
