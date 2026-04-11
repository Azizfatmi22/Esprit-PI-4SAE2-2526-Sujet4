package com.mspathandschedule.services.interfaces;

import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.entities.ScheduleStatus;
import com.mspathandschedule.entities.ScheduleType;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public interface ScheduleService {

    // ==================== CRUD OPERATIONS ====================
    Schedule createSchedule(Schedule schedule);
    Schedule updateSchedule(Long id, Schedule schedule);
    Schedule getSchedule(Long id);
    List<Schedule> getAllSchedules();
    void deleteSchedule(Long id);

    // ==================== PLANNING RELATIONS ====================
    List<Schedule> getSchedulesByPlanningId(Long planningId);
    Schedule addScheduleToPlanning(Long planningId, Schedule schedule);
    void removeScheduleFromPlanning(Long scheduleId);

    // ==================== ADVANCED FUNCTIONS ====================

    // 1. Schedule Analytics
    Map<String, Object> getScheduleAnalytics(Long planningId);
    Map<String, Object> getScheduleStatistics(Long planningId);

    // 2. Conflict Detection
    boolean hasTimeConflict(Long planningId, LocalDateTime startTime, LocalDateTime endTime);
    List<Schedule> findConflictingSchedules(Long planningId, LocalDateTime startTime, LocalDateTime endTime);

    // 3. Schedule Optimization
    Schedule optimizeScheduleTime(Long scheduleId, LocalDateTime preferredStart);
    List<Schedule> autoScheduleSessions(Long planningId, int daysNeeded);

    // 4. Schedule Status Management
    Schedule updateScheduleStatus(Long scheduleId, ScheduleStatus status);
    void updateAllSchedulesStatus(Long planningId);

    // 5. Schedule Generation
    List<Schedule> generateWeeklySchedule(Long planningId, int weeks);
    Schedule generateScheduleFromTemplate(Long planningId, Long templateId);

    // 6. Schedule Analytics
    Map<String, Object> calculateScheduleEfficiency(Long planningId);
    Map<String, Object> getUpcomingSchedules(Long planningId, int days);

    // 7. Bulk Operations
    List<Schedule> bulkCreateSchedules(Long planningId, List<Schedule> schedules);
    void bulkUpdateStatus(List<Long> scheduleIds, ScheduleStatus status);

    // 8. Schedule Recommendations
    List<LocalDateTime> suggestAvailableTimeSlots(Long planningId, int durationHours, int daysAhead);

    // 9. Export/Import
    String exportScheduleToJson(Long planningId);
    void importScheduleFromJson(Long planningId, String jsonData);

    // ==================== ADDITIONAL METHODS FROM IMPLEMENTATION ====================

    // Buffer Time Check
    boolean hasTimeConflictWithBuffer(Long planningId, LocalDateTime startTime, LocalDateTime endTime);

    // Conflict Resolution Suggestions
    List<LocalDateTime> suggestAlternativeSlots(Long planningId, int durationHours, int maxSuggestions);

    // Recurring Sessions
    List<Schedule> createRecurringSessions(Long planningId, Schedule template,
                                           LocalDate startDate, int weeks,
                                           List<DayOfWeek> days);

    // Session Split
    List<Schedule> splitSession(Long scheduleId, int breakMinutes);

    // Shift Future Sessions
    void shiftFutureSessions(Long planningId, LocalDateTime fromDate, int offsetHours);

    // iCal Export
    String exportToIcal(Long planningId);

    // Schedule Duplication
    Schedule duplicateSchedule(Long scheduleId, LocalDateTime newStartTime);

    // Bulk Delete
    void bulkDelete(List<Long> scheduleIds);
}