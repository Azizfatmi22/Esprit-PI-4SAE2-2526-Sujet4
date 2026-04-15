package com.example.msforum.controllers;

import com.example.msforum.dto.ParticipantRequest;
import com.example.msforum.dto.ParticipantResponse;
import com.example.msforum.entities.Event;
import com.example.msforum.services.ParticipantService;
import com.example.msforum.services.PdfService;
import com.example.msforum.repositories.EventRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final ParticipantService participantService;
    private final EventRepository eventRepository;
    private final PdfService pdfService;

    @Value("${app.base-url}")
    private String frontBaseUrl;

    public EventController(ParticipantService participantService,
            EventRepository eventRepository,
            PdfService pdfService) {
        this.participantService = participantService;
        this.eventRepository = eventRepository;
        this.pdfService = pdfService;
    }

    @GetMapping
    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event saved = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event update) {
        return eventRepository.findById(id)
                .map(existing -> {
                    existing.setName(update.getName());
                    existing.setDescription(update.getDescription());
                    existing.setDetails(update.getDetails());
                    existing.setLocation(update.getLocation());
                    existing.setLatitude(update.getLatitude());
                    existing.setLongitude(update.getLongitude());
                    existing.setImageUrl(update.getImageUrl());
                    existing.setDate(update.getDate());
                    existing.setAttendees(update.getAttendees());
                    existing.setCategory(update.getCategory());
                    existing.setForumTopicId(update.getForumTopicId());
                    Event saved = eventRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/participants")
    public ResponseEntity<ParticipantResponse> addParticipant(@PathVariable Long eventId,
            @Valid @RequestBody ParticipantRequest request) {
        ParticipantResponse response = participantService.addParticipant(eventId, request.getName(),
                request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventId}/participants")
    public List<ParticipantResponse> getParticipants(@PathVariable Long eventId) {
        return participantService.getParticipants(eventId);
    }

    @GetMapping("/{eventId}/participants/all")
    public List<ParticipantResponse> getAllParticipantsForEvent(@PathVariable Long eventId) {
        return participantService.getAllParticipantsForEvent(eventId);
    }

    @DeleteMapping("/{eventId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(@PathVariable Long eventId,
            @PathVariable Long participantId) {
        participantService.removeParticipant(eventId, participantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirmation endpoint clicked from email.
     * On success, returns a simple 200 response. You can change this to redirect to
     * the front-end.
     */
    @GetMapping("/{eventId}/participants/confirm")
    public ResponseEntity<Void> confirmParticipant(@PathVariable Long eventId,
            @RequestParam("token") String token) {
        participantService.confirmParticipant(eventId, token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontBaseUrl + "/events"))
                .build();
    }

    @GetMapping("/{eventId}/participants/{participantId}/pdf")
    public ResponseEntity<InputStreamResource> downloadParticipantPdf(@PathVariable Long eventId,
            @PathVariable Long participantId) {
        ByteArrayInputStream bis = pdfService.generateParticipantPdf(eventId, participantId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=participation-ticket.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/participants")
    public List<ParticipantResponse> getAllParticipants() {
        return participantService.getAllParticipants();
    }
}
