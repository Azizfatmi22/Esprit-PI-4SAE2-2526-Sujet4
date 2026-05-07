package com.sessionmanagementservice.services;

import com.sessionmanagementservice.Repositories.*;
import com.sessionmanagementservice.Services.impl.PlanningServiceImpl;
import com.sessionmanagementservice.Services.interfaces.SessionService;
import com.sessionmanagementservice.entities.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningServiceImplEdgeCasesTest {

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
    private Planning planning;

    @BeforeEach
    void setUp() {
        session = new Session();
        session.setId(1L);
        session.setMaxParticipants(20);
        session.setCreatedAt(LocalDate.now().minusDays(5));

        location = new Location();
        location.setId(1L);
        location.setCapacity(30);
        location.setType(LocationType.ROOM);
        location.setName("Test Room");

        planning = new Planning();
        planning.setId((long)1);
        planning.setSession(session);
        planning.setLocation(location);
        planning.setStartDate(LocalDate.now().plusDays(1));
        planning.setEndDate(LocalDate.now().plusDays(3));
        planning.setTotalHours(35);
    }

    // ==================== DATE VALIDATION TESTS ====================

    @Test
    void shouldThrowWhenStartDateAfterEndDate() {
        Planning invalidPlanning = new Planning();
        invalidPlanning.setStartDate(LocalDate.now().plusDays(5));
        invalidPlanning.setEndDate(LocalDate.now().plusDays(1));

        // Remove the unnecessary mock - validation happens before session lookup
        // The exception is thrown before reaching sessionRepository.findById
        // when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class,
                () -> service.createPlanning(invalidPlanning, 1L, 1L));
    }

    @Test
    void shouldThrowWhenStartDateBeforeSessionCreation() {
        Planning invalidPlanning = new Planning();
        invalidPlanning.setStartDate(session.getCreatedAt().minusDays(1));
        invalidPlanning.setEndDate(session.getCreatedAt().plusDays(2));

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class,
                () -> service.createPlanning(invalidPlanning, 1L, 1L));
    }

    @Test
    void shouldThrowWhenDatesAreNull() {
        Planning invalidPlanning = new Planning();
        invalidPlanning.setStartDate(null);
        invalidPlanning.setEndDate(null);

        // Remove the unnecessary stub - validation fails first
        // when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class,
                () -> service.createPlanning(invalidPlanning, 1L, 1L));
    }

    // ==================== LOCATION DETERMINATION TESTS ====================

    @Test
    void shouldCreateOnlineLocationWhenPlatformUrlProvided() {
        Planning planningWithUrl = new Planning();
        planningWithUrl.setStartDate(LocalDate.now().plusDays(1));
        planningWithUrl.setEndDate(LocalDate.now().plusDays(3));

        Location onlineLocation = new Location();
        onlineLocation.setPlatformUrl("zoom.com");
        planningWithUrl.setLocation(onlineLocation);

        Planning savedPlanning = new Planning();
        savedPlanning.setId((long)1);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.findByPlatformUrl("zoom.com")).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenAnswer(i -> i.getArgument(0));
        when(planningRepository.save(any(Planning.class))).thenReturn(savedPlanning);

        Planning result = service.createPlanning(planningWithUrl, 1L, null);

        assertNotNull(result);
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void shouldUseExistingOnlineLocationWhenPlatformUrlExists() {
        Planning planningWithUrl = new Planning();
        planningWithUrl.setStartDate(LocalDate.now().plusDays(1));
        planningWithUrl.setEndDate(LocalDate.now().plusDays(3));

        Location existingOnline = new Location();
        existingOnline.setId(2L);
        existingOnline.setPlatformUrl("zoom.com");
        planningWithUrl.setLocation(existingOnline);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.findByPlatformUrl("zoom.com")).thenReturn(Optional.of(existingOnline));
        // Need to mock save for the planning, not for location
        when(planningRepository.save(any(Planning.class))).thenAnswer(i -> i.getArgument(0));

        Planning result = service.createPlanning(planningWithUrl, 1L, null);

        assertNotNull(result);
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldCreateUnassignedLocationWhenNoLocationProvided() {
        Planning planningWithoutLocation = new Planning();
        planningWithoutLocation.setStartDate(LocalDate.now().plusDays(1));
        planningWithoutLocation.setEndDate(LocalDate.now().plusDays(3));
        planningWithoutLocation.setLocation(null);

        Location unassignedLocation = new Location();
        unassignedLocation.setId(3L);

        Planning savedPlanning = new Planning();
        savedPlanning.setId((long)1);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.save(any(Location.class))).thenReturn(unassignedLocation);
        when(planningRepository.save(any(Planning.class))).thenReturn(savedPlanning);

        Planning result = service.createPlanning(planningWithoutLocation, 1L, null);

        assertNotNull(result);
        verify(locationRepository).save(any(Location.class));
    }

    // ==================== UPDATE PLANNING TESTS ====================

    @Test
    void shouldThrowWhenPlanningNotFoundForUpdate() {
        when(planningRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.updatePlanning(999, new Planning()));
    }

    @Test
    void shouldUpdatePlanningWithNewSession() {
        Session newSession = new Session();
        newSession.setId(2L);

        Planning updateData = new Planning();
        updateData.setSession(newSession);
        updateData.setStartDate(LocalDate.now().plusDays(1));
        updateData.setEndDate(LocalDate.now().plusDays(3));
        updateData.setTotalHours(40);

        when(planningRepository.findById(1)).thenReturn(Optional.of(planning));
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(newSession));
        when(planningRepository.save(any(Planning.class))).thenAnswer(i -> i.getArgument(0));

        Planning result = service.updatePlanning(1, updateData);

        assertNotNull(result);
        verify(planningRepository).save(any(Planning.class));
    }

    @Test
    void shouldUpdatePlanningWithLocationById() {
        Location newLocation = new Location();
        newLocation.setId(2L);

        Planning updateData = new Planning();
        updateData.setLocation(newLocation);
        updateData.setStartDate(LocalDate.now().plusDays(1));
        updateData.setEndDate(LocalDate.now().plusDays(3));

        when(planningRepository.findById(1)).thenReturn(Optional.of(planning));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(newLocation));
        when(planningRepository.save(any(Planning.class))).thenAnswer(i -> i.getArgument(0));

        Planning result = service.updatePlanning(1, updateData);

        assertNotNull(result);
        verify(planningRepository).save(any(Planning.class));
    }

    // ==================== SMART DATE TESTS ====================

    @Test
    void shouldSmartSuggestDateWithConflicts() {
        when(planningRepository.findConflictingPlannings(eq(1L), any(), any()))
                .thenReturn(List.of(new Planning())) // First date has conflict
                .thenReturn(List.of()); // Second date is free

        LocalDate result = service.smartSuggestDate(1L, LocalDate.now());

        assertNotNull(result);
    }

    @Test
    void shouldThrowWhenNoSmartDateFound() {
        when(planningRepository.findConflictingPlannings(eq(1L), any(), any()))
                .thenReturn(List.of(new Planning())); // Always return conflict

        assertThrows(RuntimeException.class,
                () -> service.smartSuggestDate(1L, LocalDate.now()));
    }

    // ==================== SUGGEST BEST LOCATION TESTS ====================

    @Test
    void shouldSuggestBestLocationByDate() {
        Location location1 = new Location();
        location1.setId(1L);
        Location location2 = new Location();
        location2.setId(2L);

        when(locationRepository.findAll()).thenReturn(List.of(location1, location2));
        when(planningRepository.countByLocationId(1L)).thenReturn(10L);
        when(planningRepository.countByLocationId(2L)).thenReturn(5L);

        Location result = service.suggestBestLocation(LocalDate.now());

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void shouldThrowWhenNoLocationsForSuggest() {
        when(locationRepository.findAll()).thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> service.suggestBestLocation(LocalDate.now()));
    }

    // ==================== FILL GAPS TESTS ====================

    @Test
    void shouldReturnExistingPlanningForFillGaps() {
        when(planningRepository.findFirstBySessionId(1L)).thenReturn(Optional.of(planning));

        Planning result = service.fillGaps(1L, 1L);

        assertNotNull(result);
    }

    @Test
    void shouldThrowWhenNoPlanningForFillGaps() {
        when(planningRepository.findFirstBySessionId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.fillGaps(1L, 1L));
    }

    // ==================== GET BUSY DAYS TESTS ====================

    @Test
    void shouldReturnEmptyBusyDaysWhenNoPlannings() {
        when(planningRepository.findByLocationId(1L)).thenReturn(List.of());

        Map<String, Object> result = service.getBusyDays(1L);

        assertEquals(0, result.get("totalDays"));
    }

    @Test
    void shouldCalculateBusyDaysWithOverbooked() {
        Planning planning1 = new Planning();
        planning1.setStartDate(LocalDate.now());
        planning1.setEndDate(LocalDate.now());
        planning1.setTotalHours(35);

        Planning planning2 = new Planning();
        planning2.setStartDate(LocalDate.now());
        planning2.setEndDate(LocalDate.now());
        planning2.setTotalHours(35);

        when(planningRepository.findByLocationId(1L)).thenReturn(List.of(planning1, planning2));

        Map<String, Object> result = service.getBusyDays(1L);

        assertNotNull(result);
        assertTrue(result.containsKey("overbookedDays"));
    }

    // ==================== HAS PLANNING CONFLICT TESTS ====================

    @Test
    void shouldReturnFalseWhenLocationIdIsNull() {
        boolean result = service.hasPlanningConflict(null, LocalDate.now(), LocalDate.now());
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenDatesAreNull() {
        boolean result = service.hasPlanningConflict(1L, null, null);
        assertFalse(result);
    }

    // ==================== IS VALID DAY TESTS ====================

    @Test
    void shouldReturnFalseWhenDateIsNull() {
        boolean result = service.isValidDay(null);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForSaturday() {
        LocalDate saturday = LocalDate.of(2026, 5, 9); // Saturday
        boolean result = service.isValidDay(saturday);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForSunday() {
        LocalDate sunday = LocalDate.of(2026, 5, 10); // Sunday
        boolean result = service.isValidDay(sunday);
        assertFalse(result);
    }

    @Test
    void shouldReturnTrueForMonday() {
        LocalDate monday = LocalDate.of(2026, 5, 11); // Monday
        boolean result = service.isValidDay(monday);
        assertTrue(result);
    }

    // ==================== DISTRIBUTE PLANNING TESTS ====================

    @Test
    void shouldThrowWhenSessionNotFoundForDistribute() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.distributePlanning(1L, 1L, 5));
    }

    @Test
    void shouldThrowWhenLocationNotFoundForDistribute() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.distributePlanning(1L, 1L, 5));
    }

    // ==================== OPTIMIZE PLANNING TESTS ====================

    @Test
    void shouldThrowWhenPlanningNotFoundForOptimize() {
        when(planningRepository.findFirstBySessionId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.optimizePlanning(1L));
    }

    @Test
    void shouldNotOptimizeWhenNoConflict() {
        when(planningRepository.findFirstBySessionId(1L)).thenReturn(Optional.of(planning));
        when(planningRepository.findConflictingPlannings(any(), any(), any()))
                .thenReturn(List.of());
        when(planningRepository.save(any(Planning.class))).thenAnswer(i -> i.getArgument(0));

        Planning result = service.optimizePlanning(1L);

        assertNotNull(result);
        verify(planningRepository, times(1)).save(any(Planning.class));
    }

    // ==================== DATE VALIDATION HELPER TESTS ====================

    @Test
    void shouldThrowWhenStartDateAfterEndDateInUpdate() {
        Planning invalidPlanning = new Planning();
        invalidPlanning.setStartDate(LocalDate.now().plusDays(5));
        invalidPlanning.setEndDate(LocalDate.now().plusDays(1));

        when(planningRepository.findById(1)).thenReturn(Optional.of(planning));

        assertThrows(RuntimeException.class,
                () -> service.updatePlanning(1, invalidPlanning));
    }

    // ==================== DELETE PLANNING TESTS ====================

    @Test
    void shouldDeletePlanning() {
        doNothing().when(planningRepository).deleteById(1);

        service.deletePlanning(1);

        verify(planningRepository).deleteById(1);
    }

    // ==================== FIND NEXT AVAILABLE DATE TESTS ====================

    @Test
    void shouldThrowWhenNoAvailableDateFound() {
        when(planningRepository.findConflictingPlannings(any(), any(), any()))
                .thenReturn(List.of(new Planning())); // Always conflict

        assertThrows(RuntimeException.class,
                () -> service.suggestNextAvailableDate(1L, LocalDate.now()));
    }

    // ==================== GENERATE PLANNING TESTS ====================

    @Test
    void shouldThrowWhenNoLocationAvailableForGenerate() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(locationRepository.findAll()).thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> service.generatePlanning(1L));
    }
}