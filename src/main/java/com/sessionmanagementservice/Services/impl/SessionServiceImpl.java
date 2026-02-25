package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    // ✅ CREATE SESSION
    @Override
    public Session createSession(Session session, Jwt jwt) {
        String trainerId = jwt.getSubject(); // 🔑 Extract trainerId from JWT

        if (!isDateValid(session.getCreatedAt()))
            throw new RuntimeException("Session date cannot be in the past");

        if (!isTrainerAvailable(trainerId, session.getCreatedAt(), null))
            throw new RuntimeException("Trainer is not available on this date");

        session.setTrainerId(trainerId);
        session.setStatus(SessionStatus.PLANNED);

        return sessionRepository.save(session);
    }

    // ✅ UPDATE SESSION
    @Override
    public Session updateSession(Long id, Session updatedSession) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() == SessionStatus.COMPLETED)
            throw new RuntimeException("Cannot update a completed session");

        if (updatedSession.getCreatedAt() != null) {
            if (!isDateValid(updatedSession.getCreatedAt()))
                throw new RuntimeException("Start date cannot be in the past");
            session.setCreatedAt(updatedSession.getCreatedAt());
        }

        if (updatedSession.getMaxParticipants() != null)
            session.setMaxParticipants(updatedSession.getMaxParticipants());

        if (updatedSession.getStatus() != null)
            session.setStatus(updatedSession.getStatus());

        return sessionRepository.save(session);
    }

    // ✅ DELETE SESSION
    @Override
    public void deleteSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() == SessionStatus.ONGOING)
            throw new RuntimeException("Cannot delete an ongoing session");

        sessionRepository.delete(session);
    }

    // ✅ GET SESSION BY ID
    @Override
    public Session getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    // ✅ GET ALL SESSIONS
    @Override
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    // ✅ GET SESSIONS BY TRAINER
    @Override
    public List<Session> getSessionsByTrainer(Jwt jwt) {
        String trainerId = jwt.getSubject();
        return sessionRepository.findByTrainerId(trainerId);
    }

    // ----------------------
    // ✅ BUSINESS LOGIC FUNCTIONS (SESSION FOCUSED)
    // ----------------------

    @Override
    public boolean isTrainerAvailable(String trainerId, LocalDate date, String timeSlot) {
        return sessionRepository.findByTrainerIdAndCreatedAt(trainerId, date).isEmpty();
    }

    @Override
    public boolean hasSchedulingConflict(String trainerId, Long locationId, LocalDate date, String timeSlot) {
        boolean trainerConflict = !sessionRepository.findByTrainerIdAndCreatedAt(trainerId, date).isEmpty();
        boolean locationConflict = !sessionRepository.findByPlanning_Location_IdAndCreatedAt(locationId, date).isEmpty();
        return trainerConflict || locationConflict;
    }

    @Override
    public boolean checkTrainerOverload(String trainerId, LocalDate date, String timeSlot) {
        List<Session> sessions = sessionRepository.findByTrainerIdAndCreatedAt(trainerId, date);
        // Trainer is overloaded only if sessions >= max per day
        int maxPerDay = 3;
        return sessions.size() >= maxPerDay;
    }

    @Override
    public boolean isDateValid(LocalDate date) {
        if (date == null) return false;
        LocalDate today = LocalDate.now(); // current system date
        return !date.isBefore(today);      // allow today
    }

    @Override
    public void cancelSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(SessionStatus.CANCELED);
        sessionRepository.save(session);
        // TODO: implement participant notifications
    }

    @Override
    public void updateSessionStatuses() {
        LocalDate today = LocalDate.now();
        List<Session> sessions = sessionRepository.findAll();
        for (Session s : sessions) {
            if (s.getStatus() == SessionStatus.PLANNED && s.getCreatedAt().isEqual(today))
                s.setStatus(SessionStatus.ONGOING);
            else if (s.getStatus() == SessionStatus.ONGOING && s.getCreatedAt().isBefore(today))
                s.setStatus(SessionStatus.COMPLETED);
        }
        sessionRepository.saveAll(sessions);
    }
}