package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.Building;
import com.energy.entity.EnergyConsumption;
import com.energy.service.EnergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnergyConsumptionController {

    private final EnergyService energyService;

    @GetMapping("/energy-consumption/{building_id}")
    public ResponseEntity<ApiResponse<List<EnergyConsumption>>> getEnergyData(
            @PathVariable("building_id") String buildingId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        List<EnergyConsumption> data;
        if (start != null && end != null) {
            data = energyService.getEnergyDataByBuildingAndPeriod(buildingId, start, end);
        } else {
            data = energyService.getEnergyDataByBuilding(buildingId);
        }
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/energy-consumption/{building_id}/latest")
    public ResponseEntity<ApiResponse<List<EnergyConsumption>>> getLatestEnergyData(
            @PathVariable("building_id") String buildingId,
            @RequestParam(defaultValue = "24") int limit) {
        List<EnergyConsumption> data = energyService.getLatestEnergyData(buildingId, limit);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/energy-consumption/{building_id}/anomalies")
    public ResponseEntity<ApiResponse<List<EnergyConsumption>>> getAnomalies(
            @PathVariable("building_id") String buildingId) {
        List<EnergyConsumption> data = energyService.getAnomalies(buildingId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/energy-consumption")
    public ResponseEntity<ApiResponse<EnergyConsumption>> addEnergyData(
            @RequestBody EnergyConsumption data) {
        EnergyConsumption saved = energyService.saveEnergyData(data);
        return ResponseEntity.ok(ApiResponse.success("数据保存成功", saved));
    }

    @GetMapping("/buildings")
    public ResponseEntity<ApiResponse<List<Building>>> getAllBuildings() {
        return ResponseEntity.ok(ApiResponse.success(energyService.getAllBuildings()));
    }

    @GetMapping("/buildings/{building_id}")
    public ResponseEntity<ApiResponse<Building>> getBuildingById(
            @PathVariable("building_id") String buildingId) {
        return energyService.getBuildingById(buildingId)
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/buildings")
    public ResponseEntity<ApiResponse<Building>> addBuilding(@RequestBody Building building) {
        Building saved = energyService.saveBuilding(building);
        return ResponseEntity.ok(ApiResponse.success("建筑信息保存成功", saved));
    }

    @GetMapping("/buildings/ids")
    public ResponseEntity<ApiResponse<List<String>>> getAllBuildingIds() {
        return ResponseEntity.ok(ApiResponse.success(energyService.getAllBuildingIds()));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(energyService.getDashboardStats()));
    }

    @GetMapping("/energy-consumption/{building_id}/monthly-trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyTrend(
            @PathVariable("building_id") String buildingId) {
        return ResponseEntity.ok(ApiResponse.success(energyService.getMonthlyTrend(buildingId)));
    }
}
