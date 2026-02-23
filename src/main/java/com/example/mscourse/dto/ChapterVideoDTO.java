package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This matches your Angular ChapterVideo interface
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterVideoDTO {
    private Long id;
    private String name;
    private String url;
    private Long size;
    private String type;
    private Integer duration;
}