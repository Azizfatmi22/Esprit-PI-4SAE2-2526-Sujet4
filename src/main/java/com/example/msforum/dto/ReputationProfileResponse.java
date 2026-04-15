package com.example.msforum.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReputationProfileResponse {
    private String userId;
    private Integer points;
    private String level;
    private Integer rank;
    private Integer postsCount;
    private Integer commentsCount;
    private Integer likesReceivedCount;
    private Integer bestAnswersCount;
    private List<ReputationEventResponse> recentEvents;
}
