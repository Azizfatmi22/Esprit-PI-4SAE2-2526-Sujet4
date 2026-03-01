package com.example.mscourse.dto;

import com.example.mscourse.entities.AttachmentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttachmentRequestDTO {

    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private AttachmentCategory category;
    private String description;
}