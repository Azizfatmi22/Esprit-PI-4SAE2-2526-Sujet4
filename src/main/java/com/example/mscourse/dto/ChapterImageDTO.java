package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This matches your Angular ChapterImage interface
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterImageDTO {
    private Long id;
    private String name;
    private String url;
    private Long size;
    private String type;
}