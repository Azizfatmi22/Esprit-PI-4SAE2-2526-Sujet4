package com.example.mscourse.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "learner_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"learner_id", "course_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnerProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "selected_chapter_index")
    private Integer selectedChapterIndex = 0;

    @Column(name = "selected_block_index")
    private Integer selectedBlockIndex = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "completed_blocks", joinColumns = @JoinColumn(name = "progress_id"))
    @Column(name = "block_id")
    private Set<Long> completedBlockIds = new HashSet<>();

    @Column(name = "total_lessons")
    private Integer totalLessons = 0;

    @Column(name = "completed_lessons")
    private Integer completedLessons = 0;

    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
    }

    public void calculateProgress() {
        if (totalLessons == null || totalLessons == 0) {
            progressPercent = 0;
            completedLessons = 0;
            isCompleted = false;
            return;
        }

        completedLessons = completedBlockIds != null ? completedBlockIds.size() : 0;
        progressPercent = Math.round((completedLessons * 100.0f) / totalLessons);
        isCompleted = completedLessons >= totalLessons;

        if (isCompleted && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
}
