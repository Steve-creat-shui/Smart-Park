package com.energy.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enterprise_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"enterprise_id", "user_id"})
})
public class EnterpriseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enterprise_id", nullable = false)
    private Long enterpriseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role = "member"; // owner, admin, member

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "active"; // active, removed

    @Transient
    private String userName;

    @Transient
    private String userPhone;

    @Transient
    private String enterpriseName;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
        if (status == null) status = "active";
    }
}
