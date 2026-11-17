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
@Table(name = "zone_occupancy", indexes = {
    @Index(name = "idx_zone_time", columnList = "zone_id, recorded_at")
})
public class ZoneOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "building_id", length = 50)
    private String buildingId;

    @Column(name = "person_count")
    private Integer personCount = 0;

    @Column(name = "is_occupied")
    private Boolean isOccupied = false;

    @Column(name = "detection_method", length = 20)
    private String detectionMethod = "simulated";

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
