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
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", length = 50, unique = true, nullable = false)
    private String orderCode;

    @Column(name = "alert_id")
    private Integer alertId;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "building_id", nullable = false)
    private Integer buildingId;

    @Column(name = "fault_description", columnDefinition = "text", nullable = false)
    private String faultDescription;

    @Column(name = "ai_suggestion", columnDefinition = "text")
    private String aiSuggestion;

    @Column(name = "priority", length = 20)
    private String priority = "medium"; // low, medium, high, critical

    @Column(name = "status", length = 20)
    private String status = "pending"; // pending, processing, completed, cancelled

    @Column(name = "assigned_to")
    private Integer assignedTo;

    @Column(name = "created_by")
    private Integer createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Transient
    private String buildingName;

    @Transient
    private String deviceName;

    @Transient
    private String alertCode;

    @Transient
    private String assignedName;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "pending";
        if (priority == null) priority = "medium";
    }
}
