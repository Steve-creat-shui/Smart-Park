package com.energy.service;

import com.energy.entity.*;
import com.energy.repository.*;
import com.energy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepo;
    private final EnterpriseMemberRepository memberRepo;
    private final EnterpriseApplicationRepository applicationRepo;
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    // ==================== 企业注册申请 ====================

    @Transactional
    public EnterpriseApplication submitApplication(Long userId, EnterpriseApplication app) {
        if (applicationRepo.existsByApplicantIdAndStatus(userId, "pending")) {
            throw new RuntimeException("您已有一个待审核的企业注册申请");
        }
        app.setApplicantId(userId);
        app.setStatus("pending");
        app.setCreatedAt(LocalDateTime.now());
        EnterpriseApplication saved = applicationRepo.save(app);

        // 通知所有管理员
        userRepo.findAll().stream()
            .filter(u -> "platform_admin".equals(u.getUserType()))
            .forEach(admin -> {
                Notification n = new Notification();
                n.setUserId(admin.getId());
                n.setTitle("新企业注册申请");
                n.setContent(app.getEnterpriseName() + " 申请注册，请审核");
                n.setType("info");
                n.setRelatedType("application");
                n.setRelatedId(saved.getId());
                notificationRepo.save(n);
            });

        return saved;
    }

    @Transactional
    public EnterpriseApplication approveApplication(Long applicationId, Long adminId) {
        EnterpriseApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        if (!"pending".equals(app.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }

        // 创建企业
        Enterprise enterprise = new Enterprise();
        enterprise.setEnterpriseName(app.getEnterpriseName());
        enterprise.setEnterpriseCode(app.getEnterpriseCode());
        enterprise.setIndustry(app.getIndustry());
        enterprise.setContactName(app.getContactName());
        enterprise.setContactPhone(app.getContactPhone());
        enterprise.setContactEmail(app.getContactEmail());
        enterprise.setAddress(app.getAddress());
        enterprise.setDescription(app.getDescription());
        enterprise.setCreatedBy(app.getApplicantId());
        enterprise.setStatus("active");
        Enterprise saved = enterpriseRepo.save(enterprise);

        // 申请人成为企业所有者
        EnterpriseMember owner = new EnterpriseMember();
        owner.setEnterpriseId(saved.getId());
        owner.setUserId(app.getApplicantId());
        owner.setRole("owner");
        owner.setStatus("active");
        memberRepo.save(owner);

        // 升级用户为企业用户
        User applicant = userRepo.findById(app.getApplicantId()).orElse(null);
        if (applicant != null) {
            applicant.setUserType("enterprise");
            applicant.setRole("enterprise");
            applicant.setCurrentEnterpriseId(saved.getId());
            userRepo.save(applicant);
        }

        // 更新申请状态
        app.setStatus("approved");
        app.setReviewedBy(adminId);
        app.setReviewedAt(LocalDateTime.now());
        app.setEnterpriseId(saved.getId());
        applicationRepo.save(app);

        // 通知申请人
        Notification n = new Notification();
        n.setUserId(app.getApplicantId());
        n.setTitle("企业注册审核通过");
        n.setContent("恭喜！您的企业 " + app.getEnterpriseName() + " 已审核通过，请重新登录进入企业端。");
        n.setType("success");
        n.setRelatedType("application");
        n.setRelatedId(applicationId);
        notificationRepo.save(n);

        return app;
    }

    @Transactional
    public EnterpriseApplication rejectApplication(Long applicationId, Long adminId, String reason) {
        EnterpriseApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        if (!"pending".equals(app.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }
        app.setStatus("rejected");
        app.setReviewedBy(adminId);
        app.setReviewedAt(LocalDateTime.now());
        app.setRejectReason(reason);
        applicationRepo.save(app);

        // 通知申请人
        Notification n = new Notification();
        n.setUserId(app.getApplicantId());
        n.setTitle("企业注册申请被驳回");
        n.setContent("您的企业注册申请被驳回，原因：" + (reason != null ? reason : "未说明"));
        n.setType("error");
        n.setRelatedType("application");
        n.setRelatedId(applicationId);
        notificationRepo.save(n);

        return app;
    }

    public List<EnterpriseApplication> listApplications(String status) {
        List<EnterpriseApplication> list;
        if (status != null && !status.isEmpty()) {
            list = applicationRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = applicationRepo.findAllByOrderByCreatedAtDesc();
        }
        // enrich applicant info
        list.forEach(app -> {
            userRepo.findById(app.getApplicantId()).ifPresent(u -> {
                app.setApplicantName(u.getRealName() != null ? u.getRealName() : u.getNickname());
                app.setApplicantPhone(u.getPhone());
            });
        });
        return list;
    }

    // ==================== 企业管理 ====================

    public List<Enterprise> listEnterprises() {
        List<Enterprise> list = enterpriseRepo.findAllByOrderByCreatedAtDesc();
        list.forEach(this::enrichEnterprise);
        return list;
    }

    public Enterprise getEnterprise(Long id) {
        Enterprise e = enterpriseRepo.findById(id).orElseThrow(() -> new RuntimeException("企业不存在"));
        enrichEnterprise(e);
        return e;
    }

    private void enrichEnterprise(Enterprise e) {
        e.setMemberCount((int) memberRepo.countByEnterpriseIdAndStatus(e.getId(), "active"));
    }

    // ==================== 企业成员管理 ====================

    public List<EnterpriseMember> listMembers(Long enterpriseId) {
        List<EnterpriseMember> members = memberRepo.findByEnterpriseIdAndStatusOrderByJoinedAtAsc(enterpriseId, "active");
        members.forEach(m -> userRepo.findById(m.getUserId()).ifPresent(u -> {
            m.setUserName(u.getRealName() != null ? u.getRealName() : u.getNickname());
            m.setUserPhone(u.getPhone());
        }));
        return members;
    }

    @Transactional
    public EnterpriseMember addMember(Long enterpriseId, String phone, String role, Long invitedBy) {
        User user = userRepo.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在，请先注册"));
        if (memberRepo.existsByEnterpriseIdAndUserIdAndStatus(enterpriseId, user.getId(), "active")) {
            throw new RuntimeException("该用户已是企业成员");
        }
        EnterpriseMember m = new EnterpriseMember();
        m.setEnterpriseId(enterpriseId);
        m.setUserId(user.getId());
        m.setRole(role != null ? role : "member");
        m.setInvitedBy(invitedBy);
        m.setStatus("active");
        EnterpriseMember saved = memberRepo.save(m);

        // 升级用户为企业用户
        if ("normal".equals(user.getUserType())) {
            user.setUserType("enterprise");
            user.setRole("enterprise");
            user.setCurrentEnterpriseId(enterpriseId);
            userRepo.save(user);
        }

        // 通知被邀请人
        Enterprise ent = enterpriseRepo.findById(enterpriseId).orElse(null);
        Notification n = new Notification();
        n.setUserId(user.getId());
        n.setTitle("您已加入企业");
        n.setContent("您已被邀请加入企业 " + (ent != null ? ent.getEnterpriseName() : enterpriseId));
        n.setType("success");
        notificationRepo.save(n);

        return saved;
    }

    @Transactional
    public void removeMember(Long enterpriseId, Long memberId) {
        EnterpriseMember m = memberRepo.findById(memberId)
                .orElseThrow(() -> new RuntimeException("成员不存在"));
        if ("owner".equals(m.getRole())) {
            throw new RuntimeException("不能移除企业所有者");
        }
        m.setStatus("removed");
        memberRepo.save(m);
    }

    @Transactional
    public void updateMemberRole(Long memberId, String newRole) {
        EnterpriseMember m = memberRepo.findById(memberId)
                .orElseThrow(() -> new RuntimeException("成员不存在"));
        if ("owner".equals(m.getRole())) {
            throw new RuntimeException("不能修改所有者角色");
        }
        m.setRole(newRole);
        memberRepo.save(m);
    }

    // ==================== 用户企业列表 & 切换 ====================

    public List<Enterprise> getUserEnterprises(Long userId) {
        List<EnterpriseMember> memberships = memberRepo.findByUserIdAndStatusOrderByJoinedAtDesc(userId, "active");
        List<Enterprise> result = new ArrayList<>();
        memberships.forEach(m -> enterpriseRepo.findById(m.getEnterpriseId()).ifPresent(e -> {
            enrichEnterprise(e);
            result.add(e);
        }));
        return result;
    }

    @Transactional
    public User switchEnterprise(Long userId, Long enterpriseId) {
        if (!memberRepo.existsByEnterpriseIdAndUserIdAndStatus(enterpriseId, userId, "active")) {
            throw new RuntimeException("您不是该企业的成员");
        }
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setCurrentEnterpriseId(enterpriseId);
        return userRepo.save(user);
    }

    // ==================== 通知 ====================

    public List<Notification> getNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepo.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepo.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false).forEach(n -> {
            n.setIsRead(true);
            notificationRepo.save(n);
        });
    }
}
