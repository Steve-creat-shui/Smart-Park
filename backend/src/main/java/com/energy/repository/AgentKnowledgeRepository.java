package com.energy.repository;

import com.energy.entity.AgentKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentKnowledgeRepository extends JpaRepository<AgentKnowledge, Long> {
    List<AgentKnowledge> findByCategory(String category);
    List<AgentKnowledge> findAllByOrderByCreatedAtDesc();
}
