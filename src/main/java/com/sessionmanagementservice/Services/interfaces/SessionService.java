package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Session;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;

public interface SessionService {


    Session createSession(Session session, Jwt jwt); // 🔑 JWT for trainerId
    Session updateSession(Long id, Session session);
    void deleteSession(Long id);
    Session getSessionById(Long id);
    List<Session> getAllSessions();


    List<Session> getSessionsByTrainer(Jwt jwt); // automatically get trainerId from JWT


    boolean isTrainerAvailable(String trainerId, LocalDate date, String timeSlot);
    boolean hasSchedulingConflict(String trainerId, Long locationId, LocalDate date, String timeSlot);
    boolean checkTrainerOverload(String trainerId, LocalDate date, String timeSlot);
    boolean isDateValid(LocalDate date);
    void cancelSession(Long sessionId);
    void updateSessionStatuses();
    public List<Session> getAllSessionsWithCurrentStatus();
    void updateSessionsStatusBasedOnPlanning();
    Session updateSessionStatusBasedOnPlanning(Long sessionId);
    List<Session> getSessionsStartingToday();
    List<Session> getSessionsEndingToday();// automatically updates Planned -> Ongoing -> Completed
}