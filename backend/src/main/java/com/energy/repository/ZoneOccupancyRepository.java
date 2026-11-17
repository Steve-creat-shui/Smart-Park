package com.energy.repository;

import com.energy.entity.ZoneOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneOccupancyRepository extends JpaRepository<ZoneOccupancy, Long> {

    Optional<ZoneOccupancy> findTopByZoneIdOrderByRecordedAtDesc(Long zoneId);

    List<ZoneOccupancy> findByBuildingIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            String buildingId, LocalDateTime start, LocalDateTime end);

    List<ZoneOccupancy> findByZoneIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long zoneId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT z FROM ZoneOccupancy z WHERE z.buildingId = ?1 AND z.recordedAt = " +
           "(SELECT MAX(z2.recordedAt) FROM ZoneOccupancy z2 WHERE z2.zoneId = z.zoneId)")
    List<ZoneOccupancy> findLatestByBuildingId(String buildingId);

    @Query("SELECT COUNT(z) FROM ZoneOccupancy z WHERE z.zoneId = ?1 AND z.personCount = 0 " +
           "AND z.recordedAt >= ?2 ORDER BY z.recordedAt DESC")
    long countEmptySince(Long zoneId, LocalDateTime since);
}
