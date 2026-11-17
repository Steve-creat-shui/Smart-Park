package com.energy.repository;

import com.energy.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    List<Alert> findByStatusOrderByCreatedAtDesc(String status);

    List<Alert> findByLevelOrderByCreatedAtDesc(String level);

    List<Alert> findByLevelAndStatusOrderByCreatedAtDesc(String level, String status);

    Optional<Alert> findByAlertCode(String alertCode);

    long countByStatus(String status);
}
