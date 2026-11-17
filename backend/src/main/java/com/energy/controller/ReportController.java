package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.dto.EnergyAnalysisRequest;
import com.energy.dto.EnergyAnalysisResponse;
import com.energy.service.EnergyService;
import com.energy.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final EnergyService energyService;
    private final ReportService reportService;

    @GetMapping("/report/{building_id}")
    public ResponseEntity<ApiResponse<EnergyAnalysisResponse>> getReport(
            @PathVariable("building_id") String buildingId,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        EnergyAnalysisRequest request = new EnergyAnalysisRequest();
        request.setBuildingId(buildingId);
        request.setStartTime(start != null ? LocalDateTime.parse(start, fmt) : LocalDateTime.now().minusMonths(1));
        request.setEndTime(end != null ? LocalDateTime.parse(end, fmt) : LocalDateTime.now());
        request.setAnalysisType("monthly_report");
        request.setUserQuery(String.format("请为建筑%s生成完整的月度能效分析报告。", buildingId));
        EnergyAnalysisResponse result = energyService.analyzeEnergy(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/report/{building_id}/export")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable("building_id") String buildingId) throws IOException {
        byte[] excelData = reportService.generateExcelReport(buildingId);
        String filename = String.format("energy_report_%s_%s.xlsx",
                buildingId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }
}
