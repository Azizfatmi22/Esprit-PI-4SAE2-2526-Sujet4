package com.example.msforum.repositories;

import com.example.msforum.entities.Event;
import com.example.msforum.entities.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {
    long countByStatus(EventStatus status);

    @Query("SELECT AVG(CAST(e.currentParticipants AS double) / e.maxParticipants) FROM Event e WHERE e.maxParticipants > 0")
    Double getAverageParticipationRate();

    Event findTopByOrderByCurrentParticipantsDesc();
}

