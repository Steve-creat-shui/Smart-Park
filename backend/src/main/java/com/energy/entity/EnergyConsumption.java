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
@Table(name = "energy_consumption", indexes = {
    @Index(name = "idx_building_id", columnList = "building_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class EnergyConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false, length = 50)
    private String buildingId;

    @Column(name = "building_type", length = 50)
    private String buildingType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "power_consumption_kwh")
    private Double powerConsumptionKwh;

    @Column(name = "water_consumption_m3")
    private Double waterConsumptionM3;

    @Column(name = "carbon_emission_kg")
    private Double carbonEmissionKg;

    @Column(name = "ac_consumption_kwh")
    private Double acConsumptionKwh;

    @Column(name = "lighting_consumption_kwh")
    private Double lightingConsumptionKwh;

    @Column(name = "other_consumption_kwh")
    private Double otherConsumptionKwh;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "humidity_percent")
    private Double humidityPercent;

    @Column(name = "occupancy_rate")
    private Double occupancyRate;

    @Column(name = "status", length = 20)
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "normal";
    }
}
