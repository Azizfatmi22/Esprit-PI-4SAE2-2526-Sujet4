package com.example.mscourse.dto;

import com.example.mscourse.entities.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private Level level;
    private Double price;
    private Integer durationMinutes;
    private String status;
    private Long trainerId;
    private Integer enrolledStudents;
    private Double rating;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterDTO> chapters;
    private List<CourseAttachmentDTO> attachments;
    private Integer totalChapters;
    private Integer totalContentBlocks;
}