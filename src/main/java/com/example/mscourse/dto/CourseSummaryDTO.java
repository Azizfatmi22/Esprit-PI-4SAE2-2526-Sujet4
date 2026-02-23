package com.example.mscourse.dto;

import com.example.mscourse.entities.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryDTO {
    private Long id;
    private String title;
    private Level level;
    private Double price;
    private String status;
    private String thumbnail;
    private Double rating;
    private Integer enrolledStudents;
}