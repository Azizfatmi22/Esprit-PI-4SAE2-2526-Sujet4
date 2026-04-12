package com.example.mscourse.controller;

import com.example.mscourse.dto.LearnerProgressDTO;
import com.example.mscourse.dto.UpdateProgressRequest;
import com.example.mscourse.entities.LearnerProgress;
import com.example.mscourse.repositories.LearnerProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress")
public class LearnerProgressController {

    private static final Logger log = LoggerFactory.getLogger(LearnerProgressController.class);

    @Autowired
    private LearnerProgressRepository progressRepository;

    /**
     * Get progress for a specific learner and course
     */
    @GetMapping("/learner/{learnerId}/course/{courseId}")
    public ResponseEntity<LearnerProgressDTO> getProgress(
            @PathVariable String learnerId,
            @PathVariable Long courseId) {
        
        log.info("Getting progress for learner {} and course {}", learnerId, courseId);
        
        LearnerProgress progress = progressRepository
                .findByLearnerIdAndCourseId(learnerId, courseId)
                .orElse(null);

        if (progress == null) {
            // Return empty progress if not found
            LearnerProgressDTO emptyProgress = new LearnerProgressDTO();
            emptyProgress.setLearnerId(learnerId);
            emptyProgress.setCourseId(courseId);
            emptyProgress.setSelectedChapterIndex(0);
            emptyProgress.setSelectedBlockIndex(0);
            emptyProgress.setCompletedBlockIds(new HashSet<>());
            emptyProgress.setTotalLessons(0);
            emptyProgress.setCompletedLessons(0);
            emptyProgress.setProgressPercent(0);
            emptyProgress.setIsCompleted(false);
            return ResponseEntity.ok(emptyProgress);
        }

        return ResponseEntity.ok(toDTO(progress));
    }

