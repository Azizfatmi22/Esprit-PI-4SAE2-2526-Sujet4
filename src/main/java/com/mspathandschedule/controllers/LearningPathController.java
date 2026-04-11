package com.mspathandschedule.controllers;

import com.mspathandschedule.entities.LearningPath;
import com.mspathandschedule.services.impl.LearningPathServiceImpl;
import com.mspathandschedule.services.interfaces.LearningPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning-paths")
public class LearningPathController {

    private final LearningPathServiceImpl service;

    public LearningPathController(LearningPathServiceImpl service) {
        this.service = service;
    }

    // ==================== CRUD OPERATIONS ====================

    @PostMapping
    public ResponseEntity<LearningPath> create(@RequestBody LearningPath lp) {
        return ResponseEntity.ok(service.createLearningPath(lp));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningPath> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getLearningPath(id));
    }

    @GetMapping
    public ResponseEntity<List<LearningPath>> getAll() {
        return ResponseEntity.ok(service.getAllLearningPaths());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LearningPath> update(@PathVariable Long id, @RequestBody LearningPath lp) {
        return ResponseEntity.ok(service.updateLearningPath(id, lp));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteLearningPath(id);
        return ResponseEntity.ok().build();
    }

    // ==================== SESSION MANAGEMENT ====================

    @PostMapping("/{id}/sessions/{sessionId}")
    public ResponseEntity<LearningPath> addSession(@PathVariable Long id, @PathVariable Long sessionId) {
        return ResponseEntity.ok(service.addSessionToPath(id, sessionId));
    }

    @DeleteMapping("/{id}/sessions/{sessionId}")
    public ResponseEntity<LearningPath> removeSession(@PathVariable Long id, @PathVariable Long sessionId) {
        return ResponseEntity.ok(service.removeSessionFromPath(id, sessionId));
    }

    // ==================== ADVANCED ANALYTICS FUNCTIONS ====================

    /**
     * 1. Calculate Path Complexity
     * Returns complexity score, level, total sessions, total hours, and average hours per session
     */
    @GetMapping("/{id}/complexity")
    public ResponseEntity<Map<String, Object>> calculateComplexity(@PathVariable Long id) {
        return ResponseEntity.ok(service.calculatePathComplexity(id));
    }

    /**
     * 2. Predict Completion Rate
     * Returns predicted completion rate, risk level, total hours, and total sessions
     */
    @GetMapping("/{id}/completion-rate")
    public ResponseEntity<Map<String, Object>> predictCompletionRate(@PathVariable Long id) {
        return ResponseEntity.ok(service.predictCompletionRate(id));
    }

    /**
     * 3. Generate Learning Summary
     * Returns a formatted summary with path details, objectives, and key figures
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<String> generateSummary(@PathVariable Long id) {
        return ResponseEntity.ok(service.generateLearningSummary(id));
    }

    /**
     * 4. Get Optimal Learning Order
     * Returns sessions sorted by hours (easier sessions first)
     */
    @GetMapping("/{id}/optimal-order")
    public ResponseEntity<List<Map<String, Object>>> getOptimalOrder(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOptimalLearningOrder(id));
    }

    // ==================== COURSE FILTERING FUNCTIONS ====================

    /**
     * 5. Filter Courses by Level
     * Returns courses filtered by level (BEGINNER, INTERMEDIATE, ADVANCED)
     */
    @GetMapping("/courses/filter/by-level")
    public ResponseEntity<List<Map<String, Object>>> filterCoursesByLevel(@RequestParam String level) {
        return ResponseEntity.ok(service.filterCoursesByLevel(level));
    }

    /**
     * 6. Filter Courses by Description
     * Returns courses containing keyword in title or description
     */
    @GetMapping("/courses/filter/by-description")
    public ResponseEntity<List<Map<String, Object>>> filterCoursesByDescription(@RequestParam String keyword) {
        return ResponseEntity.ok(service.filterCoursesByDescription(keyword));
    }

    // ==================== LEGACY/EXISTING FUNCTIONS ====================

    /**
     * Calculate total hours (legacy - kept for backward compatibility)
     */
    @GetMapping("/{id}/hours")
    public ResponseEntity<Integer> calculateTotalHours(@PathVariable Long id) {
        // This method doesn't exist in your service yet, you may need to add it
        LearningPath lp = service.getLearningPath(id);
        int totalHours = lp.getTotalHours() != null ? lp.getTotalHours() : 0;
        return ResponseEntity.ok(totalHours);
    }

    /**
     * Analyze difficulty (legacy - kept for backward compatibility)
     */
    @GetMapping("/{id}/difficulty")
    public ResponseEntity<String> analyzeDifficulty(@PathVariable Long id) {
        Map<String, Object> complexity = service.calculatePathComplexity(id);
        String complexityLevel = (String) complexity.get("complexityLevel");

        String difficulty;
        switch (complexityLevel) {
            case "ÉLEVÉE":
                difficulty = "HARD";
                break;
            case "MOYENNE":
                difficulty = "MEDIUM";
                break;
            default:
                difficulty = "EASY";
        }
        return ResponseEntity.ok(difficulty);
    }

    /**
     * Detect risks (legacy - kept for backward compatibility)
     */
    @GetMapping("/{id}/risks")
    public ResponseEntity<List<String>> detectRisks(@PathVariable Long id) {
        List<String> risks = new java.util.ArrayList<>();
        Map<String, Object> completion = service.predictCompletionRate(id);

        String riskLevel = (String) completion.get("riskLevel");
        if ("ÉLEVÉ".equals(riskLevel)) {
            risks.add("High risk of learner dropout");
        }

        LearningPath lp = service.getLearningPath(id);
        if (lp.getSessionIds() == null || lp.getSessionIds().isEmpty()) {
            risks.add("No sessions assigned to this learning path");
        }

        if (lp.getStatus() != null && lp.getStatus().name().equals("INACTIVE")) {
            risks.add("Path is inactive");
        }

        return ResponseEntity.ok(risks);
    }
}