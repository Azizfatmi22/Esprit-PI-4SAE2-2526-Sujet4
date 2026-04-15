package com.example.msforum.repositories;

import com.example.msforum.entities.Participant;
import com.example.msforum.entities.ParticipantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByEventId(Long eventId);
    List<Participant> findByEventIdAndStatus(Long eventId, ParticipantStatus status);
    Optional<Participant> findByConfirmationToken(String token);
    List<Participant> findByStatus(ParticipantStatus status);
}
