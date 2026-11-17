package com.energy.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "building_zones")
public class BuildingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false, length = 50)
    private String buildingId;

    @Column(name = "zone_name", nullable = false, length = 50)
    private String zoneName;

    @Column(name = "zone_type", length = 30)
    private String zoneType;

    @Column(name = "floor_num")
    private Integer floorNum;

    @Column(name = "capacity")
    private Integer capacity = 30;

    @Column(name = "has_camera")
    private Boolean hasCamera = false;

    @Column(name = "has_sensor")
    private Boolean hasSensor = true;
}
