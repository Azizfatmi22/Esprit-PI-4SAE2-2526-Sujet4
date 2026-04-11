package com.mspathandschedule.services.impl;

import com.mspathandschedule.clients.SessionManagementFeignClient;
import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.entities.ScheduleStatus;
import com.mspathandschedule.entities.ScheduleType;
import com.mspathandschedule.repositories.ScheduleRepository;
import com.mspathandschedule.services.interfaces.ScheduleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END   = LocalTime.of(18, 0);

    private final ScheduleRepository scheduleRepository;
    private final TrainerAvailabilityService trainerAvailabilityService;
    private final SessionManagementFeignClient sessionClient;

    @Value("${schedule.buffer.minutes:30}")
    private int bufferMinutes;

    public ScheduleServiceImpl(ScheduleRepository scheduleRepository,
                               TrainerAvailabilityService trainerAvailabilityService,
                               SessionManagementFeignClient sessionClient) {
        this.scheduleRepository = scheduleRepository;
        this.trainerAvailabilityService = trainerAvailabilityService;
        this.sessionClient = sessionClient;
    }

    // ==================== CRUD ====================

    @Override
    public Schedule createSchedule(Schedule schedule) {
        validateScheduleTimes(schedule.getStartTime(), schedule.getEndTime());
        if (schedule.getStatus() == null) schedule.setStatus(ScheduleStatus.PENDING);
        return scheduleRepository.save(schedule);
    }

    @Override
    public Schedule updateSchedule(Long id, Schedule schedule) {
        Schedule existing = getSchedule(id);
        existing.setTitle(schedule.getTitle());
        existing.setNotes(schedule.getNotes());
        existing.setStartTime(schedule.getStartTime());
        existing.setEndTime(schedule.getEndTime());
        existing.setStatus(schedule.getStatus());
        existing.setType(schedule.getType());
        existing.setPlanningId(schedule.getPlanningId());
        validateScheduleTimes(existing.getStartTime(), existing.getEndTime());
        return scheduleRepository.save(existing);
    }

    @Override
    public Schedule getSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
    }

    @Override
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Override
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    // ==================== PLANNING RELATIONS ====================

    @Override
    public List<Schedule> getSchedulesByPlanningId(Long planningId) {
        return scheduleRepository.findByPlanningIdOrderByStartTimeAsc(planningId);
    }

    @Override
    public Schedule addScheduleToPlanning(Long planningId, Schedule schedule) {
        String trainerId = trainerAvailabilityService.getTrainerIdFromPlanning(planningId);
        if (trainerId == null) {
            throw new RuntimeException("Cannot determine trainer for this planning");
        }
        if (!trainerAvailabilityService.isTrainerAvailable(trainerId, schedule.getStartTime(), schedule.getEndTime())) {
            throw new RuntimeException("Trainer is not available at this time");
        }
        if (hasTimeConflictWithBuffer(planningId, schedule.getStartTime(), schedule.getEndTime())) {
            throw new RuntimeException("Time conflict detected with existing schedules (including buffer period)");
        }
        schedule.setPlanningId(planningId);
        schedule.setStatus(ScheduleStatus.PENDING);
        validateScheduleTimes(schedule.getStartTime(), schedule.getEndTime());
        return scheduleRepository.save(schedule);
    }

    @Override
    public void removeScheduleFromPlanning(Long scheduleId) {
        Schedule schedule = getSchedule(scheduleId);
        schedule.setPlanningId(null);
        scheduleRepository.save(schedule);
    }

    // ==================== ADVANCED FUNCTIONS ====================

    // 1. Buffer Time Check
    public boolean hasTimeConflictWithBuffer(Long planningId, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime bufferedStart = startTime.minusMinutes(bufferMinutes);
        LocalDateTime bufferedEnd   = endTime.plusMinutes(bufferMinutes);
        return getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED)
                .anyMatch(s -> bufferedStart.isBefore(s.getEndTime()) && bufferedEnd.isAfter(s.getStartTime()));
    }

    // 2. Suggest alternative slots — bounded by planning date range
    @Override
    public List<LocalDateTime> suggestAlternativeSlots(Long planningId, int durationHours, int maxSuggestions) {
        String trainerId = trainerAvailabilityService.getTrainerIdFromPlanning(planningId);
        if (trainerId == null) return new ArrayList<>();

        Map<String, Object> planning = sessionClient.getPlanningById(planningId);
        if (planning == null) return new ArrayList<>();

        LocalDate startDate = parseLocalDate(planning.get("startDate"));
        LocalDate endDate   = parseLocalDate(planning.get("endDate"));
        if (startDate == null || endDate == null) return new ArrayList<>();

        LocalDateTime rangeStart = startDate.atTime(WORK_START);
        LocalDateTime rangeEnd   = endDate.atTime(WORK_END);

        // Start from now (next full hour) or planning start, whichever is later
        LocalDateTime cursor = LocalDateTime.now().isAfter(rangeStart)
                ? LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1)
                : rangeStart;

        List<LocalDateTime> suggestions = new ArrayList<>();

        while (suggestions.size() < maxSuggestions && cursor.isBefore(rangeEnd)) {
            DayOfWeek dow = cursor.getDayOfWeek();

            // Skip weekends
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }

            // Snap to work start if before 09:00
            if (cursor.toLocalTime().isBefore(WORK_START)) {
                cursor = cursor.with(WORK_START);
                continue;
            }

            LocalDateTime end = cursor.plusHours(durationHours);

            // Skip if slot overflows working hours or planning end
            if (end.toLocalTime().isAfter(WORK_END) || end.isAfter(rangeEnd)) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }

            if (!hasTimeConflictWithBuffer(planningId, cursor, end) &&
                    trainerAvailabilityService.isTrainerAvailable(trainerId, cursor, end)) {
                suggestions.add(cursor);
            }

            cursor = cursor.plusHours(1);
        }

        return suggestions;
    }

    // 3. Recurring Sessions — FIX: days param was String in original; resolved via DayOfWeek
    @Override
    public List<Schedule> createRecurringSessions(Long planningId, Schedule template,
                                                  LocalDate startDate, int weeks,
                                                  List<DayOfWeek> days) {
        // Resolve trainer once, not per iteration
        String trainerId = trainerAvailabilityService.getTrainerIdFromPlanning(planningId);
        long durationMinutes = ChronoUnit.MINUTES.between(template.getStartTime(), template.getEndTime());
        List<Schedule> created = new ArrayList<>();

        for (int w = 0; w < weeks; w++) {
            for (DayOfWeek dow : days) {
                LocalDate sessionDate = startDate.plusWeeks(w).with(TemporalAdjusters.nextOrSame(dow));
                LocalDateTime start = sessionDate.atTime(template.getStartTime().toLocalTime());
                LocalDateTime end   = start.plusMinutes(durationMinutes);

                // Skip past slots
                if (!start.isAfter(LocalDateTime.now())) continue;

                if (hasTimeConflictWithBuffer(planningId, start, end)) continue;

                if (trainerId != null && !trainerAvailabilityService.isTrainerAvailable(trainerId, start, end)) continue;

                Schedule copy = new Schedule();
                copy.setTitle(template.getTitle());
                copy.setNotes(template.getNotes());
                copy.setStartTime(start);
                copy.setEndTime(end);
                copy.setType(template.getType());
                copy.setStatus(ScheduleStatus.PENDING);
                copy.setPlanningId(planningId);
                created.add(scheduleRepository.save(copy));
            }
        }
        return created;
    }

    // 4. Session Split — FIX: minimum was checked on total duration but split produced wrong secondEnd
    @Override
    public List<Schedule> splitSession(Long scheduleId, int breakMinutes) {
        Schedule original = getSchedule(scheduleId);
        long durationMinutes = ChronoUnit.MINUTES.between(original.getStartTime(), original.getEndTime());

        if (durationMinutes <= 60) {
            throw new RuntimeException("Session is too short to split (minimum 1 hour)");
        }
        // Each part must be at least 30 minutes after the break is removed
        if ((durationMinutes - breakMinutes) / 2 < 30) {
            throw new RuntimeException(
                    "Break is too long for this session — each part would be under 30 minutes");
        }

        long halfDuration     = (durationMinutes - breakMinutes) / 2;
        LocalDateTime firstEnd    = original.getStartTime().plusMinutes(halfDuration);
        LocalDateTime secondStart = firstEnd.plusMinutes(breakMinutes);
        // FIX: secondEnd was original.getEndTime() which ignored the break, extending total duration
        LocalDateTime secondEnd   = secondStart.plusMinutes(halfDuration);

        Schedule firstPart = buildCopy(original, original.getStartTime(), firstEnd, " (Partie 1)");
        Schedule secondPart = buildCopy(original, secondStart, secondEnd, " (Partie 2)");

        scheduleRepository.delete(original);
        return List.of(scheduleRepository.save(firstPart), scheduleRepository.save(secondPart));
    }

    // 5. Shift Future Sessions — FIX: negative offset was allowed with no guard; add validation
    @Override
    public void shiftFutureSessions(Long planningId, LocalDateTime fromDate, int offsetHours) {
        if (offsetHours == 0) return;

        List<Schedule> toShift = getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStartTime().isAfter(fromDate))
                .collect(Collectors.toList());

        for (Schedule s : toShift) {
            LocalDateTime newStart = s.getStartTime().plusHours(offsetHours);
            LocalDateTime newEnd   = s.getEndTime().plusHours(offsetHours);

            // Guard: do not shift into the past
            if (newStart.isBefore(LocalDateTime.now())) {
                log.warn("Skipping shift for schedule {} — would move to past", s.getId());
                continue;
            }

            s.setStartTime(newStart);
            s.setEndTime(newEnd);
        }

        scheduleRepository.saveAll(toShift);
    }

    // 6. iCal Export — FIX: LocalDateTime.toString() produces invalid iCal format (missing 'Z' / seconds)
    @Override
    public String exportToIcal(Long planningId) {
        List<Schedule> schedules = getSchedulesByPlanningId(planningId);
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//Training Platform//Schedule//EN\r\n")
                .append("CALSCALE:GREGORIAN\r\n");

        for (Schedule s : schedules) {
            ical.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(s.getId()).append("@training.com\r\n")
                    .append("DTSTAMP:").append(formatIcal(LocalDateTime.now())).append("\r\n")
                    .append("DTSTART:").append(formatIcal(s.getStartTime())).append("\r\n")
                    .append("DTEND:").append(formatIcal(s.getEndTime())).append("\r\n")
                    .append("SUMMARY:").append(escapeIcal(s.getTitle())).append("\r\n");
            if (s.getNotes() != null && !s.getNotes().isBlank()) {
                ical.append("DESCRIPTION:").append(escapeIcal(s.getNotes())).append("\r\n");
            }
            ical.append("STATUS:").append(mapStatus(s.getStatus())).append("\r\n")
                    .append("END:VEVENT\r\n");
        }
        ical.append("END:VCALENDAR\r\n");
        return ical.toString();
    }

    // FIX: format as YYYYMMDDTHHmmss — original produced invalid strings like "2025-06-0109:00"
    private String formatIcal(LocalDateTime dt) {
        return String.format("%04d%02d%02dT%02d%02d%02d",
                dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(),
                dt.getHour(), dt.getMinute(), dt.getSecond());
    }

    // FIX: iCal STATUS values are TENTATIVE/CONFIRMED/CANCELLED — not our internal enum names
    private String mapStatus(ScheduleStatus status) {
        return switch (status) {
            case CONFIRMED -> "CONFIRMED";
            case CANCELLED -> "CANCELLED";
            default        -> "TENTATIVE";
        };
    }

    private String escapeIcal(String text) {
        return text.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }

    // 7. Duplicate Schedule
    @Override
    public Schedule duplicateSchedule(Long scheduleId, LocalDateTime newStartTime) {
        Schedule original = getSchedule(scheduleId);
        long durationMinutes = ChronoUnit.MINUTES.between(original.getStartTime(), original.getEndTime());
        LocalDateTime newEndTime = newStartTime.plusMinutes(durationMinutes);

        if (hasTimeConflictWithBuffer(original.getPlanningId(), newStartTime, newEndTime)) {
            throw new RuntimeException("Time conflict detected for the new time slot");
        }
        String trainerId = trainerAvailabilityService.getTrainerIdFromPlanning(original.getPlanningId());
        if (trainerId != null && !trainerAvailabilityService.isTrainerAvailable(trainerId, newStartTime, newEndTime)) {
            throw new RuntimeException("Trainer not available at the new time");
        }

        return scheduleRepository.save(buildCopy(original, newStartTime, newEndTime, " (Copie)"));
    }

    // 8. Bulk Status Update
    @Override
    public void bulkUpdateStatus(List<Long> scheduleIds, ScheduleStatus status) {
        List<Schedule> schedules = scheduleRepository.findAllById(scheduleIds);
        schedules.forEach(s -> s.setStatus(status));
        scheduleRepository.saveAll(schedules);
    }

    // 9. Bulk Delete
    @Override
    public void bulkDelete(List<Long> scheduleIds) {
        scheduleRepository.deleteAllById(scheduleIds);
    }

    // ==================== ANALYTICS ====================

    @Override
    public Map<String, Object> getScheduleAnalytics(Long planningId) {
        List<Schedule> schedules = getSchedulesByPlanningId(planningId);

        long totalMinutes = schedules.stream()
                .mapToLong(s -> ChronoUnit.MINUTES.between(s.getStartTime(), s.getEndTime()))
                .sum();

        double averageDuration = schedules.stream()
                .mapToLong(s -> ChronoUnit.MINUTES.between(s.getStartTime(), s.getEndTime()))
                .average()
                .orElse(0.0) / 60.0; // FIX: was computing hours directly losing sub-hour precision

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalSchedules", schedules.size());
        analytics.put("totalHours", Math.round(totalMinutes / 6.0) / 10.0);
        analytics.put("averageDurationHours", Math.round(averageDuration * 10) / 10.0);
        analytics.put("statusDistribution",
                schedules.stream().collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting())));
        analytics.put("typeDistribution",
                schedules.stream().collect(Collectors.groupingBy(s -> s.getType().name(), Collectors.counting())));
        analytics.put("dailyDistribution",
                schedules.stream().collect(Collectors.groupingBy(
                        s -> s.getStartTime().getDayOfWeek().toString(), Collectors.counting())));
        return analytics;
    }

    @Override
    public Map<String, Object> getScheduleStatistics(Long planningId) {
        List<Schedule> schedules = getSchedulesByPlanningId(planningId);
        LocalDateTime now = LocalDateTime.now();

        long pending   = schedules.stream().filter(s -> s.getStatus() == ScheduleStatus.PENDING).count();
        long active    = schedules.stream().filter(s -> s.getStatus() == ScheduleStatus.ACTIVE).count();
        long confirmed = schedules.stream().filter(s -> s.getStatus() == ScheduleStatus.CONFIRMED).count();
        long cancelled = schedules.stream().filter(s -> s.getStatus() == ScheduleStatus.CANCELLED).count();
        long completed = confirmed + active;
        double completionRate = schedules.isEmpty() ? 0 : (completed * 100.0 / schedules.size());

        long upcoming = schedules.stream()
                .filter(s -> s.getStartTime().isAfter(now) && s.getStatus() != ScheduleStatus.CANCELLED)
                .count();

        // FIX: "delayed" = past start, not yet confirmed, not cancelled
        long delayed = schedules.stream()
                .filter(s -> s.getStartTime().isBefore(now)
                        && s.getStatus() == ScheduleStatus.PENDING)
                .count();

        long totalMinutes = schedules.stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED)
                .mapToLong(s -> ChronoUnit.MINUTES.between(s.getStartTime(), s.getEndTime()))
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSchedules", schedules.size());
        stats.put("pending", pending);
        stats.put("active", active);
        stats.put("confirmed", confirmed);
        stats.put("cancelled", cancelled);
        stats.put("completionRate", Math.round(completionRate));
        stats.put("upcomingSchedules", upcoming);
        stats.put("delayedSchedules", delayed);
        stats.put("totalHours", Math.round(totalMinutes / 6.0) / 10.0);
        return stats;
    }

    @Override
    public boolean hasTimeConflict(Long planningId, LocalDateTime startTime, LocalDateTime endTime) {
        return getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED)
                .anyMatch(s -> startTime.isBefore(s.getEndTime()) && endTime.isAfter(s.getStartTime()));
    }

    @Override
    public List<Schedule> findConflictingSchedules(Long planningId, LocalDateTime startTime, LocalDateTime endTime) {
        return getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED)
                .filter(s -> startTime.isBefore(s.getEndTime()) && endTime.isAfter(s.getStartTime()))
                .collect(Collectors.toList());
    }

    @Override
    public Schedule optimizeScheduleTime(Long scheduleId, LocalDateTime preferredStart) {
        Schedule schedule = getSchedule(scheduleId);
        Long planningId   = schedule.getPlanningId();
        // FIX: was truncating to hours, losing sessions shorter than 1 hour
        long durationMinutes = ChronoUnit.MINUTES.between(schedule.getStartTime(), schedule.getEndTime());

        LocalDateTime optimalStart = findOptimalStartTime(planningId, preferredStart, (int) durationMinutes);
        schedule.setStartTime(optimalStart);
        schedule.setEndTime(optimalStart.plusMinutes(durationMinutes));
        schedule.setStatus(ScheduleStatus.CONFIRMED);
        return scheduleRepository.save(schedule);
    }

    // FIX: autoSchedule and generateWeekly ignored planning date range and started from now
    @Override
    public List<Schedule> autoScheduleSessions(Long planningId, int daysNeeded) {
        Map<String, Object> planning = sessionClient.getPlanningById(planningId);
        if (planning == null) return new ArrayList<>();

        LocalDate startDate = parseLocalDate(planning.get("startDate"));
        LocalDate endDate   = parseLocalDate(planning.get("endDate"));
        if (startDate == null || endDate == null) return new ArrayList<>();

        LocalDateTime cursor = (LocalDateTime.now().toLocalDate().isAfter(startDate)
                ? LocalDateTime.now().toLocalDate()
                : startDate).atTime(WORK_START);

        List<Schedule> scheduled = new ArrayList<>();
        int day = 0;

        while (day < daysNeeded && cursor.toLocalDate().isBefore(endDate.plusDays(1))) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }

            LocalDateTime end = cursor.plusHours(7);
            if (end.toLocalTime().isAfter(WORK_END)) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }

            if (!hasTimeConflictWithBuffer(planningId, cursor, end)) {
                Schedule s = new Schedule();
                s.setPlanningId(planningId);
                s.setTitle("Session J" + (day + 1));
                s.setType(ScheduleType.LIVE);
                s.setStatus(ScheduleStatus.PENDING);
                s.setStartTime(cursor);
                s.setEndTime(end);
                s.setNotes("Auto-generated");
                scheduled.add(scheduleRepository.save(s));
                day++;
            }
            cursor = cursor.plusDays(1).with(WORK_START);
        }
        return scheduled;
    }

    @Override
    public Schedule updateScheduleStatus(Long scheduleId, ScheduleStatus status) {
        Schedule schedule = getSchedule(scheduleId);
        schedule.setStatus(status);
        return scheduleRepository.save(schedule);
    }

    @Override
    public void updateAllSchedulesStatus(Long planningId) {
        List<Schedule> schedules = getSchedulesByPlanningId(planningId);
        LocalDateTime now = LocalDateTime.now();

        schedules.forEach(s -> {
            if (s.getStatus() == ScheduleStatus.CANCELLED) return;
            if (s.getEndTime().isBefore(now))                          s.setStatus(ScheduleStatus.CONFIRMED);
            else if (s.getStartTime().isBefore(now))                   s.setStatus(ScheduleStatus.ACTIVE);
            else                                                        s.setStatus(ScheduleStatus.PENDING);
        });

        scheduleRepository.saveAll(schedules);
    }

    // FIX: generateWeekly now respects planning range + skips weekends properly
    @Override
    public List<Schedule> generateWeeklySchedule(Long planningId, int weeks) {
        Map<String, Object> planning = sessionClient.getPlanningById(planningId);
        if (planning == null) return new ArrayList<>();

        LocalDate startDate = parseLocalDate(planning.get("startDate"));
        LocalDate endDate   = parseLocalDate(planning.get("endDate"));
        if (startDate == null || endDate == null) return new ArrayList<>();

        // Clamp to the later of today and planning start
        LocalDate cursor = startDate.isBefore(LocalDate.now()) ? LocalDate.now() : startDate;
        List<Schedule> result = new ArrayList<>();
        int weekCount = 0;

        while (weekCount < weeks && !cursor.isAfter(endDate)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                LocalDateTime start = cursor.atTime(WORK_START);
                LocalDateTime end   = start.plusHours(7);

                if (!end.toLocalTime().isAfter(WORK_END) && !cursor.isAfter(endDate)
                        && !hasTimeConflictWithBuffer(planningId, start, end)) {
                    Schedule s = new Schedule();
                    s.setPlanningId(planningId);
                    s.setTitle("Semaine " + (weekCount + 1) + " - " + dow);
                    s.setType(ScheduleType.LIVE);
                    s.setStatus(ScheduleStatus.PENDING);
                    s.setStartTime(start);
                    s.setEndTime(end);
                    s.setNotes("Séance hebdomadaire");
                    result.add(scheduleRepository.save(s));
                }

                // Increment week counter at end of each Friday
                if (dow == DayOfWeek.FRIDAY) weekCount++;
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    @Override
    public Map<String, Object> calculateScheduleEfficiency(Long planningId) {
        List<Schedule> sorted = getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED)
                .sorted(Comparator.comparing(Schedule::getStartTime))
                .collect(Collectors.toList());

        long totalGapMinutes = 0;
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDateTime end       = sorted.get(i).getEndTime();
            LocalDateTime nextStart = sorted.get(i + 1).getStartTime();
            if (nextStart.isAfter(end)) {
                totalGapMinutes += ChronoUnit.MINUTES.between(end, nextStart);
            }
        }

        long totalPlannedMinutes = sorted.stream()
                .mapToLong(s -> ChronoUnit.MINUTES.between(s.getStartTime(), s.getEndTime()))
                .sum();

        // FIX: totalAvailableMinutes was hardcoded to 5 days — use actual planning span
        long totalAvailableMinutes = totalPlannedMinutes + totalGapMinutes;
        double utilizationRate = totalAvailableMinutes == 0 ? 0
                : Math.min(100, totalPlannedMinutes * 100.0 / totalAvailableMinutes);

        // FIX: original score could go negative; cap at 0
        double efficiencyScore = Math.max(0, 100 - (totalGapMinutes / 60.0));

        Map<String, Object> efficiency = new HashMap<>();
        efficiency.put("efficiencyScore", Math.round(efficiencyScore));
        efficiency.put("utilizationRate", Math.round(utilizationRate));
        efficiency.put("totalGapHours", Math.round(totalGapMinutes / 6.0) / 10.0);
        efficiency.put("recommendation", getEfficiencyRecommendation(efficiencyScore));
        return efficiency;
    }

    private String getEfficiencyRecommendation(double score) {
        if (score >= 80) return "Planning très efficace";
        if (score >= 60) return "Bon planning, quelques améliorations possibles";
        if (score >= 40) return "Planning à optimiser";
        return "Planning à restructurer significativement";
    }

    @Override
    public Map<String, Object> getUpcomingSchedules(Long planningId, int days) {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime until = now.plusDays(days);

        List<Schedule> upcoming = getSchedulesByPlanningId(planningId).stream()
                .filter(s -> s.getStartTime().isAfter(now)
                        && s.getStartTime().isBefore(until)
                        && s.getStatus() != ScheduleStatus.CANCELLED)
                .sorted(Comparator.comparing(Schedule::getStartTime))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("upcomingSchedules", upcoming);
        result.put("count", upcoming.size());
        result.put("nextSchedule", upcoming.isEmpty() ? null : upcoming.get(0));
        return result;
    }

    @Override
    public List<Schedule> bulkCreateSchedules(Long planningId, List<Schedule> schedules) {
        schedules.forEach(s -> {
            s.setPlanningId(planningId);
            if (s.getStatus() == null) s.setStatus(ScheduleStatus.PENDING);
        });
        return scheduleRepository.saveAll(schedules);
    }

    // FIX: suggestAvailableTimeSlots ignored weekends and work hours
    @Override
    public List<LocalDateTime> suggestAvailableTimeSlots(Long planningId, int durationHours, int daysAhead) {
        LocalDateTime cursor = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        LocalDateTime limit  = cursor.plusDays(daysAhead);
        List<LocalDateTime> slots = new ArrayList<>();

        while (cursor.isBefore(limit)) {
            DayOfWeek dow = cursor.getDayOfWeek();

            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }
            if (cursor.toLocalTime().isBefore(WORK_START)) {
                cursor = cursor.with(WORK_START);
                continue;
            }

            LocalDateTime end = cursor.plusHours(durationHours);

            if (end.toLocalTime().isAfter(WORK_END)) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }

            if (!hasTimeConflictWithBuffer(planningId, cursor, end)) {
                slots.add(cursor);
            }
            cursor = cursor.plusHours(1);
        }
        return slots;
    }

    @Override
    public Schedule generateScheduleFromTemplate(Long planningId, Long templateId) {
        Schedule template = getSchedule(templateId);
        long durationMinutes = ChronoUnit.MINUTES.between(template.getStartTime(), template.getEndTime());
        LocalDateTime start  = LocalDateTime.now().with(WORK_START).plusDays(1);

        Schedule newSchedule = new Schedule();
        newSchedule.setPlanningId(planningId);
        newSchedule.setTitle(template.getTitle());
        newSchedule.setType(template.getType());
        newSchedule.setNotes(template.getNotes());
        newSchedule.setStatus(ScheduleStatus.PENDING);
        newSchedule.setStartTime(start);
        newSchedule.setEndTime(start.plusMinutes(durationMinutes));
        return scheduleRepository.save(newSchedule);
    }

    @Override
    public String exportScheduleToJson(Long planningId) {
        return getSchedulesByPlanningId(planningId).toString();
    }

    @Override
    public void importScheduleFromJson(Long planningId, String jsonData) {
        // TODO: implement with ObjectMapper
    }

    // ==================== HELPERS ====================

    private void validateScheduleTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null)
            throw new RuntimeException("Start and end time cannot be null");
        if (!start.isBefore(end))
            throw new RuntimeException("Start time must be before end time");
        if (start.isBefore(LocalDateTime.now()))
            throw new RuntimeException("Cannot schedule in the past");
    }

    // FIX: findOptimalStartTime took durationHours but used it as hours — now takes minutes
    private LocalDateTime findOptimalStartTime(Long planningId, LocalDateTime preferredStart, int durationMinutes) {
        LocalDateTime candidate = preferredStart;
        for (int i = 0; i < 48; i++) { // search up to 48 hours ahead
            if (!hasTimeConflictWithBuffer(planningId, candidate, candidate.plusMinutes(durationMinutes))) {
                return candidate;
            }
            candidate = candidate.plusHours(1);
        }
        throw new RuntimeException("Could not find an available slot within 48 hours of the preferred time");
    }

    private Schedule buildCopy(Schedule original, LocalDateTime start, LocalDateTime end, String titleSuffix) {
        Schedule copy = new Schedule();
        copy.setTitle(original.getTitle() + titleSuffix);
        copy.setNotes(original.getNotes());
        copy.setStartTime(start);
        copy.setEndTime(end);
        copy.setType(original.getType());
        copy.setStatus(ScheduleStatus.PENDING);
        copy.setPlanningId(original.getPlanningId());
        return copy;
    }

    private LocalDate parseLocalDate(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String s) return LocalDate.parse(s);
        if (raw instanceof List<?> parts) {
            return LocalDate.of(
                    ((Number) parts.get(0)).intValue(),
                    ((Number) parts.get(1)).intValue(),
                    ((Number) parts.get(2)).intValue()
            );
        }
        log.warn("Unexpected date format from planning MS: {}", raw.getClass().getSimpleName());
        return null;
    }
}