package com.sessionmanagementservice.services;


import com.sessionmanagementservice.Repositories.*;
import com.sessionmanagementservice.Services.impl.PlanningServiceImpl;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningServiceImplAdvancedTest {

    @Mock
    private PlanningRepository planningRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private PlanningServiceImpl service;

    private Session session;
    private Location location;

    @BeforeEach
    void init() {
        session = new Session();
        session.setId(1L);
        session.setCreatedAt(LocalDate.now().minusDays(1));
        session.setMaxParticipants(20);

        location = new Location();
        location.setId(1L);
        location.setCapacity(30);
        location.setType(LocationType.ROOM);
    }

    // ✅ GENERATE PLANNING (core logic)
    @Test
    void shouldGeneratePlanningWithoutConflict() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.findAll()).thenReturn(List.of(location));
        when(planningRepository.countByLocationId(anyLong())).thenReturn(0L);
        when(planningRepository.findConflictingPlannings(any(), any(), any()))
                .thenReturn(List.of());

        Planning result = service.generatePlanning(1L);

        assertNotNull(result);
        assertEquals(session, result.getSession());
        assertEquals(location, result.getLocation());
    }

    // ❌ GENERATE FAIL (no location)
    @Test
    void shouldThrowIfNoLocationAvailable() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.findAll()).thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> service.generatePlanning(1L));
    }

    // ✅ OPTIMIZE WITH CONFLICT
    @Test
    void shouldOptimizePlanningWhenConflictExists() {
        Planning planning = new Planning();

        planning.setSession(session);
        planning.setLocation(location);
        planning.setStartDate(LocalDate.now().plusDays(1));
        planning.setEndDate(LocalDate.now().plusDays(3));

        when(planningRepository.findFirstBySessionId(1L))
                .thenReturn(Optional.of(planning));

        when(planningRepository.findConflictingPlannings(any(), any(), any()))
                .thenReturn(List.of(new Planning())); // conflict

        when(planningRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Planning result = service.optimizePlanning(1L);

        assertNotNull(result);
        assertTrue(result.getEndDate().isAfter(result.getStartDate())
                || result.getEndDate().isEqual(result.getStartDate()));
    }

    // ✅ MAINTAIN ROLLING
    @Test
    void shouldExtendPlanning() {
        Planning planning = new Planning();
        planning.setEndDate(LocalDate.now().plusDays(2));
        planning.setTotalHours(35);
        planning.setLocation(location);

        when(planningRepository.findFirstBySessionId(1L))
                .thenReturn(Optional.of(planning));

        when(planningRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Planning result = service.maintainRollingPlanning(1L, 1L, 2);

        assertEquals(planning.getTotalHours(), result.getTotalHours());
        assertTrue(result.getEndDate().isAfter(planning.getEndDate()));
    }

    // ❌ ROLLING FAIL (conflict)
    @Test
    void shouldThrowWhenRollingConflict() {
        Planning planning = new Planning();
        planning.setEndDate(LocalDate.now().plusDays(1));
        planning.setLocation(location);

        when(planningRepository.findFirstBySessionId(1L))
                .thenReturn(Optional.of(planning));

        when(planningRepository.findConflictingPlannings(any(), any(), any()))
                .thenReturn(List.of(new Planning())); // conflict

        assertThrows(RuntimeException.class,
                () -> service.maintainRollingPlanning(1L, 1L, 2));
    }

    // ✅ SMART DATE
    @Test
    void shouldReturnNextValidDate() {
        when(planningRepository.findConflictingPlannings(any(), any(), any()))
                .thenReturn(List.of());

        LocalDate result = service.smartSuggestDate(1L, LocalDate.now());

        assertNotNull(result);
    }

    // ✅ VALID DAY
    @Test
    void shouldDetectWeekend() {
        LocalDate saturday = LocalDate.of(2026, 5, 9);

        assertFalse(service.isValidDay(saturday));
    }

    // ✅ HIGH RISK
    @Test
    void shouldDetectHighRiskPlanning() {
        Planning planning = new Planning();

        planning.setSession(session);
        planning.setLocation(location);
        planning.setStartDate(LocalDate.now().plusDays(1));
        planning.setEndDate(LocalDate.now().plusDays(1));
        planning.setTotalHours(100); // heavy load

        when(planningRepository.findFirstBySessionId(1L))
                .thenReturn(Optional.of(planning));

        when(planningRepository.findByLocationIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.isHighRiskPlanning(1L);

        assertTrue((Integer) result.get("riskScore") > 0);
    }

    // ✅ BUSY DAYS
    @Test
    void shouldCalculateBusyDays() {
        Planning planning = new Planning();
        planning.setStartDate(LocalDate.now());
        planning.setEndDate(LocalDate.now().plusDays(1));
        planning.setTotalHours(10);

        when(planningRepository.findByLocationId(1L))
                .thenReturn(List.of(planning));

        Map<String, Object> result = service.getBusyDays(1L);

        assertTrue(result.containsKey("days"));
        assertTrue((int) result.get("totalDays") > 0);
    }
}