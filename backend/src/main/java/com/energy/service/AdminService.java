package com.energy.service;

import com.energy.entity.User;
import com.energy.entity.EnterpriseApplication;
import com.energy.repository.UserRepository;
import com.energy.repository.EnterpriseRepository;
import com.energy.repository.EnterpriseApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseApplicationRepository applicationRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Map<String, Object>> getPendingUsers() {
        return userRepository.findByStatusOrderByCreatedAtDesc("pending")
                .stream().map(this::toUserMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllUsers(String status, String role, String keyword) {
        List<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.searchUsers(keyword.trim());
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }
        return users.stream()
                .filter(u -> status == null || status.isEmpty() || status.equals(u.getStatus()))
                .filter(u -> role == null || role.isEmpty() || role.equals(u.getRole()))
                .map(this::toUserMap)
                .collect(Collectors.toList());
    }

    public void approveUser(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"pending".equals(user.getStatus())) {
            throw new RuntimeException("该用户当前状态不是待审批");
        }
        user.setStatus("active");
        user.setApprovedBy(adminId);
        user.setApprovedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void rejectUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!"pending".equals(user.getStatus())) {
            throw new RuntimeException("该用户当前状态不是待审批");
        }
        user.setStatus("rejected");
        user.setRejectReason(reason);
        userRepository.save(user);
    }

    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("disabled");
        userRepository.save(user);
    }

    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("active");
        userRepository.save(user);
    }

    public void updateRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRole(newRole);
        userRepository.save(user);
    }

    public void resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode("123456"));
        userRepository.save(user);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("pendingCount", userRepository.countByStatus("pending"));
        stats.put("activeCount", userRepository.countByStatus("active"));
        stats.put("disabledCount", userRepository.countByStatus("disabled"));
        stats.put("rejectedCount", userRepository.countByStatus("rejected"));
        stats.put("adminCount", userRepository.countByRole("admin"));
        stats.put("enterpriseCount", userRepository.countByRole("enterprise"));
        stats.put("userCount", userRepository.countByRole("user"));

        // 部门分布
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> deptDist = allUsers.stream()
                .filter(u -> u.getDepartment() != null && !u.getDepartment().isEmpty())
                .collect(Collectors.groupingBy(User::getDepartment, Collectors.counting()));
        stats.put("departmentDistribution", deptDist);

        // 企业统计
        stats.put("totalEnterprises", enterpriseRepository.count());
        stats.put("activeEnterprises", enterpriseRepository.countByStatus("active"));
        stats.put("pendingApplications", applicationRepository.countByStatus("pending"));

        return stats;
    }

    public List<EnterpriseApplication> getApplications(String status) {
        if (status != null && !status.isEmpty()) {
            return applicationRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("phone", u.getPhone());
        m.put("nickname", u.getNickname());
        m.put("employeeId", u.getEmployeeId());
        m.put("realName", u.getRealName());
        m.put("department", u.getDepartment());
        m.put("position", u.getPosition());
        m.put("engineerCert", u.getEngineerCert());
        m.put("companyEmail", u.getCompanyEmail());
        m.put("remark", u.getRemark());
        m.put("role", u.getRole());
        m.put("userType", u.getUserType());
        m.put("currentEnterpriseId", u.getCurrentEnterpriseId());
        m.put("status", u.getStatus());
        m.put("rejectReason", u.getRejectReason());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        m.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
        m.put("approvedAt", u.getApprovedAt() != null ? u.getApprovedAt().toString() : null);
        return m;
    }
}
