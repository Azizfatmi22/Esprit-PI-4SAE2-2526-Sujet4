package com.example.mscourse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {
    private Integer selectedChapterIndex;
    private Integer selectedBlockIndex;
    private Set<Long> completedBlockIds;
    private Integer totalLessons;
}
