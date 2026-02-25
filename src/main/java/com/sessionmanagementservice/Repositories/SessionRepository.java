package com.sessionmanagementservice.Repositories;

import com.sessionmanagementservice.entities.Session;
import com.sessionmanagementservice.entities.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByStatus(SessionStatus status);
    List<Session> findByTrainerId(String trainerId);
    List<Session> findByTrainerIdAndCreatedAt(String trainerId, LocalDate date);
    List<Session> findByPlanning_Location_IdAndCreatedAt(Long locationId, LocalDate date);
}
