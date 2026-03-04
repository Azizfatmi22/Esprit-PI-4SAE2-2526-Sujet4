package com.sessionmanagementservice.Repositories;

import com.sessionmanagementservice.entities.Planning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    List<Planning> findByStartDate(LocalDate startDate);

    // Nouvelle méthode pour trouver les plannings par date de fin
    List<Planning> findByEndDate(LocalDate endDate);

    // Vérifier si une session a des plannings
    boolean existsBySessionId(Long sessionId);

    // Trouver les plannings entre deux dates
    List<Planning> findByStartDateBetweenOrEndDateBetween(
            LocalDate startDateStart, LocalDate startDateEnd,
            LocalDate endDateStart, LocalDate endDateEnd
    );

    @Query("SELECT p FROM Planning p WHERE p.location.id = :locationId " +
            "AND p.startDate <= :endDate AND p.endDate >= :startDate")
    List<Planning> findConflictingPlannings(
            @Param("locationId") Long locationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<Planning> findFirstBySessionId(Long sessionId);

    @Modifying
    @Query("DELETE FROM Planning p WHERE p.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
