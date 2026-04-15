package com.example.msforum.backoffice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventParticipationDTO {
    private String eventName;
    private int currentParticipants;
    private int maxParticipants;
}
