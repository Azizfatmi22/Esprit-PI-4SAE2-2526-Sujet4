package com.sessionmanagementservice.Services.impl;

import com.sessionmanagementservice.Repositories.SessionRepository;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

@Transactional
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    SessionServiceImpl(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    @Override
    public Session createSession(Session session) {
        session.setStatus(SessionStatus.PLANNED);
        return sessionRepository.save(session);
    }

    @Override
    public Session updateSession(int id, Session updatedSession) {

        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(updatedSession.getStatus());
        session.setMaxParticipants(updatedSession.getMaxParticipants());

        return sessionRepository.save(session);
    }

    @Override
    public void deleteSession(int id) {
        sessionRepository.deleteById(id);
    }

    @Override
    public Session getSessionById(int id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    @Override
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }
}
