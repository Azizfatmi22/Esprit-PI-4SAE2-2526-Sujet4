package com.example.msforum.services.impl;

import com.example.msforum.dto.ParticipantResponse;
import com.example.msforum.entities.Event;
import com.example.msforum.entities.Participant;
import com.example.msforum.entities.ParticipantStatus;
import com.example.msforum.exception.ResourceNotFoundException;
import com.example.msforum.repositories.EventRepository;
import com.example.msforum.repositories.ParticipantRepository;
import com.example.msforum.services.EmailService;
import com.example.msforum.services.ParticipantService;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EmailService emailService;

    public ParticipantServiceImpl(EventRepository eventRepository,
            ParticipantRepository participantRepository,
            EmailService emailService) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.emailService = emailService;
    }

    @Override
    public ParticipantResponse addParticipant(Long eventId, String name, String email) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        Participant p = new Participant();
        p.setEvent(event);
        p.setName(name);
        p.setEmail(email);
        p.setStatus(ParticipantStatus.PENDING);
        p.setConfirmationToken(UUID.randomUUID().toString());

        // Update event current participants count
        if (event.getCurrentParticipants() == null) {
            event.setCurrentParticipants(0);
        }
        event.setCurrentParticipants(event.getCurrentParticipants() + 1);
        eventRepository.save(event);

        Participant saved = participantRepository.save(p);

        // Send real confirmation email (async — non-blocking)
        emailService.sendParticipationConfirmation(
                email, name, event.getName(), event.getId(), saved.getConfirmationToken());

        return ParticipantResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        return participantRepository.findByEventIdAndStatus(eventId, ParticipantStatus.CONFIRMED)
                .stream()
                .map(p -> ParticipantResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getAllParticipantsForEvent(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        return participantRepository.findByEventId(eventId)
                .stream()
                .map(p -> ParticipantResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void removeParticipant(Long eventId, Long participantId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        Participant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id " + participantId));

        if (p.getEvent() == null || !p.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("Participant " + participantId + " is not in event " + eventId);
        }

        participantRepository.delete(p);

        // Decrement participant count
        if (event.getCurrentParticipants() != null && event.getCurrentParticipants() > 0) {
            event.setCurrentParticipants(event.getCurrentParticipants() - 1);
            eventRepository.save(event);
        }
    }

    @Override
    public void confirmParticipant(Long eventId, String token) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));

        Participant p = participantRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired confirmation token"));

        if (!p.getEvent().getId().equals(event.getId())) {
            throw new ResourceNotFoundException("Token does not match the event");
        }

        p.setStatus(ParticipantStatus.CONFIRMED);
        p.setConfirmationToken(null);
        participantRepository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getAllParticipants() {
        return participantRepository.findByStatus(ParticipantStatus.CONFIRMED)
                .stream()
                .map(p -> ParticipantResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}
