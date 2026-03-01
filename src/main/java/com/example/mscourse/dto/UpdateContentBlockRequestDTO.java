package com.example.mscourse.dto;

import com.example.mscourse.entities.ContentType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentBlockRequestDTO {

    private ContentType type;

    private Integer orderIndex;

    private String data;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;
}