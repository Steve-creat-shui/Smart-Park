package com.energy.repository;

import com.energy.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, String> {
    List<Building> findByBuildingType(String buildingType);
    List<Building> findByStatus(String status);
    List<Building> findByEnterpriseId(Long enterpriseId);
    long countByEnterpriseId(Long enterpriseId);

    @Query("SELECT b FROM Building b WHERE b.enterpriseId IS NULL OR b.enterpriseId = 1")
    List<Building> findDemoBuildings();

    @Query("SELECT COUNT(b) FROM Building b WHERE b.enterpriseId IS NULL OR b.enterpriseId = 1")
    long countDemoBuildings();
}
