package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This matches your Angular CourseAttachment interface
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseAttachmentDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String category; // 'syllabus' | 'prerequisites' | 'resources'
}