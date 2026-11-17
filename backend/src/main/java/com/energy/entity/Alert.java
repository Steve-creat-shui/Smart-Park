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
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "alert_code", length = 50, unique = true, nullable = false)
    private String alertCode;

    @Column(name = "level", length = 20, nullable = false)
    private String level; // info, warning, error, critical

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "building_id")
    private Integer buildingId;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "enterprise_id")
    private Long enterpriseId;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "status", length = 20)
    private String status = "active"; // active, acknowledged, resolved

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Integer resolvedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String buildingName;

    @Transient
    private String deviceName;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        if (status == null) status = "active";
    }
}
