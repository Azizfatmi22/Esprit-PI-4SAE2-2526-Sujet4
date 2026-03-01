package com.example.mscourse.dto;

import com.example.mscourse.entities.Level;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseRequestDTO {

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private Level level;

    @PositiveOrZero(message = "Price must be positive or zero")
    private Double price;

    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;

    private String status;

    private String thumbnailUrl;
}