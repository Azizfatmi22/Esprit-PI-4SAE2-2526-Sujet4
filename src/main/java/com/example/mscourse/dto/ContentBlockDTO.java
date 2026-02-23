package com.example.mscourse.dto;

import com.example.mscourse.entities.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This matches your Angular ContentBlock interface
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlockDTO {
    private Long id;
    private ContentType type; // 'text' | 'image' | 'video' | 'resource' | 'pdf'
    private Integer order;
    private String data;
    private String title;
}