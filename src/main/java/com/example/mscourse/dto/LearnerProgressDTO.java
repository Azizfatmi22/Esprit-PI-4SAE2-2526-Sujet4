package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnerProgressDTO {
    private Long id;
    private String learnerId;
    private Long courseId;
    private Integer selectedChapterIndex;
    private Integer selectedBlockIndex;
    private Set<Long> completedBlockIds;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer progressPercent;
    private Boolean isCompleted;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
