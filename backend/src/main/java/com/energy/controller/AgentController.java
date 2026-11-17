package com.energy.controller;

import com.energy.dto.ApiResponse;
import com.energy.entity.AgentKnowledge;
import com.energy.entity.ConversationFeedback;
import com.energy.repository.AgentKnowledgeRepository;
import com.energy.repository.ConversationFeedbackRepository;
import com.energy.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentKnowledgeRepository knowledgeRepo;
    private final ConversationFeedbackRepository feedbackRepo;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> query(@RequestBody Map<String, Object> body) {
        String buildingId = (String) body.getOrDefault("buildingId", "B001");
        String query = (String) body.getOrDefault("query", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) body.get("history");
        Map<String, Object> result = agentService.agentQuery(buildingId, query, history);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/knowledge")
    public ResponseEntity<ApiResponse<List<AgentKnowledge>>> getKnowledge() {
        return ResponseEntity.ok(ApiResponse.success(knowledgeRepo.findAllByOrderByCreatedAtDesc()));
    }

    @PostMapping("/knowledge")
    public ResponseEntity<ApiResponse<AgentKnowledge>> addKnowledge(@RequestBody AgentKnowledge k) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeRepo.save(k)));
    }

    @PutMapping("/knowledge/{id}")
    public ResponseEntity<ApiResponse<AgentKnowledge>> updateKnowledge(
            @PathVariable Long id, @RequestBody AgentKnowledge k) {
        k.setId(id);
        return ResponseEntity.ok(ApiResponse.success(knowledgeRepo.save(k)));
    }

    @DeleteMapping("/knowledge/{id}")
    public ResponseEntity<ApiResponse<String>> deleteKnowledge(@PathVariable Long id) {
        knowledgeRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("deleted"));
    }

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<ConversationFeedback>> saveFeedback(
            @RequestBody ConversationFeedback feedback) {
        return ResponseEntity.ok(ApiResponse.success(feedbackRepo.save(feedback)));
    }

    @GetMapping("/feedback/examples")
    public ResponseEntity<ApiResponse<List<ConversationFeedback>>> getExamples() {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackRepo.findByRatingOrderByCreatedAtDesc(1)));
    }

    @GetMapping("/feedback/all")
    public ResponseEntity<ApiResponse<List<ConversationFeedback>>> getAllFeedback() {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackRepo.findAllByOrderByCreatedAtDesc()));
    }

    @DeleteMapping("/feedback/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFeedback(@PathVariable Long id) {
        feedbackRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("deleted"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long total = feedbackRepo.count();
        long positive = feedbackRepo.countByRating(1);
        long negative = feedbackRepo.countByRating(-1);
        long knowledge = knowledgeRepo.count();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalFeedback", total,
                "positiveFeedback", positive,
                "negativeFeedback", negative,
                "knowledgeCount", knowledge
        )));
    }
}
