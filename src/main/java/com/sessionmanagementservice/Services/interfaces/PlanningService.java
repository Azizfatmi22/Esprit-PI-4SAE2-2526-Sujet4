package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.Planning;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PlanningService {

    Planning createPlanning(Planning planning, Long sessionId, Long locationId);

    Planning updatePlanning(int id, Planning planning);

    void deletePlanning(int id);

    Planning getPlanningById(int id);

    List<Planning> getPlanningsBySession(Long sessionId);

    // Generate planning automatically based on duration
    Planning generatePlanning(Long sessionId);

    // Distribute sessions across available days
    Planning distributePlanning(Long sessionId, Long locationId, int numberOfDays);

    // Detect overlapping planning
    boolean hasPlanningConflict(Long locationId, LocalDate startDate, LocalDate endDate);

    // Suggest next available time slot
    LocalDate suggestNextAvailableDate(Long locationId, LocalDate startDate);

    boolean isValidDay(LocalDate date);

    // Optimize planning (avoid conflicts & overload)
    Planning optimizePlanning(Long sessionId);

    // Get location usage statistics
    long countPlanningsByLocation(Long locationId);

    Planning fillGaps(Long sessionId, Long locationId);

    Planning maintainRollingPlanning(Long sessionId, Long locationId, int daysAhead);

    Location suggestBestLocation(LocalDate date);

    Map<String, Object> getBusyDays(Long locationId);

    LocalDate smartSuggestDate(Long locationId, LocalDate start);

    Map<String, Object> isHighRiskPlanning(Long sessionId);
}