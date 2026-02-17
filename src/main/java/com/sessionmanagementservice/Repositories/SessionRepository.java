package com.sessionmanagementservice.Repositories;

import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    List<Session> findByStatus(SessionStatus status);
}
