package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.AnomalyAlert;
import com.energy.service.MultiModalAnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/multimodal")
@RequiredArgsConstructor
public class OccupancyController {

    private final MultiModalAnomalyService service;

    @GetMapping("/dashboard/{buildingId}")
    public ApiResponse<Map<String, Object>> getDashboard(@PathVariable String buildingId) {
        return ApiResponse.success(service.getDashboardData(buildingId));
    }

    @GetMapping("/zones/{buildingId}")
    public ApiResponse<List<Map<String, Object>>> getZoneStatus(@PathVariable String buildingId) {
        return ApiResponse.success(service.getZoneStatus(buildingId));
    }

    @GetMapping("/occupancy-history/{zoneId}")
    public ApiResponse<List<Map<String, Object>>> getOccupancyHistory(
            @PathVariable Long zoneId,
            @RequestParam(defaultValue = "24") int hours) {
        return ApiResponse.success(service.getOccupancyHistory(zoneId, hours));
    }

    @GetMapping("/alerts/{buildingId}")
    public ApiResponse<List<AnomalyAlert>> getAlerts(
            @PathVariable String buildingId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(service.getAlerts(buildingId, status));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ApiResponse<String> acknowledgeAlert(@PathVariable Long id) {
        service.acknowledgeAlert(id);
        return ApiResponse.success("已确认");
    }

    @PostMapping("/alerts/{id}/resolve")
    public ApiResponse<String> resolveAlert(@PathVariable Long id) {
        service.resolveAlert(id);
        return ApiResponse.success("已解决");
    }

    @PostMapping("/analyze/{buildingId}")
    public ApiResponse<String> triggerAnalysis(@PathVariable String buildingId) {
        service.analyzeBuilding(buildingId);
        return ApiResponse.success("分析完成");
    }
}
