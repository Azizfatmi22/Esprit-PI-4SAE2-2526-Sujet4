package com.example.msforum.dto;

import com.example.msforum.entities.ContentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {
    private Long id;
    private Long postId;
    private String userId;
    private String content;
    private Boolean isBestAnswer;
    private LocalDateTime createdAt;
    private ContentStatus status;
    private Boolean reviewedByAdmin;
    private String sentiment;
}
