package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.EnterpriseApplication;
import com.energy.entity.Enterprise;
import com.energy.entity.User;
import com.energy.repository.UserRepository;
import com.energy.service.AdminService;
import com.energy.service.EnterpriseService;
import com.energy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final EnterpriseService enterpriseService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/users/pending")
    public ApiResponse<List<Map<String, Object>>> getPendingUsers() {
        return ApiResponse.success(adminService.getPendingUsers());
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.getAllUsers(status, role, keyword));
    }

    @PostMapping("/users/{id}/approve")
    public ApiResponse<String> approveUser(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getAdminId(request);
        adminService.approveUser(id, adminId);
        return ApiResponse.success("审批通过");
    }

    @PostMapping("/users/{id}/reject")
    public ApiResponse<String> rejectUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "未说明原因");
        adminService.rejectUser(id, reason);
        return ApiResponse.success("已拒绝");
    }

    @PostMapping("/users/{id}/disable")
    public ApiResponse<String> disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return ApiResponse.success("已禁用");
    }

    @PostMapping("/users/{id}/enable")
    public ApiResponse<String> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return ApiResponse.success("已启用");
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<String> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newRole = body.getOrDefault("role", "user");
        adminService.updateRole(id, newRole);
        return ApiResponse.success("角色已更新");
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<String> resetPassword(@PathVariable Long id) {
        adminService.resetPassword(id);
        return ApiResponse.success("密码已重置为 123456");
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    // ========== 企业注册申请审批 ==========

    @GetMapping("/enterprise-applications")
    public ApiResponse<List<EnterpriseApplication>> getApplications(@RequestParam(required = false) String status) {
        return ApiResponse.success(enterpriseService.listApplications(status));
    }

    @GetMapping("/enterprise-applications/pending-count")
    public ApiResponse<Long> pendingApplicationCount() {
        return ApiResponse.success(adminService.getApplications("pending").stream().count());
    }

    @PostMapping("/enterprise-applications/{id}/approve")
    public ApiResponse<EnterpriseApplication> approveApplication(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getAdminId(request);
        return ApiResponse.success(enterpriseService.approveApplication(id, adminId));
    }

    @PostMapping("/enterprise-applications/{id}/reject")
    public ApiResponse<EnterpriseApplication> rejectApplication(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long adminId = 0L;
        String reason = body.getOrDefault("reason", "未说明原因");
        return ApiResponse.success(enterpriseService.rejectApplication(id, adminId, reason));
    }

    // ========== 企业管理 ==========

    @GetMapping("/enterprises")
    public ApiResponse<List<Enterprise>> listEnterprises() {
        return ApiResponse.success(enterpriseService.listEnterprises());
    }

    private Long getAdminId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String phone = jwtUtil.getPhoneFromToken(token);
                return userRepository.findByPhone(phone).map(User::getId).orElse(0L);
            }
        } catch (Exception ignored) {}
        return 0L;
    }
}
