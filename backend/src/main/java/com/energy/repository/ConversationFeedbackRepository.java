package com.energy.repository;

import com.energy.entity.ConversationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback, Long> {
    List<ConversationFeedback> findByRatingOrderByCreatedAtDesc(int rating);
    List<ConversationFeedback> findAllByOrderByCreatedAtDesc();
    long countByRating(int rating);
}
