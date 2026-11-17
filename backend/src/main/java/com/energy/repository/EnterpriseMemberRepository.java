package com.energy.repository;

import com.energy.entity.EnterpriseMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnterpriseMemberRepository extends JpaRepository<EnterpriseMember, Long> {

    List<EnterpriseMember> findByEnterpriseIdAndStatusOrderByJoinedAtAsc(Long enterpriseId, String status);

    List<EnterpriseMember> findByUserIdAndStatusOrderByJoinedAtDesc(Long userId, String status);

    Optional<EnterpriseMember> findByEnterpriseIdAndUserId(Long enterpriseId, Long userId);

    boolean existsByEnterpriseIdAndUserIdAndStatus(Long enterpriseId, Long userId, String status);

    long countByEnterpriseIdAndStatus(Long enterpriseId, String status);
}
