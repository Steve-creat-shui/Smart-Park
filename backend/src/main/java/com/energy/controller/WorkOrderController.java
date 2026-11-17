package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.WorkOrder;
import com.energy.service.AlertWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final AlertWorkOrderService service;

    @GetMapping
    public ApiResponse<List<WorkOrder>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ApiResponse.success(service.listWorkOrders(status, priority));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(service.getWorkOrderStats());
    }

    @PutMapping("/{id}/start")
    public ApiResponse<WorkOrder> start(@PathVariable Integer id) {
        return ApiResponse.success(service.startWorkOrder(id));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<WorkOrder> complete(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String notes = body.getOrDefault("resolutionNotes", "已处理完成");
        return ApiResponse.success(service.completeWorkOrder(id, notes));
    }

    @PutMapping("/{id}/assign")
    public ApiResponse<WorkOrder> assign(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer assignedTo = ((Number) body.get("assignedTo")).intValue();
        return ApiResponse.success(service.assignWorkOrder(id, assignedTo));
    }

    @GetMapping("/assignable-users")
    public ApiResponse<List<Map<String, Object>>> assignableUsers() {
        return ApiResponse.success(service.getAssignableUsers());
    }
}
