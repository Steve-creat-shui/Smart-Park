package com.energy.repository;

import com.energy.entity.BuildingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingZoneRepository extends JpaRepository<BuildingZone, Long> {
    List<BuildingZone> findByBuildingId(String buildingId);
    long countByBuildingId(String buildingId);
}
