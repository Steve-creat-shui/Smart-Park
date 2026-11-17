package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.Alert;
import com.energy.service.AlertWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertWorkOrderService service;

    @GetMapping
    public ApiResponse<List<Alert>> list(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(service.listAlerts(level, status));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(service.getAlertStats());
    }

    @PostMapping
    public ApiResponse<Alert> create(@RequestBody Map<String, Object> body) {
        String level = (String) body.getOrDefault("level", "warning");
        String type = (String) body.get("type");
        Integer buildingId = body.get("buildingId") != null ? ((Number) body.get("buildingId")).intValue() : null;
        Integer deviceId = body.get("deviceId") != null ? ((Number) body.get("deviceId")).intValue() : null;
        String message = (String) body.get("message");
        return ApiResponse.success(service.createAlert(level, type, buildingId, deviceId, message));
    }

    @PutMapping("/{id}/acknowledge")
    public ApiResponse<Alert> acknowledge(@PathVariable Integer id) {
        return ApiResponse.success(service.acknowledgeAlert(id));
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<Alert> resolve(@PathVariable Integer id) {
        return ApiResponse.success(service.resolveAlert(id));
    }
}
