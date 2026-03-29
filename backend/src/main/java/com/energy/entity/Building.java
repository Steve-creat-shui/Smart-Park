package com.energy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "buildings")
public class Building {

    @Id
    @Column(name = "building_id", length = 50)
    private String buildingId;

    @Column(name = "building_name", length = 100)
    private String buildingName;

    @Column(name = "building_type", length = 50)
    private String buildingType;

    @Column(name = "floor_count")
    private Integer floorCount;

    @Column(name = "total_area_m2")
    private Double totalAreaM2;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "energy_rating", length = 10)
    private String energyRating;

    @Column(name = "ac_system_type", length = 50)
    private String acSystemType;

    @Column(name = "status", length = 20)
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "active";
    }
}
