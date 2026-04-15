package com.example.msforum.backoffice;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class EventAnalyticsDTO {
    private long totalEvents;
    private long upcomingEvents;
    private long ongoingEvents;
    private long finishedEvents;
    private double averageParticipationRate;
    private String mostPopularEventName;
    private List<EventParticipationDTO> eventParticipations;
}
