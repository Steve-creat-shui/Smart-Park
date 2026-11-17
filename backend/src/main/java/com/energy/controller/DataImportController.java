package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.ImportHistory;
import com.energy.entity.User;
import com.energy.repository.UserRepository;
import com.energy.service.DataImportService;
import com.energy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<Map<String, Object>>> preview(@RequestParam("file") MultipartFile file) {
        try {
            return ApiResponse.success(dataImportService.previewFile(file));
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private Long resolveEnterpriseId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long eid = jwtUtil.getEnterpriseIdFromToken(token);
            if (eid != null) return eid;
            // 个人用户: 用 -userId 作为虚拟企业ID
            Long uid = jwtUtil.getUserIdFromToken(token);
            if (uid != null) return -uid;
        }
        return null;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> importData(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            Long enterpriseId = resolveEnterpriseId(request);
            Long userId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                userId = jwtUtil.getUserIdFromToken(token);
            }
            return ApiResponse.success(dataImportService.importFileForEnterprise(file, enterpriseId, userId));
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/import-history")
    public ApiResponse<List<ImportHistory>> importHistory(HttpServletRequest request) {
        Long enterpriseId = resolveEnterpriseId(request);
        return ApiResponse.success(dataImportService.getImportHistory(enterpriseId));
    }
}
