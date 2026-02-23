package com.example.mscourse.dto;

import com.example.mscourse.entities.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private Level level;
    private Double price;
    private String duration;
    private String status;
    private List<ChapterDTO> chapters;
    private Long trainerId;
    private Integer enrolledStudents;
    private Double rating;
    private String thumbnail;
    private List<CourseAttachmentDTO> attachments;
}