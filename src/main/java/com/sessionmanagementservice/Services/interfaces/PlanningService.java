package com.sessionmanagementservice.Services.interfaces;

import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.Planning;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PlanningService {

    Planning createPlanning(Planning planning, Long  sessionId, Long locationId);

    Planning updatePlanning(int id, Planning planning);

    void deletePlanning(int id);

    Planning getPlanningById(int id);

    List<Planning> getPlanningsBySession(Long sessionId);

    // Generate planning automatically based on duration
    List<Planning> generatePlanning(Long sessionId, Long locationId);

    // Distribute sessions across available days
    List<Planning> distributePlanning(Long sessionId, Long locationId, int numberOfDays);

    // Detect overlapping planning
    boolean hasPlanningConflict(Long locationId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    // Suggest next available time slot
    java.time.LocalDate suggestNextAvailableDate(Long locationId, java.time.LocalDate startDate);

    // Optimize planning (avoid conflicts & overload)
    List<Planning> optimizePlanning(Long sessionId);

    // Get location usage statistics
    long countPlanningsByLocation(Long locationId);

    public List<Planning> fillGaps(Long sessionId, Long locationId);

    public List<Planning> maintainRollingPlanning(Long sessionId, Long locationId, int daysAhead);

    public Location suggestBestLocation(LocalDate date);

    public Map<String, Object>  getBusyDays(Long locationId) ;

    public LocalDate smartSuggestDate(Long locationId, LocalDate start);
    public Map<String, Object> isHighRiskPlanning(Long sessionId);


}
