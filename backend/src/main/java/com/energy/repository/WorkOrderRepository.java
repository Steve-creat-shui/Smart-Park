package com.energy.repository;

import com.energy.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer> {

    List<WorkOrder> findAllByOrderByCreatedAtDesc();

    List<WorkOrder> findByStatusOrderByCreatedAtDesc(String status);

    List<WorkOrder> findByPriorityOrderByCreatedAtDesc(String priority);

    List<WorkOrder> findByStatusAndPriorityOrderByCreatedAtDesc(String status, String priority);

    Optional<WorkOrder> findByOrderCode(String orderCode);

    Optional<WorkOrder> findByAlertId(Integer alertId);

    long countByStatus(String status);
}
