package com.mspathandschedule.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.entities.ScheduleStatus;
import com.mspathandschedule.entities.ScheduleType;
import com.mspathandschedule.services.interfaces.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    private ObjectMapper objectMapper;
    private Schedule testSchedule;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(scheduleController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setTitle("Java Session");
        testSchedule.setNotes("Learn Java basics");
        testSchedule.setStatus(ScheduleStatus.PENDING);
        testSchedule.setType(ScheduleType.LIVE);
        testSchedule.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        testSchedule.setEndTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0));
        testSchedule.setPlanningId(100L);
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    void testCreateSchedule() throws Exception {
        when(scheduleService.createSchedule(any(Schedule.class))).thenReturn(testSchedule);

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSchedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Java Session"));

        verify(scheduleService, times(1)).createSchedule(any(Schedule.class));
    }

    @Test
    void testGetScheduleById() throws Exception {
        when(scheduleService.getSchedule(1L)).thenReturn(testSchedule);

        mockMvc.perform(get("/api/schedules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Java Session"));

        verify(scheduleService, times(1)).getSchedule(1L);
    }

    @Test
    void testGetAllSchedules() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule, new Schedule());
        when(scheduleService.getAllSchedules()).thenReturn(schedules);

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(scheduleService, times(1)).getAllSchedules();
    }

    @Test
    void testUpdateSchedule() throws Exception {
        when(scheduleService.updateSchedule(eq(1L), any(Schedule.class))).thenReturn(testSchedule);

        mockMvc.perform(put("/api/schedules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSchedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(scheduleService, times(1)).updateSchedule(eq(1L), any(Schedule.class));
    }

    @Test
    void testDeleteSchedule() throws Exception {
        doNothing().when(scheduleService).deleteSchedule(1L);

        mockMvc.perform(delete("/api/schedules/1"))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).deleteSchedule(1L);
    }

    @Test
    void testTestEndpoint() throws Exception {
        mockMvc.perform(get("/api/schedules/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("ScheduleController is working!"));
    }

    // ==================== PLANNING RELATIONS TESTS ====================

    @Test
    void testGetSchedulesByPlanningId() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleService.getSchedulesByPlanningId(100L)).thenReturn(schedules);

        mockMvc.perform(get("/api/schedules/planning/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).getSchedulesByPlanningId(100L);
    }

    @Test
    void testAddScheduleToPlanning() throws Exception {
        when(scheduleService.addScheduleToPlanning(eq(100L), any(Schedule.class))).thenReturn(testSchedule);

        mockMvc.perform(post("/api/schedules/planning/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSchedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningId").value(100L));

        verify(scheduleService, times(1)).addScheduleToPlanning(eq(100L), any(Schedule.class));
    }

    @Test
    void testRemoveFromPlanning() throws Exception {
        doNothing().when(scheduleService).removeScheduleFromPlanning(1L);

        mockMvc.perform(delete("/api/schedules/1/planning"))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).removeScheduleFromPlanning(1L);
    }

    // ==================== ANALYTICS TESTS ====================

    @Test
    void testGetAnalytics() throws Exception {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalSchedules", 5);
        analytics.put("totalHours", 12.5);

        when(scheduleService.getScheduleAnalytics(100L)).thenReturn(analytics);

        mockMvc.perform(get("/api/schedules/planning/100/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSchedules").value(5))
                .andExpect(jsonPath("$.totalHours").value(12.5));

        verify(scheduleService, times(1)).getScheduleAnalytics(100L);
    }

    @Test
    void testGetStatistics() throws Exception {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalSchedules", 10);
        statistics.put("completionRate", 75.0);

        when(scheduleService.getScheduleStatistics(100L)).thenReturn(statistics);

        mockMvc.perform(get("/api/schedules/planning/100/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSchedules").value(10));

        verify(scheduleService, times(1)).getScheduleStatistics(100L);
    }

    @Test
    void testGetEfficiency() throws Exception {
        Map<String, Object> efficiency = new HashMap<>();
        efficiency.put("efficiencyScore", 85);
        efficiency.put("utilizationRate", 75.5);

        when(scheduleService.calculateScheduleEfficiency(100L)).thenReturn(efficiency);

        mockMvc.perform(get("/api/schedules/planning/100/efficiency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.efficiencyScore").value(85));

        verify(scheduleService, times(1)).calculateScheduleEfficiency(100L);
    }

    @Test
    void testGetUpcomingSchedules() throws Exception {
        Map<String, Object> upcoming = new HashMap<>();
        upcoming.put("count", 3);
        upcoming.put("upcomingSchedules", new ArrayList<>());

        when(scheduleService.getUpcomingSchedules(eq(100L), eq(7))).thenReturn(upcoming);

        mockMvc.perform(get("/api/schedules/planning/100/upcoming")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        verify(scheduleService, times(1)).getUpcomingSchedules(eq(100L), eq(7));
    }

    // ==================== CONFLICT DETECTION TESTS ====================

    @Test
    void testHasConflict() throws Exception {
        when(scheduleService.hasTimeConflict(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        mockMvc.perform(get("/api/schedules/planning/100/conflict")
                        .param("start", LocalDateTime.now().plusDays(1).toString())
                        .param("end", LocalDateTime.now().plusDays(1).plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(scheduleService, times(1)).hasTimeConflict(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testFindConflicts() throws Exception {
        List<Schedule> conflicts = Arrays.asList(testSchedule);
        when(scheduleService.findConflictingSchedules(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(conflicts);

        mockMvc.perform(get("/api/schedules/planning/100/conflicts")
                        .param("start", LocalDateTime.now().plusDays(1).toString())
                        .param("end", LocalDateTime.now().plusDays(1).plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).findConflictingSchedules(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testHasConflictWithBuffer() throws Exception {
        when(scheduleService.hasTimeConflictWithBuffer(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        mockMvc.perform(get("/api/schedules/planning/100/conflict-buffer")
                        .param("start", LocalDateTime.now().plusDays(1).toString())
                        .param("end", LocalDateTime.now().plusDays(1).plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(scheduleService, times(1)).hasTimeConflictWithBuffer(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // ==================== SCHEDULE OPTIMIZATION TESTS ====================

    @Test
    void testOptimizeScheduleTime() throws Exception {
        when(scheduleService.optimizeScheduleTime(eq(1L), any(LocalDateTime.class)))
                .thenReturn(testSchedule);

        mockMvc.perform(post("/api/schedules/1/optimize")
                        .param("preferredStart", LocalDateTime.now().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(scheduleService, times(1)).optimizeScheduleTime(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void testAutoSchedule() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleService.autoScheduleSessions(eq(100L), eq(5))).thenReturn(schedules);

        mockMvc.perform(post("/api/schedules/planning/100/auto-schedule")
                        .param("daysNeeded", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).autoScheduleSessions(eq(100L), eq(5));
    }

    // ==================== STATUS MANAGEMENT TESTS ====================

    @Test
    void testUpdateScheduleStatus() throws Exception {
        testSchedule.setStatus(ScheduleStatus.CONFIRMED);
        when(scheduleService.updateScheduleStatus(eq(1L), eq(ScheduleStatus.CONFIRMED)))
                .thenReturn(testSchedule);

        mockMvc.perform(patch("/api/schedules/1/status")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(scheduleService, times(1)).updateScheduleStatus(eq(1L), eq(ScheduleStatus.CONFIRMED));
    }

    @Test
    void testUpdateAllStatuses() throws Exception {
        doNothing().when(scheduleService).updateAllSchedulesStatus(100L);

        mockMvc.perform(post("/api/schedules/planning/100/update-statuses"))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).updateAllSchedulesStatus(100L);
    }

    @Test
    void testGetByStatus() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleService.getSchedulesByPlanningId(100L)).thenReturn(schedules);

        mockMvc.perform(get("/api/schedules/planning/100/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).getSchedulesByPlanningId(100L);
    }

    // ==================== SCHEDULE GENERATION TESTS ====================

    @Test
    void testGenerateWeeklySchedule() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleService.generateWeeklySchedule(eq(100L), eq(4))).thenReturn(schedules);

        mockMvc.perform(post("/api/schedules/planning/100/generate-weekly")
                        .param("weeks", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).generateWeeklySchedule(eq(100L), eq(4));
    }

    @Test
    void testGenerateFromTemplate() throws Exception {
        when(scheduleService.generateScheduleFromTemplate(eq(100L), eq(50L)))
                .thenReturn(testSchedule);

        mockMvc.perform(post("/api/schedules/planning/100/generate-from-template/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(scheduleService, times(1)).generateScheduleFromTemplate(eq(100L), eq(50L));
    }

    // ==================== TIME SLOT SUGGESTIONS TESTS ====================

    @Test
    void testGetAvailableSlots() throws Exception {
        List<LocalDateTime> slots = Arrays.asList(
                LocalDateTime.now().plusDays(1).withHour(10),
                LocalDateTime.now().plusDays(1).withHour(14)
        );
        when(scheduleService.suggestAvailableTimeSlots(eq(100L), eq(2), eq(14)))
                .thenReturn(slots);

        mockMvc.perform(get("/api/schedules/planning/100/available-slots")
                        .param("durationHours", "2")
                        .param("daysAhead", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(scheduleService, times(1)).suggestAvailableTimeSlots(eq(100L), eq(2), eq(14));
    }

    @Test
    void testGetAlternativeSlots() throws Exception {
        List<LocalDateTime> slots = Arrays.asList(LocalDateTime.now().plusDays(1).withHour(11));
        when(scheduleService.suggestAlternativeSlots(eq(100L), eq(3), eq(5)))
                .thenReturn(slots);

        mockMvc.perform(get("/api/schedules/planning/100/alternative-slots")
                        .param("durationHours", "3")
                        .param("maxSuggestions", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).suggestAlternativeSlots(eq(100L), eq(3), eq(5));
    }

    // ==================== RECURRING SESSIONS TESTS ====================

    @Test
    void testCreateRecurringSessions() throws Exception {
        List<Schedule> recurringSessions = Arrays.asList(testSchedule, testSchedule);
        when(scheduleService.createRecurringSessions(anyLong(), any(Schedule.class), any(LocalDate.class), anyInt(), anyList()))
                .thenReturn(recurringSessions);

        List<DayOfWeek> days = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        mockMvc.perform(post("/api/schedules/planning/100/recurring")
                        .param("startDate", LocalDate.now().plusDays(1).toString())
                        .param("weeks", "4")
                        .param("days", "MONDAY", "WEDNESDAY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testSchedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(scheduleService, times(1)).createRecurringSessions(anyLong(), any(Schedule.class), any(LocalDate.class), anyInt(), anyList());
    }

    // ==================== SESSION MANAGEMENT TESTS ====================

    @Test
    void testSplitSession() throws Exception {
        List<Schedule> splitSessions = Arrays.asList(testSchedule, testSchedule);
        when(scheduleService.splitSession(eq(1L), eq(30))).thenReturn(splitSessions);

        mockMvc.perform(post("/api/schedules/1/split")
                        .param("breakMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(scheduleService, times(1)).splitSession(eq(1L), eq(30));
    }

    @Test
    void testShiftFutureSessions() throws Exception {
        doNothing().when(scheduleService).shiftFutureSessions(eq(100L), any(LocalDateTime.class), eq(2));

        mockMvc.perform(post("/api/schedules/planning/100/shift")
                        .param("fromDate", LocalDateTime.now().plusDays(1).toString())
                        .param("offsetHours", "2"))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).shiftFutureSessions(eq(100L), any(LocalDateTime.class), eq(2));
    }

    @Test
    void testDuplicateSchedule() throws Exception {
        when(scheduleService.duplicateSchedule(eq(1L), any(LocalDateTime.class)))
                .thenReturn(testSchedule);

        mockMvc.perform(post("/api/schedules/1/duplicate")
                        .param("newStartTime", LocalDateTime.now().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(scheduleService, times(1)).duplicateSchedule(eq(1L), any(LocalDateTime.class));
    }

    // ==================== EXPORT/IMPORT TESTS ====================

    @Test
    void testExportSchedule() throws Exception {
        String jsonExport = "[{\"id\":1,\"title\":\"Java Session\"}]";
        when(scheduleService.exportScheduleToJson(100L)).thenReturn(jsonExport);

        mockMvc.perform(get("/api/schedules/planning/100/export"))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonExport));

        verify(scheduleService, times(1)).exportScheduleToJson(100L);
    }

    @Test
    void testExportToIcal() throws Exception {
        String icalData = "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR";
        when(scheduleService.exportToIcal(100L)).thenReturn(icalData);

        mockMvc.perform(get("/api/schedules/planning/100/export/ical"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/calendar"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=schedule.ics"))
                .andExpect(content().string(icalData));

        verify(scheduleService, times(1)).exportToIcal(100L);
    }

    @Test
    void testImportSchedule() throws Exception {
        String jsonData = "[{\"title\":\"Imported Session\"}]";
        doNothing().when(scheduleService).importScheduleFromJson(eq(100L), eq(jsonData));

        mockMvc.perform(post("/api/schedules/planning/100/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonData))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).importScheduleFromJson(eq(100L), eq(jsonData));
    }

    // ==================== BULK OPERATIONS TESTS ====================

    @Test
    void testBulkCreate() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleService.bulkCreateSchedules(eq(100L), anyList())).thenReturn(schedules);

        mockMvc.perform(post("/api/schedules/planning/100/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedules)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(scheduleService, times(1)).bulkCreateSchedules(eq(100L), anyList());
    }

    @Test
    void testBulkUpdateStatus() throws Exception {
        List<Long> scheduleIds = Arrays.asList(1L, 2L, 3L);
        doNothing().when(scheduleService).bulkUpdateStatus(scheduleIds, ScheduleStatus.CONFIRMED);

        mockMvc.perform(patch("/api/schedules/bulk/status")
                        .param("scheduleIds", "1", "2", "3")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).bulkUpdateStatus(scheduleIds, ScheduleStatus.CONFIRMED);
    }

    @Test
    void testBulkDelete() throws Exception {
        List<Long> scheduleIds = Arrays.asList(1L, 2L, 3L);
        doNothing().when(scheduleService).bulkDelete(scheduleIds);

        mockMvc.perform(delete("/api/schedules/bulk")
                        .param("scheduleIds", "1", "2", "3"))
                .andExpect(status().isOk());

        verify(scheduleService, times(1)).bulkDelete(scheduleIds);
    }

    // ==================== SCHEDULE VALIDATION TESTS ====================

    @Test
    void testValidateSchedule() throws Exception {
        when(scheduleService.getSchedule(1L)).thenReturn(testSchedule);

        mockMvc.perform(get("/api/schedules/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.durationHours").value(2));

        verify(scheduleService, times(1)).getSchedule(1L);
    }

    // ==================== SCHEDULE SUMMARY TESTS ====================

    @Test
    void testGetSummary() throws Exception {
        List<Schedule> schedules = Arrays.asList(testSchedule);
        when(scheduleService.getSchedulesByPlanningId(100L)).thenReturn(schedules);

        mockMvc.perform(get("/api/schedules/planning/100/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSchedules").value(1))
                .andExpect(jsonPath("$.totalHours").value(2))
                .andExpect(jsonPath("$.pendingCount").value(1));

        verify(scheduleService, times(1)).getSchedulesByPlanningId(100L);
    }
}
