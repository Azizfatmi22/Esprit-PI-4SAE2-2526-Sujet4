package com.example.mscourse.dto;

import com.example.mscourse.entities.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseRequestDTO {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private Level level;

    @Min(value = 0, message = "Price cannot be negative")
    @Max(value = 99999, message = "Price cannot exceed 99999")
    private Double price;

    private String status;

    private String thumbnail;
}