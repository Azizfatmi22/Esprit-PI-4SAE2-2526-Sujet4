package com.example.msforum.dto;

import com.example.msforum.entities.ContentStatus;
import com.example.msforum.entities.PostCategory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostResponse {
    private Long id;
    private String userId;
    private Long formationId;
    private String title;
    private String content;
    private Long bestAnswerCommentId;
    private LocalDateTime createdAt;
    private ContentStatus status;
    private Boolean reviewedByAdmin;
    private PostCategory category;
    private String imageUrl;
}
