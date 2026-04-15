package com.example.msforum.services;

import com.example.msforum.dto.ParticipantResponse;
import java.util.List;

public interface ParticipantService {
    ParticipantResponse addParticipant(Long eventId, String name, String email);
    List<ParticipantResponse> getParticipants(Long eventId);
    List<ParticipantResponse> getAllParticipantsForEvent(Long eventId);
    void removeParticipant(Long eventId, Long participantId);
    void confirmParticipant(Long eventId, String token);
    List<ParticipantResponse> getAllParticipants();
}
