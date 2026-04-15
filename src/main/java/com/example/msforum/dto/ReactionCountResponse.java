package com.example.msforum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReactionCountResponse {
    private Long postId;
    private long likeCount;
    private long dislikeCount;
}
