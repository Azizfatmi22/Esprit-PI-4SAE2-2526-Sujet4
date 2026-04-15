package com.example.msforum.backoffice;

import com.example.msforum.entities.Event;
import com.example.msforum.entities.EventStatus;
import com.example.msforum.repositories.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventAnalyticsService {

    private final EventRepository eventRepository;
    private final EventStatusScheduler eventStatusScheduler;

    public EventAnalyticsService(EventRepository eventRepository, EventStatusScheduler eventStatusScheduler) {
        this.eventRepository = eventRepository;
        this.eventStatusScheduler = eventStatusScheduler;
    }

    public EventAnalyticsDTO getAnalytics() {
        // Ensure statuses are up-to-date before fetching analytics
        eventStatusScheduler.updateEventStatuses();
        
        long total = eventRepository.count();
        long upcoming = eventRepository.countByStatus(EventStatus.UPCOMING);
        long ongoing = eventRepository.countByStatus(EventStatus.ONGOING);
        long finished = eventRepository.countByStatus(EventStatus.FINISHED);
        
        Double avgRate = eventRepository.getAverageParticipationRate();
        Event popular = eventRepository.findTopByOrderByCurrentParticipantsDesc();
        
        // Collect per-event participation stats
        List<EventParticipationDTO> eventParticipations = eventRepository.findAll().stream()
            .map(e -> EventParticipationDTO.builder()
                .eventName(e.getName())
                .currentParticipants(e.getCurrentParticipants() != null ? e.getCurrentParticipants() : 0)
                .maxParticipants(e.getMaxParticipants() != null ? e.getMaxParticipants() : (e.getAttendees() != null ? e.getAttendees() : 0))
                .build())
            .collect(Collectors.toList());

        return EventAnalyticsDTO.builder()
                .totalEvents(total)
                .upcomingEvents(upcoming)
                .ongoingEvents(ongoing)
                .finishedEvents(finished)
                .averageParticipationRate(avgRate != null ? avgRate : 0.0)
                .mostPopularEventName(popular != null ? popular.getName() : null)
                .eventParticipations(eventParticipations)
                .build();
    }
}
