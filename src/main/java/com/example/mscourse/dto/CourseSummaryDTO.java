package com.example.mscourse.dto;

import com.example.mscourse.entities.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryDTO {
    private Long id;
    private String title;
    private String description;
    private Level level;
    private Double price;
    private String status;
    private String trainerId;
    private Integer enrolledStudents;
    private Double rating;
    private String thumbnailUrl;
    private Integer totalChapters;
    private Integer totalDurationMinutes;
}