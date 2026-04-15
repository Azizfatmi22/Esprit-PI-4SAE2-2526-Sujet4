package com.example.msforum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaderboardEntryResponse {
    private Integer rank;
    private String userId;
    private Integer points;
    private String level;
}
