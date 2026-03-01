package com.example.mscourse.dto;

import com.example.mscourse.entities.AttachmentCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttachmentRequestDTO {

    @NotNull(message = "File name is required")
    private String fileName;

    private String fileType;

    private Long fileSize;

    @NotNull(message = "File URL is required")
    private String fileUrl;

    @NotNull(message = "Category is required")
    private AttachmentCategory category;

    private String description;
}