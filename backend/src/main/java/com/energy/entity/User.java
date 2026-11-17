package com.energy.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", unique = true, nullable = false, length = 11)
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "engineer_cert", length = 100)
    private String engineerCert;

    @Column(name = "company_email", length = 100)
    private String companyEmail;

    @Column(name = "id_card_last4", length = 4)
    private String idCardLast4;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "role", length = 20)
    private String role = "user";

    @Column(name = "user_type", length = 20)
    private String userType = "normal"; // normal, enterprise, platform_admin

    @Column(name = "current_enterprise_id")
    private Long currentEnterpriseId;

    @Column(name = "status", length = 20)
    private String status = "active";

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
