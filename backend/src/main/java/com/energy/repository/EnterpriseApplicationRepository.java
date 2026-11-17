package com.energy.repository;

import com.energy.entity.EnterpriseApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnterpriseApplicationRepository extends JpaRepository<EnterpriseApplication, Long> {

    List<EnterpriseApplication> findByStatusOrderByCreatedAtDesc(String status);

    List<EnterpriseApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    List<EnterpriseApplication> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    boolean existsByApplicantIdAndStatus(Long applicantId, String status);
}
