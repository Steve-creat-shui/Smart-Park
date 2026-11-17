package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.Building;
import com.energy.entity.EnergyConsumption;
import com.energy.service.EnergyService;
import com.energy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnergyConsumptionController {

    private final EnergyService energyService;
    private final JwtUtil jwtUtil;

    private Long getEnterpriseId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String token = auth.substring(7);
                Long eid = jwtUtil.getEnterpriseIdFromToken(token);
                if (eid != null) return eid;
                // 个人用户: 用 -userId 作为虚拟企业ID
                Long uid = jwtUtil.getUserIdFromToken(token);
                if (uid != null) return -uid;
            } catch (Exception e) { }
        }
        return null;
    }

    @GetMapping("/energy-consumption/{building_id}")
    public ResponseEntity<ApiResponse<List<EnergyConsumption>>> getEnergyData(
            @PathVariable("building_id") String buildingId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            HttpServletRequest request) {
        Long eid = getEnterpriseId(request);
        List<EnergyConsumption> data;
        if (start != null && end != null) {
            data = energyService.getEnergyDataByEnterprisePeriod(eid, buildingId, start, end);
        } else {
            data = energyService.getEnergyDataByEnterprise(eid, buildingId);
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
    public ResponseEntity<ApiResponse<List<Building>>> getAllBuildings(HttpServletRequest request) {
        Long eid = getEnterpriseId(request);
        return ResponseEntity.ok(ApiResponse.success(energyService.getBuildingsByEnterprise(eid)));
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
    public ResponseEntity<ApiResponse<List<String>>> getAllBuildingIds(HttpServletRequest request) {
        Long eid = getEnterpriseId(request);
        return ResponseEntity.ok(ApiResponse.success(energyService.getBuildingIdsByEnterprise(eid)));
    }

    @GetMapping("/buildings/list")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBuildingList(HttpServletRequest request) {
        Long eid = getEnterpriseId(request);
        List<Map<String, String>> list = energyService.getBuildingsByEnterprise(eid).stream().map(b -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", b.getBuildingId());
            m.put("name", b.getBuildingName() != null ? b.getBuildingName() : b.getBuildingId());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(HttpServletRequest request) {
        Long eid = getEnterpriseId(request);
        return ResponseEntity.ok(ApiResponse.success(energyService.getDashboardStatsByEnterprise(eid)));
    }

    @GetMapping("/enterprise/has-data")
    public ResponseEntity<ApiResponse<Boolean>> enterpriseHasData(HttpServletRequest request) {
        Long eid = getEnterpriseId(request);
        return ResponseEntity.ok(ApiResponse.success(energyService.enterpriseHasData(eid)));
    }

    @GetMapping("/energy-consumption/{building_id}/available-dates")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableDates(
            @PathVariable("building_id") String buildingId) {
        return ResponseEntity.ok(ApiResponse.success(energyService.getAvailableDates(buildingId)));
    }

    @GetMapping("/energy-consumption/{building_id}/monthly-trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyTrend(
            @PathVariable("building_id") String buildingId) {
        return ResponseEntity.ok(ApiResponse.success(energyService.getMonthlyTrend(buildingId)));
    }
}
