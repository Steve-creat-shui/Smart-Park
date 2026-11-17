package com.energy.repository;

import com.energy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    List<User> findByStatusOrderByCreatedAtDesc(String status);
    List<User> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
    long countByRole(String role);

    @Query("SELECT u FROM User u WHERE u.realName LIKE %?1% OR u.phone LIKE %?1% OR u.employeeId LIKE %?1%")
    List<User> searchUsers(String keyword);
}
