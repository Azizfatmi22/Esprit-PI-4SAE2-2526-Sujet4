package com.mspathandschedule.controllers;

import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.entities.ScheduleStatus;
import com.mspathandschedule.services.interfaces.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // ==================== BASIC CRUD OPERATIONS ====================

    @PostMapping
    public ResponseEntity<Schedule> create(@RequestBody Schedule schedule) {
        return ResponseEntity.ok(scheduleService.createSchedule(schedule));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ScheduleController is working!");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getSchedule(id));
    }

    @GetMapping
    public ResponseEntity<List<Schedule>> getAll() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Schedule> update(@PathVariable Long id, @RequestBody Schedule schedule) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok().build();
    }

    // ==================== PLANNING RELATIONS ====================

    @GetMapping("/planning/{planningId}")
    public ResponseEntity<List<Schedule>> getByPlanningId(@PathVariable Long planningId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByPlanningId(planningId));
    }

    @PostMapping("/planning/{planningId}")
    public ResponseEntity<Schedule> addToPlanning(@PathVariable Long planningId, @RequestBody Schedule schedule) {
        return ResponseEntity.ok(scheduleService.addScheduleToPlanning(planningId, schedule));
    }

    @DeleteMapping("/{scheduleId}/planning")
    public ResponseEntity<Void> removeFromPlanning(@PathVariable Long scheduleId) {
        scheduleService.removeScheduleFromPlanning(scheduleId);
        return ResponseEntity.ok().build();
    }

    // ==================== SCHEDULE ANALYTICS ====================

    @GetMapping("/planning/{planningId}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable Long planningId) {
        return ResponseEntity.ok(scheduleService.getScheduleAnalytics(planningId));
    }

    @GetMapping("/planning/{planningId}/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable Long planningId) {
        return ResponseEntity.ok(scheduleService.getScheduleStatistics(planningId));
    }

    @GetMapping("/planning/{planningId}/efficiency")
    public ResponseEntity<Map<String, Object>> getEfficiency(@PathVariable Long planningId) {
        return ResponseEntity.ok(scheduleService.calculateScheduleEfficiency(planningId));
    }

    @GetMapping("/planning/{planningId}/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcoming(@PathVariable Long planningId,
                                                           @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(scheduleService.getUpcomingSchedules(planningId, days));
    }

    // ==================== CONFLICT DETECTION ====================

    @GetMapping("/planning/{planningId}/conflict")
    public ResponseEntity<Boolean> hasConflict(@PathVariable Long planningId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(scheduleService.hasTimeConflict(planningId, start, end));
    }

    @GetMapping("/planning/{planningId}/conflicts")
    public ResponseEntity<List<Schedule>> findConflicts(@PathVariable Long planningId,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(scheduleService.findConflictingSchedules(planningId, start, end));
    }

    @GetMapping("/planning/{planningId}/conflict-buffer")
    public ResponseEntity<Boolean> hasConflictWithBuffer(@PathVariable Long planningId,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(scheduleService.hasTimeConflictWithBuffer(planningId, start, end));
    }

    // ==================== SCHEDULE OPTIMIZATION ====================

    @PostMapping("/{scheduleId}/optimize")
    public ResponseEntity<Schedule> optimizeTime(@PathVariable Long scheduleId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime preferredStart) {
        return ResponseEntity.ok(scheduleService.optimizeScheduleTime(scheduleId, preferredStart));
    }

    @PostMapping("/planning/{planningId}/auto-schedule")
    public ResponseEntity<List<Schedule>> autoSchedule(@PathVariable Long planningId,
                                                       @RequestParam int daysNeeded) {
        return ResponseEntity.ok(scheduleService.autoScheduleSessions(planningId, daysNeeded));
    }

    // ==================== STATUS MANAGEMENT ====================

    @PatchMapping("/{scheduleId}/status")
    public ResponseEntity<Schedule> updateStatus(@PathVariable Long scheduleId,
                                                 @RequestParam ScheduleStatus status) {
        return ResponseEntity.ok(scheduleService.updateScheduleStatus(scheduleId, status));
    }

    @PostMapping("/planning/{planningId}/update-statuses")
    public ResponseEntity<Void> updateAllStatuses(@PathVariable Long planningId) {
        scheduleService.updateAllSchedulesStatus(planningId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/planning/{planningId}/status/{status}")
    public ResponseEntity<List<Schedule>> getByStatus(@PathVariable Long planningId,
                                                      @PathVariable ScheduleStatus status) {
        return ResponseEntity.ok(scheduleService.getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStatus() == status)
                .toList());
    }

    // ==================== SCHEDULE GENERATION ====================

    @PostMapping("/planning/{planningId}/generate-weekly")
    public ResponseEntity<List<Schedule>> generateWeekly(@PathVariable Long planningId,
                                                         @RequestParam(defaultValue = "4") int weeks) {
        return ResponseEntity.ok(scheduleService.generateWeeklySchedule(planningId, weeks));
    }

    @PostMapping("/planning/{planningId}/generate-from-template/{templateId}")
    public ResponseEntity<Schedule> generateFromTemplate(@PathVariable Long planningId,
                                                         @PathVariable Long templateId) {
        return ResponseEntity.ok(scheduleService.generateScheduleFromTemplate(planningId, templateId));
    }

    // ==================== TIME SLOT SUGGESTIONS ====================

    @GetMapping("/planning/{planningId}/available-slots")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(@PathVariable Long planningId,
                                                                 @RequestParam int durationHours,
                                                                 @RequestParam(defaultValue = "14") int daysAhead) {
        return ResponseEntity.ok(scheduleService.suggestAvailableTimeSlots(planningId, durationHours, daysAhead));
    }

    @GetMapping("/planning/{planningId}/alternative-slots")
    public ResponseEntity<List<LocalDateTime>> getAlternativeSlots(@PathVariable Long planningId,
                                                                   @RequestParam int durationHours,
                                                                   @RequestParam(defaultValue = "5") int maxSuggestions) {
        return ResponseEntity.ok(scheduleService.suggestAlternativeSlots(planningId, durationHours, maxSuggestions));
    }

    // ==================== RECURRING SESSIONS ====================

    @PostMapping("/planning/{planningId}/recurring")
    public ResponseEntity<List<Schedule>> createRecurringSessions(@PathVariable Long planningId,
                                                                  @RequestBody Schedule template,
                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                  @RequestParam int weeks,
                                                                  @RequestParam List<DayOfWeek> days) {
        return ResponseEntity.ok(scheduleService.createRecurringSessions(planningId, template, startDate, weeks, days));
    }

    // ==================== SESSION MANAGEMENT ====================

    @PostMapping("/{scheduleId}/split")
    public ResponseEntity<List<Schedule>> splitSession(@PathVariable Long scheduleId,
                                                       @RequestParam(defaultValue = "30") int breakMinutes) {
        return ResponseEntity.ok(scheduleService.splitSession(scheduleId, breakMinutes));
    }

    @PostMapping("/planning/{planningId}/shift")
    public ResponseEntity<Void> shiftFutureSessions(@PathVariable Long planningId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                    @RequestParam int offsetHours) {
        scheduleService.shiftFutureSessions(planningId, fromDate, offsetHours);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{scheduleId}/duplicate")
    public ResponseEntity<Schedule> duplicateSchedule(@PathVariable Long scheduleId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStartTime) {
        return ResponseEntity.ok(scheduleService.duplicateSchedule(scheduleId, newStartTime));
    }

    // ==================== EXPORT/IMPORT ====================

    @GetMapping("/planning/{planningId}/export")
    public ResponseEntity<String> export(@PathVariable Long planningId) {
        return ResponseEntity.ok(scheduleService.exportScheduleToJson(planningId));
    }

    @GetMapping("/planning/{planningId}/export/ical")
    public ResponseEntity<String> exportToIcal(@PathVariable Long planningId) {
        String icalData = scheduleService.exportToIcal(planningId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar")
                .header("Content-Disposition", "attachment; filename=schedule.ics")
                .body(icalData);
    }

    @PostMapping("/planning/{planningId}/import")
    public ResponseEntity<Void> importSchedule(@PathVariable Long planningId,
                                               @RequestBody String jsonData) {
        scheduleService.importScheduleFromJson(planningId, jsonData);
        return ResponseEntity.ok().build();
    }

    // ==================== BULK OPERATIONS ====================

    @PostMapping("/planning/{planningId}/bulk")
    public ResponseEntity<List<Schedule>> bulkCreate(@PathVariable Long planningId,
                                                     @RequestBody List<Schedule> schedules) {
        return ResponseEntity.ok(scheduleService.bulkCreateSchedules(planningId, schedules));
    }

    @PatchMapping("/bulk/status")
    public ResponseEntity<Void> bulkUpdateStatus(@RequestParam List<Long> scheduleIds,
                                                 @RequestParam ScheduleStatus status) {
        scheduleService.bulkUpdateStatus(scheduleIds, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDelete(@RequestParam List<Long> scheduleIds) {
        scheduleService.bulkDelete(scheduleIds);
        return ResponseEntity.ok().build();
    }

    // ==================== SCHEDULE VALIDATION ====================

    @GetMapping("/{scheduleId}/validate")
    public ResponseEntity<Map<String, Object>> validate(@PathVariable Long scheduleId) {
        Schedule schedule = scheduleService.getSchedule(scheduleId);
        Map<String, Object> validation = Map.of(
                "isValid", schedule.getStartTime().isBefore(schedule.getEndTime()),
                "isPast", schedule.getStartTime().isBefore(LocalDateTime.now()),
                "status", schedule.getStatus(),
                "durationHours", java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toHours()
        );
        return ResponseEntity.ok(validation);
    }

    // ==================== SCHEDULE SUMMARY ====================

    @GetMapping("/planning/{planningId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable Long planningId) {
        List<Schedule> schedules = scheduleService.getSchedulesByPlanningId(planningId);

        long totalHours = schedules.stream()
                .mapToLong(s -> java.time.Duration.between(s.getStartTime(), s.getEndTime()).toHours())
                .sum();

        Map<String, Object> summary = Map.of(
                "totalSchedules", schedules.size(),
                "totalHours", totalHours,
                "averageHoursPerSchedule", schedules.isEmpty() ? 0 : totalHours / schedules.size(),
                "upcomingCount", schedules.stream()
                        .filter(s -> s.getStartTime().isAfter(LocalDateTime.now()))
                        .count(),
                "activeCount", schedules.stream()
                        .filter(s -> s.getStatus() == ScheduleStatus.ACTIVE)
                        .count(),
                "confirmedCount", schedules.stream()
                        .filter(s -> s.getStatus() == ScheduleStatus.CONFIRMED)
                        .count(),
                "pendingCount", schedules.stream()
                        .filter(s -> s.getStatus() == ScheduleStatus.PENDING)
                        .count(),
                "cancelledCount", schedules.stream()
                        .filter(s -> s.getStatus() == ScheduleStatus.CANCELLED)
                        .count()
        );

        return ResponseEntity.ok(summary);
    }
}