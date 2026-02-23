package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This matches your Angular ChapterFile interface
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterFileDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String description;
}