    /**
     * Get all progress for a learner
     */
    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<List<LearnerProgressDTO>> getLearnerProgress(@PathVariable String learnerId) {
        log.info("Getting all progress for learner {}", learnerId);
        
        List<LearnerProgress> progressList = progressRepository.findByLearnerId(learnerId);
        List<LearnerProgressDTO> dtoList = progressList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Get progress statistics for a learner
     */
    @GetMapping("/learner/{learnerId}/stats")
    public ResponseEntity<Map<String, Object>> getLearnerStats(@PathVariable String learnerId) {
        log.info("Getting progress stats for learner {}", learnerId);
        
        List<LearnerProgress> allProgress = progressRepository.findByLearnerId(learnerId);
        Long completedCount = progressRepository.countByLearnerIdAndIsCompleted(learnerId, true);
        
        int totalCourses = allProgress.size();
        int inProgressCount = (int) allProgress.stream()
                .filter(p -> !p.getIsCompleted() && p.getCompletedLessons() > 0)
                .count();
        
        int totalLessonsCompleted = allProgress.stream()
                .mapToInt(p -> p.getCompletedLessons() != null ? p.getCompletedLessons() : 0)
                .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCourses", totalCourses);
        stats.put("completedCourses", completedCount);
        stats.put("inProgressCourses", inProgressCount);
        stats.put("notStartedCourses", totalCourses - completedCount - inProgressCount);
        stats.put("totalLessonsCompleted", totalLessonsCompleted);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Update or create progress for a learner
     */
    @PutMapping("/learner/{learnerId}/course/{courseId}")
    public ResponseEntity<LearnerProgressDTO> updateProgress(
            @PathVariable String learnerId,
            @PathVariable Long courseId,
            @RequestBody UpdateProgressRequest request) {
        
        log.info("Updating progress for learner {} and course {}", learnerId, courseId);
        
        LearnerProgress progress = progressRepository
                .findByLearnerIdAndCourseId(learnerId, courseId)
                .orElse(new LearnerProgress());

        // Set basic info
        progress.setLearnerId(learnerId);
        progress.setCourseId(courseId);

        // Update position
        if (request.getSelectedChapterIndex() != null) {
            progress.setSelectedChapterIndex(request.getSelectedChapterIndex());
        }
        if (request.getSelectedBlockIndex() != null) {
            progress.setSelectedBlockIndex(request.getSelectedBlockIndex());
        }

        // Update completed blocks
        if (request.getCompletedBlockIds() != null) {
            progress.setCompletedBlockIds(new HashSet<>(request.getCompletedBlockIds()));
        }

        // Update total lessons
        if (request.getTotalLessons() != null) {
            progress.setTotalLessons(request.getTotalLessons());
        }

        // Calculate progress
        progress.calculateProgress();

        // Save
        LearnerProgress saved = progressRepository.save(progress);
        
        log.info("Progress updated: {}% complete ({}/{})", 
                saved.getProgressPercent(), 
                saved.getCompletedLessons(), 
                saved.getTotalLessons());

        return ResponseEntity.ok(toDTO(saved));
    }

    /**
     * Mark a specific lesson as completed
     */
    @PostMapping("/learner/{learnerId}/course/{courseId}/complete-lesson/{blockId}")
    public ResponseEntity<LearnerProgressDTO> markLessonComplete(
            @PathVariable String learnerId,
            @PathVariable Long courseId,
            @PathVariable Long blockId,
            @RequestParam(required = false) Integer totalLessons) {
        
        log.info("Marking lesson {} as complete for learner {} in course {}", blockId, learnerId, courseId);
        
        LearnerProgress progress = progressRepository
                .findByLearnerIdAndCourseId(learnerId, courseId)
                .orElse(new LearnerProgress());

        progress.setLearnerId(learnerId);
        progress.setCourseId(courseId);

        if (progress.getCompletedBlockIds() == null) {
            progress.setCompletedBlockIds(new HashSet<>());
        }

        progress.getCompletedBlockIds().add(blockId);

        if (totalLessons != null) {
            progress.setTotalLessons(totalLessons);
        }

        progress.calculateProgress();

        LearnerProgress saved = progressRepository.save(progress);
        
        return ResponseEntity.ok(toDTO(saved));
    }

    /**
     * Reset progress for a course
     */
    @DeleteMapping("/learner/{learnerId}/course/{courseId}")
    public ResponseEntity<Map<String, String>> resetProgress(
            @PathVariable String learnerId,
            @PathVariable Long courseId) {
        
        log.info("Resetting progress for learner {} and course {}", learnerId, courseId);
        
        progressRepository.findByLearnerIdAndCourseId(learnerId, courseId)
                .ifPresent(progressRepository::delete);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Progress reset successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get all learners' progress for a specific course (for trainers/admins)
     */
    @GetMapping("/course/{courseId}/learners")
    public ResponseEntity<List<LearnerProgressDTO>> getCourseProgress(@PathVariable Long courseId) {
        log.info("Getting all learners' progress for course {}", courseId);
        
        List<LearnerProgress> progressList = progressRepository.findByCourseId(courseId);
        List<LearnerProgressDTO> dtoList = progressList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtoList);
    }

    private LearnerProgressDTO toDTO(LearnerProgress progress) {
        LearnerProgressDTO dto = new LearnerProgressDTO();
        dto.setId(progress.getId());
        dto.setLearnerId(progress.getLearnerId());
        dto.setCourseId(progress.getCourseId());
        dto.setSelectedChapterIndex(progress.getSelectedChapterIndex());
        dto.setSelectedBlockIndex(progress.getSelectedBlockIndex());
        dto.setCompletedBlockIds(progress.getCompletedBlockIds());
        dto.setTotalLessons(progress.getTotalLessons());
        dto.setCompletedLessons(progress.getCompletedLessons());
        dto.setProgressPercent(progress.getProgressPercent());
        dto.setIsCompleted(progress.getIsCompleted());
        dto.setLastAccessedAt(progress.getLastAccessedAt());
        dto.setCompletedAt(progress.getCompletedAt());
        dto.setCreatedAt(progress.getCreatedAt());
        dto.setUpdatedAt(progress.getUpdatedAt());
        return dto;
    }
}
