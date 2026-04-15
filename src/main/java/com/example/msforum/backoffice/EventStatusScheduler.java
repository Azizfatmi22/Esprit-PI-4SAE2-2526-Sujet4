package com.example.msforum.backoffice;

import com.example.msforum.entities.Event;
import com.example.msforum.entities.EventStatus;
import com.example.msforum.repositories.EventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventStatusScheduler {

    private final EventRepository eventRepository;

    public EventStatusScheduler(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void updateEventStatuses() {
        List<Event> events = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Event event : events) {
            updateStatus(event, now);
        }
        eventRepository.saveAll(events);
    }

    // Run on startup
    @jakarta.annotation.PostConstruct
    @Transactional
    public void init() {
        updateEventStatuses();
    }


    private void updateStatus(Event event, LocalDateTime now) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            return;
        }

        LocalDateTime start = event.getDate();
        LocalDateTime end = event.getEndDate();
        
        // Use currentParticipants field instead of lazy loading participants collection
        int current = event.getCurrentParticipants() != null ? event.getCurrentParticipants() : 0;
        
        // Only try to sync from participants list if explicitly initialized (avoid LazyInitializationException)
        // But since we are updating count on add/remove, we can trust currentParticipants field
        
        if (event.getCurrentParticipants() == null) {
             event.setCurrentParticipants(0);
             current = 0;
        }

        int max = event.getMaxParticipants() != null ? event.getMaxParticipants() : (event.getAttendees() != null ? event.getAttendees() : Integer.MAX_VALUE);

        // Sync maxParticipants if needed
        if (event.getMaxParticipants() == null && event.getAttendees() != null) {
             event.setMaxParticipants(event.getAttendees());
             max = event.getAttendees();
        }

        // If no end date is set, default to 24 hours after start date
        if (end == null && start != null) {
            end = start.plusDays(1);
        }

        if (end != null && now.isAfter(end)) {
            event.setStatus(EventStatus.FINISHED);
        } else if (current >= max) {
            event.setStatus(EventStatus.FULL);
        } else if (start != null && now.isBefore(start)) {
            event.setStatus(EventStatus.UPCOMING);
        } else if (start != null && (end == null || !now.isAfter(end)) && (now.isEqual(start) || now.isAfter(start))) {
            event.setStatus(EventStatus.ONGOING);
        }
    }
}
