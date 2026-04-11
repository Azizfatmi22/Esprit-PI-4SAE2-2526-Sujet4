package com.mspathandschedule.services.impl;



import com.mspathandschedule.clients.SessionManagementFeignClient;
import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.entities.ScheduleStatus;
import com.mspathandschedule.entities.ScheduleType;
import com.mspathandschedule.repositories.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TrainerAvailabilityService trainerAvailabilityService;

    @Mock
    private SessionManagementFeignClient sessionClient;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Schedule testSchedule;
    private Map<String, Object> testPlanning;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduleService, "bufferMinutes", 30);

        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setTitle("Java Session");
        testSchedule.setStatus(ScheduleStatus.PENDING);
        testSchedule.setType(ScheduleType.LIVE);
        testSchedule.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        testSchedule.setEndTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0));
        testSchedule.setPlanningId(100L);

        testPlanning = new HashMap<>();
        testPlanning.put("id", 100L);
        testPlanning.put("startDate", LocalDateTime.now().plusDays(1).toLocalDate().toString());
        testPlanning.put("endDate", LocalDateTime.now().plusDays(30).toLocalDate().toString());
    }

    @Test
    void createSchedule_Success() {
        // Arrange
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        // Act
        Schedule result = scheduleService.createSchedule(testSchedule);

        // Assert
        assertNotNull(result);
        assertEquals(ScheduleStatus.PENDING, result.getStatus());
        verify(scheduleRepository, times(1)).save(testSchedule);
    }

    @Test
    void createSchedule_PastTime_ThrowsException() {
        // Arrange
        testSchedule.setStartTime(LocalDateTime.now().minusDays(1));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scheduleService.createSchedule(testSchedule));
        assertEquals("Cannot schedule in the past", exception.getMessage());
    }

    @Test
    void getSchedule_NotFound_ThrowsException() {
        // Arrange
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scheduleService.getSchedule(999L));
        assertTrue(exception.getMessage().contains("Schedule not found"));
    }

    @Test
    void addScheduleToPlanning_Success() {
        // Arrange
        when(trainerAvailabilityService.getTrainerIdFromPlanning(100L)).thenReturn("trainer123");
        when(trainerAvailabilityService.isTrainerAvailable(eq("trainer123"), any(), any())).thenReturn(true);
        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(new ArrayList<>());
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        // Act
        Schedule result = scheduleService.addScheduleToPlanning(100L, testSchedule);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getPlanningId());
    }

    @Test
    void addScheduleToPlanning_TrainerNotAvailable_ThrowsException() {
        // Arrange
        when(trainerAvailabilityService.getTrainerIdFromPlanning(100L)).thenReturn("trainer123");
        when(trainerAvailabilityService.isTrainerAvailable(eq("trainer123"), any(), any())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scheduleService.addScheduleToPlanning(100L, testSchedule));
        assertEquals("Trainer is not available at this time", exception.getMessage());
    }

    @Test
    void hasTimeConflictWithBuffer_DetectsConflict() {
        // Arrange
        Schedule existingSchedule = new Schedule();
        existingSchedule.setStartTime(testSchedule.getStartTime().plusMinutes(15));
        existingSchedule.setEndTime(testSchedule.getEndTime().minusMinutes(15));
        existingSchedule.setStatus(ScheduleStatus.CONFIRMED);

        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L))
                .thenReturn(Arrays.asList(existingSchedule));

        // Act
        boolean hasConflict = scheduleService.hasTimeConflictWithBuffer(
                100L,
                testSchedule.getStartTime(),
                testSchedule.getEndTime()
        );

        // Assert
        assertTrue(hasConflict);
    }

    @Test
    void suggestAlternativeSlots_ReturnsSuggestions() {
        // Arrange
        when(trainerAvailabilityService.getTrainerIdFromPlanning(100L)).thenReturn("trainer123");
        when(sessionClient.getPlanningById(100L)).thenReturn(testPlanning);
        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(new ArrayList<>());
        when(trainerAvailabilityService.isTrainerAvailable(eq("trainer123"), any(), any())).thenReturn(true);

        // Act
        List<LocalDateTime> suggestions = scheduleService.suggestAlternativeSlots(100L, 2, 3);

        // Assert
        assertNotNull(suggestions);
        assertTrue(suggestions.size() <= 3);
    }

    @Test
    void splitSession_Success() {
        // Arrange
        Schedule originalSchedule = new Schedule();
        originalSchedule.setId(1L);
        originalSchedule.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        originalSchedule.setEndTime(LocalDateTime.now().plusDays(1).withHour(13).withMinute(0));
        originalSchedule.setTitle("Long Session");

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(originalSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Schedule> splitSessions = scheduleService.splitSession(1L, 30);

        // Assert
        assertEquals(2, splitSessions.size());
        assertTrue(splitSessions.get(0).getTitle().contains("Partie 1"));
        assertTrue(splitSessions.get(1).getTitle().contains("Partie 2"));
        verify(scheduleRepository, times(1)).delete(originalSchedule);
    }

    @Test
    void splitSession_TooShort_ThrowsException() {
        // Arrange
        Schedule shortSession = new Schedule();
        shortSession.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        shortSession.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(45));

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(shortSession));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> scheduleService.splitSession(1L, 30));
        assertTrue(exception.getMessage().contains("too short"));
    }

    @Test
    void shiftFutureSessions_Success() {
        // Arrange
        Schedule futureSchedule = new Schedule();
        futureSchedule.setId(2L);
        futureSchedule.setStartTime(LocalDateTime.now().plusDays(5));
        futureSchedule.setEndTime(LocalDateTime.now().plusDays(5).plusHours(2));

        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L))
                .thenReturn(Arrays.asList(futureSchedule));
        when(scheduleRepository.saveAll(anyList())).thenReturn(Arrays.asList(futureSchedule));

        // Act
        scheduleService.shiftFutureSessions(100L, LocalDateTime.now().plusDays(2), 3);

        // Assert
        verify(scheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void duplicateSchedule_Success() {
        // Arrange
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(new ArrayList<>());
        when(trainerAvailabilityService.getTrainerIdFromPlanning(100L)).thenReturn("trainer123");
        when(trainerAvailabilityService.isTrainerAvailable(eq("trainer123"), any(), any())).thenReturn(true);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);

        // Act
        Schedule duplicated = scheduleService.duplicateSchedule(1L, newStartTime);

        // Assert
        assertNotNull(duplicated);
        assertTrue(duplicated.getTitle().contains("Copie"));
        assertEquals(newStartTime, duplicated.getStartTime());
    }

    @Test
    void bulkUpdateStatus_Success() {
        // Arrange
        List<Long> scheduleIds = Arrays.asList(1L, 2L, 3L);
        List<Schedule> schedules = Arrays.asList(
                createScheduleWithId(1L),
                createScheduleWithId(2L),
                createScheduleWithId(3L)
        );

        when(scheduleRepository.findAllById(scheduleIds)).thenReturn(schedules);
        when(scheduleRepository.saveAll(anyList())).thenReturn(schedules);

        // Act
        scheduleService.bulkUpdateStatus(scheduleIds, ScheduleStatus.CONFIRMED);

        // Assert
        verify(scheduleRepository, times(1)).saveAll(anyList());
        schedules.forEach(s -> assertEquals(ScheduleStatus.CONFIRMED, s.getStatus()));
    }

    @Test
    void getScheduleAnalytics_ReturnsCorrectData() {
        // Arrange
        List<Schedule> schedules = Arrays.asList(
                createScheduleWithDuration(2),
                createScheduleWithDuration(3),
                createScheduleWithDuration(1)
        );

        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(schedules);

        // Act
        Map<String, Object> analytics = scheduleService.getScheduleAnalytics(100L);

        // Assert
        assertNotNull(analytics);
        assertEquals(3, analytics.get("totalSchedules"));
        assertNotNull(analytics.get("totalHours"));
        assertNotNull(analytics.get("statusDistribution"));
        assertNotNull(analytics.get("typeDistribution"));
    }

    @Test
    void calculateScheduleEfficiency_ReturnsScore() {
        // Arrange
        List<Schedule> schedules = Arrays.asList(
                createScheduleWithTime(LocalDateTime.now().plusDays(1).withHour(9), LocalDateTime.now().plusDays(1).withHour(12)),
                createScheduleWithTime(LocalDateTime.now().plusDays(1).withHour(13), LocalDateTime.now().plusDays(1).withHour(15))
        );

        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(schedules);

        // Act
        Map<String, Object> efficiency = scheduleService.calculateScheduleEfficiency(100L);

        // Assert
        assertNotNull(efficiency);
        assertTrue(efficiency.containsKey("efficiencyScore"));
        assertTrue(efficiency.containsKey("utilizationRate"));
    }

    @Test
    void generateWeeklySchedule_RespectsWorkingHours() {
        // Arrange
        when(sessionClient.getPlanningById(100L)).thenReturn(testPlanning);
        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(new ArrayList<>());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Schedule> weeklySchedules = scheduleService.generateWeeklySchedule(100L, 2);

        // Assert
        assertNotNull(weeklySchedules);
        for (Schedule schedule : weeklySchedules) {
            assertFalse(schedule.getStartTime().getDayOfWeek() == DayOfWeek.SATURDAY ||
                    schedule.getStartTime().getDayOfWeek() == DayOfWeek.SUNDAY);
            assertTrue(schedule.getStartTime().toLocalTime().isAfter(LocalTime.of(8, 59)));
            assertTrue(schedule.getEndTime().toLocalTime().isBefore(LocalTime.of(18, 1)));
        }
    }

    @Test
    void exportToIcal_ReturnsValidFormat() {
        // Arrange
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleRepository.findByPlanningIdOrderByStartTimeAsc(100L)).thenReturn(schedules);

        // Act
        String ical = scheduleService.exportToIcal(100L);

        // Assert
        assertNotNull(ical);
        assertTrue(ical.startsWith("BEGIN:VCALENDAR"));
        assertTrue(ical.contains("BEGIN:VEVENT"));
        assertTrue(ical.contains("END:VEVENT"));
        assertTrue(ical.contains("END:VCALENDAR"));
        assertTrue(ical.contains("DTSTART:"));
        assertTrue(ical.contains("DTEND:"));
    }

    // Helper methods
    private Schedule createScheduleWithId(Long id) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setStatus(ScheduleStatus.PENDING);
        return schedule;
    }

    private Schedule createScheduleWithDuration(int hours) {
        Schedule schedule = new Schedule();
        schedule.setStartTime(LocalDateTime.now());
        schedule.setEndTime(LocalDateTime.now().plusHours(hours));
        schedule.setStatus(ScheduleStatus.CONFIRMED);
        schedule.setType(ScheduleType.LIVE);
        return schedule;
    }

    private Schedule createScheduleWithTime(LocalDateTime start, LocalDateTime end) {
        Schedule schedule = new Schedule();
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setStatus(ScheduleStatus.CONFIRMED);
        return schedule;
    }
}
