package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// This matches your Angular Chapter interface
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDTO {
    private Long id;
    private String title;
    private List<ContentBlockDTO> contentBlocks;
    private List<ChapterFileDTO> files;
    private List<ChapterImageDTO> images;
    private List<ChapterVideoDTO> videos;
}