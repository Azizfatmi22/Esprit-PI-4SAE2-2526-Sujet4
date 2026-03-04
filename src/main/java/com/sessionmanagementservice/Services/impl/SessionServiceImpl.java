package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.PlanningRepository;
import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.Planning;
import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final PlanningRepository planningRepository;

    // ✅ CREATE SESSION
    @Override
    public Session createSession(Session session, Jwt jwt) {
        String trainerId = jwt.getSubject(); // 🔑 Extract trainerId from JWT

        // 1️⃣ Vérifier que la date n'est pas dans le passé
        if (!isDateValid(session.getCreatedAt())) {
            throw new RuntimeException("❌ Date de session invalide (ne peut pas être dans le passé)");
        }

        // 2️⃣ Vérifier la surcharge du formateur (max 3 sessions par jour)
        // timeSlot = null car on vérifie juste par date (pas par créneau horaire)
        boolean isOverloaded = checkTrainerOverload(trainerId, session.getCreatedAt(), null);

        if (isOverloaded) {
            // Récupérer le nombre exact de sessions pour le message
            List<Session> existingSessions = sessionRepository.findByTrainerIdAndCreatedAt(
                    trainerId,
                    session.getCreatedAt()
            );

            throw new RuntimeException(
                    "❌ Formateur surchargé : déjà " + existingSessions.size() +
                            " sessions programmées ce jour-là (maximum 3)"
            );
        }

        // 3️⃣ Configurer et sauvegarder la session
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
    @Override
    @Transactional
    public void updateSessionsStatusBasedOnPlanning() {
        LocalDate today = LocalDate.now();

        // Mettre à jour les sessions qui commencent aujourd'hui
        List<Session> startingSessions = getSessionsByPlanningDate(today, true);
        for (Session session : startingSessions) {
            if (session.getStatus() == SessionStatus.PLANNED) {
                session.setStatus(SessionStatus.ONGOING);
                sessionRepository.save(session);
                System.out.println("Session " + session.getId() + " démarrée le " + today);
            }
        }

        // Mettre à jour les sessions qui se terminent aujourd'hui
        List<Session> endingSessions = getSessionsByPlanningDate(today, false);
        for (Session session : endingSessions) {
            if (session.getStatus() == SessionStatus.ONGOING) {
                session.setStatus(SessionStatus.COMPLETED);
                sessionRepository.save(session);
                System.out.println("Session " + session.getId() + " terminée le " + today);
            }
        }
    }

    @Override
    @Transactional
    public Session updateSessionStatusBasedOnPlanning(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        LocalDate today = LocalDate.now();
        List<Planning> plannings = planningRepository.findBySessionId(sessionId);

        if (plannings.isEmpty()) {
            return session;
        }

        // Trouver la première et dernière date de planning
        LocalDate firstDate = plannings.stream()
                .map(Planning::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastDate = plannings.stream()
                .map(Planning::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (firstDate == null || lastDate == null) {
            return session;
        }

        // Mettre à jour le statut en fonction des dates
        SessionStatus newStatus = determineSessionStatus(firstDate, lastDate, today);

        if (session.getStatus() != newStatus) {
            session.setStatus(newStatus);
            session = sessionRepository.save(session);
        }

        return session;
    }

    @Override
    public List<Session> getSessionsStartingToday() {
        LocalDate today = LocalDate.now();
        return getSessionsByPlanningDate(today, true);
    }

    @Override
    public List<Session> getSessionsEndingToday() {
        LocalDate today = LocalDate.now();
        return getSessionsByPlanningDate(today, false);
    }

    /**
     * Récupère les sessions par date de planning
     * @param date La date à vérifier
     * @param isStart Si true, cherche les sessions qui commencent à cette date
     *                Si false, cherche les sessions qui se terminent à cette date
     */
    private List<Session> getSessionsByPlanningDate(LocalDate date, boolean isStart) {
        List<Planning> plannings;

        if (isStart) {
            plannings = planningRepository.findByStartDate(date);
        } else {
            plannings = planningRepository.findByEndDate(date);
        }

        return plannings.stream()
                .map(Planning::getSession)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Détermine le statut d'une session en fonction des dates
     */
    private SessionStatus determineSessionStatus(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (today.isBefore(startDate)) {
            return SessionStatus.PLANNED;
        } else if (today.isAfter(endDate)) {
            return SessionStatus.COMPLETED;
        } else {
            return SessionStatus.ONGOING;
        }
    }

    /**
     * Vérifie si une session devrait être en cours aujourd'hui
     */
    public boolean isSessionInProgressToday(Long sessionId) {
        List<Planning> plannings = planningRepository.findBySessionId(sessionId);
        LocalDate today = LocalDate.now();

        return plannings.stream()
                .anyMatch(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()));
    }


    public List<Session> getAllSessionsWithCurrentStatus() {
        List<Session> sessions = sessionRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Session session : sessions) {
            List<Planning> plannings = planningRepository.findBySessionId(session.getId());

            if (!plannings.isEmpty()) {
                LocalDate firstDate = plannings.stream()
                        .map(Planning::getStartDate)
                        .min(LocalDate::compareTo)
                        .orElse(null);

                LocalDate lastDate = plannings.stream()
                        .map(Planning::getEndDate)
                        .max(LocalDate::compareTo)
                        .orElse(null);

                if (firstDate != null && lastDate != null) {
                    SessionStatus calculatedStatus = determineSessionStatus(firstDate, lastDate, today);

                    // Mettre à jour si différent
                    if (session.getStatus() != calculatedStatus) {
                        session.setStatus(calculatedStatus);
                        sessionRepository.save(session);
                    }
                }
            }
        }

        return sessions;
    }
}