package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Session;

import java.util.List;

public interface SessionService {
    Session createSession(Session session);
    Session updateSession(int id, Session session);
    void deleteSession(int id);
    Session getSessionById(int id);
    List<Session> getAllSessions();
}
