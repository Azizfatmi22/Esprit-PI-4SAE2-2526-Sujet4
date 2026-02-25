package com.sessionmanagementservice.Repositories;

import com.sessionmanagementservice.entities.Planning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PlanningRepository extends JpaRepository<Planning, Integer> {
    List<Planning> findBySessionId(Long sessionId);
    List<Planning> findByLocationId(Long locationId);
    long countByLocationId(Long locationId);

    @Query("SELECT p FROM Planning p WHERE p.location.id = :locationId " +
            "AND p.startDate <= :endDate AND p.endDate >= :startDate")
    List<Planning> findByLocationIdAndDateRange(
            @Param("locationId") Long locationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
