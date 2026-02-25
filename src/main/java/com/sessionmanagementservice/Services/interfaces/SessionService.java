package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Session;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;

public interface SessionService {

    // ✅ Core CRUD
    Session createSession(Session session, Jwt jwt); // 🔑 JWT for trainerId
    Session updateSession(Long id, Session session);
    void deleteSession(Long id);
    Session getSessionById(Long id);
    List<Session> getAllSessions();

    // ✅ Trainer-specific
    List<Session> getSessionsByTrainer(Jwt jwt); // automatically get trainerId from JWT

    // ----------------------
    // ✅ Session Business Functions
    // ----------------------
    boolean isTrainerAvailable(String trainerId, LocalDate date, String timeSlot);
    boolean hasSchedulingConflict(String trainerId, Long locationId, LocalDate date, String timeSlot);
    boolean checkTrainerOverload(String trainerId, LocalDate date, String timeSlot);
    boolean isDateValid(LocalDate date);
    void cancelSession(Long sessionId);
    void updateSessionStatuses(); // automatically updates Planned -> Ongoing -> Completed
}