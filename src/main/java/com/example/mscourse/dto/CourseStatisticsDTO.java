package com.example.mscourse.dto;

import com.example.mscourse.entities.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatisticsDTO {
    private Long trainerId;
    private Long totalCourses;
    private Long publishedCourses;
    private Long draftCourses;
    private Long archivedCourses;
    private Long totalEnrollments;
    private Double averageRating;
    private Map<Level, Long> coursesByLevel;
    private Map<String, Long> coursesByStatus;
    private Long totalChapters;
    private Long totalContentBlocks;
    private Long totalAttachments;
    private Long totalAttachmentsSize;
}