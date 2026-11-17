package com.energy.repository;

import com.energy.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {

    List<Enterprise> findByStatusOrderByCreatedAtDesc(String status);

    List<Enterprise> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
