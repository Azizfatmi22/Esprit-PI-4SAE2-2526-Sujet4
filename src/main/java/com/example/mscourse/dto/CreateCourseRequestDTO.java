package com.example.mscourse.dto;

import com.example.mscourse.entities.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Level is required")
    private Level level;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    @Max(value = 99999, message = "Price cannot exceed 99999")
    private Double price;

    private Long trainerId; // Made optional for frontend (it sets it to 1L in service)
    private Integer duration; // Duration in seconds

    private List<ChapterDTO> chapters; // Include chapters with their content
    private String thumbnail;
}