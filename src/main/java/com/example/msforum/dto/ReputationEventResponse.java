package com.example.msforum.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReputationEventResponse {
    private String actionType;
    private Integer pointsDelta;
    private String sourceType;
    private String sourceId;
    private LocalDateTime createdAt;
}
