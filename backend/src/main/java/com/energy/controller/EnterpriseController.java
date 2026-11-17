package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.*;
import com.energy.repository.UserRepository;
import com.energy.service.EnterpriseService;
import com.energy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    private Long getUserId(HttpServletRequest request) {
        String phone = (String) request.getAttribute("phone");
        return userRepo.findByPhone(phone).map(User::getId).orElse(null);
    }

    // ========== 企业注册申请 ==========

    @PostMapping("/apply")
    public ApiResponse<EnterpriseApplication> apply(@RequestBody EnterpriseApplication app, HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(enterpriseService.submitApplication(userId, app));
    }

    @GetMapping("/my-applications")
    public ApiResponse<List<EnterpriseApplication>> myApplications(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(enterpriseService.listApplications(null).stream()
                .filter(a -> a.getApplicantId().equals(userId))
                .collect(java.util.stream.Collectors.toList()));
    }

    // ========== 我的企业列表 & 切换 ==========

    @GetMapping("/my-enterprises")
    public ApiResponse<List<Enterprise>> myEnterprises(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(enterpriseService.getUserEnterprises(userId));
    }

    @PostMapping("/switch/{enterpriseId}")
    public ApiResponse<Map<String, Object>> switchEnterprise(@PathVariable Long enterpriseId, HttpServletRequest request) {
        Long userId = getUserId(request);
        User user = enterpriseService.switchEnterprise(userId, enterpriseId);
        // 重新生成token
        String token = jwtUtil.generateToken(user.getPhone(), user.getNickname(), user.getRole(), user.getUserType(), enterpriseId);
        Enterprise ent = enterpriseService.getEnterprise(enterpriseId);
        return ApiResponse.success(Map.of(
                "token", token,
                "currentEnterpriseId", enterpriseId,
                "enterpriseName", ent.getEnterpriseName()
        ));
    }

    // ========== 企业详情 ==========

    @GetMapping("/info")
    public ApiResponse<Enterprise> getEnterpriseInfo(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long enterpriseId = jwtUtil.getEnterpriseIdFromToken(token);
        if (enterpriseId == null) return ApiResponse.error("未选择企业");
        return ApiResponse.success(enterpriseService.getEnterprise(enterpriseId));
    }

    // ========== 企业成员管理 ==========

    @GetMapping("/members")
    public ApiResponse<List<EnterpriseMember>> members(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long enterpriseId = jwtUtil.getEnterpriseIdFromToken(token);
        if (enterpriseId == null) return ApiResponse.error("未选择企业");
        return ApiResponse.success(enterpriseService.listMembers(enterpriseId));
    }

    @PostMapping("/members/add")
    public ApiResponse<EnterpriseMember> addMember(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long enterpriseId = jwtUtil.getEnterpriseIdFromToken(token);
        Long userId = getUserId(request);
        String phone = body.get("phone");
        String role = body.getOrDefault("role", "member");
        return ApiResponse.success(enterpriseService.addMember(enterpriseId, phone, role, userId));
    }

    @DeleteMapping("/members/{memberId}")
    public ApiResponse<String> removeMember(@PathVariable Long memberId, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long enterpriseId = jwtUtil.getEnterpriseIdFromToken(token);
        enterpriseService.removeMember(enterpriseId, memberId);
        return ApiResponse.success("已移除");
    }

    @PutMapping("/members/{memberId}/role")
    public ApiResponse<String> updateRole(@PathVariable Long memberId, @RequestBody Map<String, String> body) {
        enterpriseService.updateMemberRole(memberId, body.get("role"));
        return ApiResponse.success("角色已更新");
    }

    // ========== 通知 ==========

    @GetMapping("/notifications")
    public ApiResponse<List<Notification>> notifications(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(enterpriseService.getNotifications(userId));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Long> unreadCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(enterpriseService.getUnreadCount(userId));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<String> markRead(@PathVariable Long id) {
        enterpriseService.markAsRead(id);
        return ApiResponse.success("已读");
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<String> markAllRead(HttpServletRequest request) {
        Long userId = getUserId(request);
        enterpriseService.markAllAsRead(userId);
        return ApiResponse.success("全部已读");
    }
}
