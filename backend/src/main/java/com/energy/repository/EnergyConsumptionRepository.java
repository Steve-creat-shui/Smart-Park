package com.energy.repository;

import com.energy.entity.EnergyConsumption;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EnergyConsumptionRepository extends JpaRepository<EnergyConsumption, Long> {

    List<EnergyConsumption> findByBuildingIdOrderByTimestampDesc(String buildingId);

    List<EnergyConsumption> findByBuildingIdAndTimestampBetweenOrderByTimestampAsc(
            String buildingId, LocalDateTime start, LocalDateTime end);

    List<EnergyConsumption> findByBuildingTypeOrderByTimestampDesc(String buildingType);

    List<EnergyConsumption> findByStatusOrderByTimestampDesc(String status);

    List<EnergyConsumption> findByBuildingIdOrderByTimestampDesc(String buildingId, Pageable pageable);

    @Query("SELECT SUM(e.powerConsumptionKwh) FROM EnergyConsumption e WHERE e.buildingId = :buildingId AND e.timestamp BETWEEN :start AND :end")
    Double sumPowerConsumptionByBuildingAndPeriod(@Param("buildingId") String buildingId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT SUM(e.carbonEmissionKg) FROM EnergyConsumption e WHERE e.buildingId = :buildingId AND e.timestamp BETWEEN :start AND :end")
    Double sumCarbonEmissionByBuildingAndPeriod(@Param("buildingId") String buildingId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Query(value = "SELECT building_id, SUM(power_consumption_kwh) as totalPower FROM energy_consumption " +
           "WHERE timestamp BETWEEN :start AND :end GROUP BY building_id ORDER BY totalPower DESC", nativeQuery = true)
    List<Object[]> findBuildingRankingByPower(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE_FORMAT(timestamp, '%Y-%m-%d') as day, SUM(power_consumption_kwh) as total " +
           "FROM energy_consumption WHERE building_id = :buildingId AND timestamp BETWEEN :start AND :end " +
           "GROUP BY day ORDER BY day ASC", nativeQuery = true)
    List<Object[]> findDailyPowerConsumption(@Param("buildingId") String buildingId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE_FORMAT(timestamp, '%Y-%m') as month, SUM(power_consumption_kwh) as totalPower, " +
           "SUM(water_consumption_m3) as totalWater, SUM(carbon_emission_kg) as totalCarbon, " +
           "SUM(ac_consumption_kwh) as totalAc " +
           "FROM energy_consumption WHERE building_id = :buildingId GROUP BY month ORDER BY month ASC", nativeQuery = true)
    List<Object[]> findMonthlyStatsByBuilding(@Param("buildingId") String buildingId);

    @Query("SELECT AVG(e.powerConsumptionKwh) FROM EnergyConsumption e WHERE e.buildingId = :buildingId")
    Double findAvgPowerConsumptionByBuilding(@Param("buildingId") String buildingId);

    @Query("SELECT e FROM EnergyConsumption e WHERE e.buildingId = :buildingId AND e.powerConsumptionKwh > " +
           "(SELECT AVG(e2.powerConsumptionKwh) * 1.5 FROM EnergyConsumption e2 WHERE e2.buildingId = :buildingId) " +
           "ORDER BY e.timestamp DESC")
    List<EnergyConsumption> findAnomaliesByBuilding(@Param("buildingId") String buildingId);

    @Query("SELECT DISTINCT e.buildingId FROM EnergyConsumption e ORDER BY e.buildingId")
    List<String> findAllBuildingIds();

    @Query(value = "SELECT DISTINCT DATE_FORMAT(timestamp, '%Y-%m-%d') FROM energy_consumption WHERE building_id = :buildingId ORDER BY 1", nativeQuery = true)
    List<String> findDistinctDatesByBuildingId(@Param("buildingId") String buildingId);

    // ==================== 企业数据隔离查询 ====================

    @Query("SELECT DISTINCT e.buildingId FROM EnergyConsumption e WHERE e.enterpriseId = :eid ORDER BY e.buildingId")
    List<String> findBuildingIdsByEnterpriseId(@Param("eid") Long enterpriseId);

    List<EnergyConsumption> findByEnterpriseIdAndBuildingIdOrderByTimestampDesc(Long enterpriseId, String buildingId);

    List<EnergyConsumption> findByEnterpriseIdAndBuildingIdAndTimestampBetweenOrderByTimestampAsc(
            Long enterpriseId, String buildingId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(e) FROM EnergyConsumption e WHERE e.enterpriseId = :eid")
    long countByEnterpriseId(@Param("eid") Long enterpriseId);

    @Query(value = "SELECT building_id, SUM(power_consumption_kwh) as totalPower FROM energy_consumption " +
           "WHERE enterprise_id = :eid AND timestamp BETWEEN :start AND :end GROUP BY building_id ORDER BY totalPower DESC", nativeQuery = true)
    List<Object[]> findBuildingRankingByPowerAndEnterprise(@Param("eid") Long enterpriseId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(e.powerConsumptionKwh) FROM EnergyConsumption e WHERE e.enterpriseId = :eid AND e.buildingId = :buildingId AND e.timestamp BETWEEN :start AND :end")
    Double sumPowerByEnterpriseBuildingPeriod(@Param("eid") Long enterpriseId, @Param("buildingId") String buildingId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(e.carbonEmissionKg) FROM EnergyConsumption e WHERE e.enterpriseId = :eid AND e.buildingId = :buildingId AND e.timestamp BETWEEN :start AND :end")
    Double sumCarbonByEnterpriseBuildingPeriod(@Param("eid") Long enterpriseId, @Param("buildingId") String buildingId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
