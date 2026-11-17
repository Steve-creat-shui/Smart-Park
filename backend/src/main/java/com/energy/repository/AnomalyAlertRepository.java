package com.energy.repository;

import com.energy.entity.AnomalyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {

    List<AnomalyAlert> findByBuildingIdAndStatusOrderByCreatedAtDesc(String buildingId, String status);

    List<AnomalyAlert> findByBuildingIdOrderByCreatedAtDesc(String buildingId);

    List<AnomalyAlert> findByBuildingIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String buildingId, LocalDateTime start, LocalDateTime end);

    long countByBuildingIdAndStatus(String buildingId, String status);

    long countByBuildingIdAndAnomalyCategory(String buildingId, String category);

    @Query("SELECT a.anomalyType, COUNT(a) FROM AnomalyAlert a WHERE a.buildingId = ?1 " +
           "AND a.createdAt >= ?2 GROUP BY a.anomalyType")
    List<Object[]> countByTypesSince(String buildingId, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(a.estimatedWasteKwh), 0) FROM AnomalyAlert a " +
           "WHERE a.buildingId = ?1 AND a.createdAt >= ?2")
    Double totalWasteSince(String buildingId, LocalDateTime since);

    boolean existsByBuildingIdAndZoneIdAndAnomalyTypeAndStatusAndCreatedAtAfter(
            String buildingId, Long zoneId, String anomalyType, String status, LocalDateTime after);
}
