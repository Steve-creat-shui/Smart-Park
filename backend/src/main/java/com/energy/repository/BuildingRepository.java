package com.energy.repository;

import com.energy.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, String> {
    List<Building> findByBuildingType(String buildingType);
    List<Building> findByStatus(String status);
}
