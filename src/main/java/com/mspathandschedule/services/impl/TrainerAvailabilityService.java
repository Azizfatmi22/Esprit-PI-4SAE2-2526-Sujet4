// services/TrainerAvailabilityService.java
package com.mspathandschedule.services.impl;

import com.mspathandschedule.clients.PlanningManagementFeignClient;
import com.mspathandschedule.clients.SessionManagementFeignClient;
import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.repositories.ScheduleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainerAvailabilityService {

    private final ScheduleRepository scheduleRepository;

    private final SessionManagementFeignClient sessionClient;

    public TrainerAvailabilityService(ScheduleRepository scheduleRepository,

                                      SessionManagementFeignClient sessionClient) {
        this.scheduleRepository = scheduleRepository;

        this.sessionClient = sessionClient;
    }

    /**
     * Get trainer ID from a planning
     */
    String getTrainerIdFromPlanning(Long planningId) {
        try {
            Map<String, Object> planning = sessionClient.getPlanningById(planningId);
            Map<String, Object> session = (Map<String, Object>) planning.get("session");
            if (session == null) return null;
            return (String) session.get("trainerId");
        } catch (Exception e) {
            System.err.println("Error getting trainer ID for planningId=" + planningId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if trainer is available for a given time slot
     */
    public boolean isTrainerAvailable(String trainerId, LocalDateTime start, LocalDateTime end) {
        // Get all schedules that overlap with the requested time
        List<Schedule> overlappingSchedules = scheduleRepository.findByStartTimeBetween(start, end);

        if (overlappingSchedules.isEmpty()) {
            return true;
        }

        // Get unique planning IDs
        List<Long> planningIds = overlappingSchedules.stream()
                .map(Schedule::getPlanningId)
                .distinct()
                .collect(Collectors.toList());

        // For each planning, get the trainer ID and compare
        for (Long planningId : planningIds) {
            String scheduleTrainerId = getTrainerIdFromPlanning(planningId);
            if (trainerId.equals(scheduleTrainerId)) {
                return false; // Trainer is busy
            }
        }

        return true;
    }

    /**
     * Suggest alternative time slots
     */
    public List<LocalDateTime> suggestAlternativeSlots(String trainerId, int durationHours, int maxSuggestions) {
        List<LocalDateTime> suggestions = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.now().withHour(9).withMinute(0);

        while (suggestions.size() < maxSuggestions && cursor.isBefore(LocalDateTime.now().plusDays(14))) {
            LocalDateTime end = cursor.plusHours(durationHours);

            // Skip weekends
            if (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cursor = cursor.withHour(9).withMinute(0).plusDays(1);
                continue;
            }

            // Skip outside working hours (9 AM - 6 PM)
            if (cursor.getHour() < 9 || cursor.getHour() > 18) {
                cursor = cursor.withHour(9).withMinute(0).plusDays(0);
                continue;
            }

            if (isTrainerAvailable(trainerId, cursor, end)) {
                suggestions.add(cursor);
            }
            cursor = cursor.plusHours(1);
        }

        return suggestions;
    }
}