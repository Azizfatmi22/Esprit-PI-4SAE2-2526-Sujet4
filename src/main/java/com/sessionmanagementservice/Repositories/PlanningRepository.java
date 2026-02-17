package com.sessionmanagementservice.Repositories;

import com.sessionmanagementservice.entities.Planning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanningRepository extends JpaRepository<Planning, Integer> {
    List<Planning> findBySessionId(Long sessionId);
}
