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
@Table(name = "anomaly_alerts", indexes = {
    @Index(name = "idx_alert_building_time", columnList = "building_id, created_at")
})
public class AnomalyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false, length = 50)
    private String buildingId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "zone_name", length = 50)
    private String zoneName;

    @Column(name = "anomaly_category", nullable = false, length = 30)
    private String anomalyCategory;

    @Column(name = "anomaly_type", nullable = false, length = 50)
    private String anomalyType;

    @Column(name = "severity", nullable = false, length = 10)
    private String severity;

    @Column(name = "energy_value")
    private Double energyValue;

    @Column(name = "person_count")
    private Integer personCount;

    @Column(name = "is_work_hour")
    private Boolean isWorkHour;

    @Column(name = "evidence_summary", length = 500)
    private String evidenceSummary;

    @Column(name = "estimated_waste_kwh")
    private Double estimatedWasteKwh;

    @Column(name = "estimated_cost")
    private Double estimatedCost;

    @Column(name = "status", length = 20)
    private String status = "active";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
