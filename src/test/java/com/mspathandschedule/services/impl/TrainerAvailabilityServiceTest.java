package com.mspathandschedule.services.impl;



import com.mspathandschedule.clients.SessionManagementFeignClient;
import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.repositories.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerAvailabilityServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SessionManagementFeignClient sessionClient;

    @InjectMocks
    private TrainerAvailabilityService trainerAvailabilityService;

    private Map<String, Object> testPlanning;
    private Map<String, Object> testSession;

    @BeforeEach
    void setUp() {
        testPlanning = new HashMap<>();
        testSession = new HashMap<>();
        testSession.put("trainerId", "trainer123");
        testPlanning.put("session", testSession);
    }

    @Test
    void getTrainerIdFromPlanning_Success() {
        // Arrange
        when(sessionClient.getPlanningById(100L)).thenReturn(testPlanning);

        // Act
        String trainerId = trainerAvailabilityService.getTrainerIdFromPlanning(100L);

        // Assert
        assertEquals("trainer123", trainerId);
    }

    @Test
    void getTrainerIdFromPlanning_Exception_ReturnsNull() {
        // Arrange
        when(sessionClient.getPlanningById(100L)).thenThrow(new RuntimeException("Connection error"));

        // Act
        String trainerId = trainerAvailabilityService.getTrainerIdFromPlanning(100L);

        // Assert
        assertNull(trainerId);
    }

    @Test
    void isTrainerAvailable_NoConflicts_ReturnsTrue() {
        // Arrange
        when(scheduleRepository.findByStartTimeBetween(any(), any())).thenReturn(new ArrayList<>());

        // Act
        boolean available = trainerAvailabilityService.isTrainerAvailable(
                "trainer123",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2)
        );

        // Assert
        assertTrue(available);
    }

    @Test
    void isTrainerAvailable_WithConflict_ReturnsFalse() {
        // Arrange
        Schedule conflictingSchedule = new Schedule();
        conflictingSchedule.setPlanningId(200L);

        when(scheduleRepository.findByStartTimeBetween(any(), any()))
                .thenReturn(Arrays.asList(conflictingSchedule));
        when(sessionClient.getPlanningById(200L)).thenReturn(testPlanning);

        // Act
        boolean available = trainerAvailabilityService.isTrainerAvailable(
                "trainer123",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2)
        );

        // Assert
        assertFalse(available);
    }

    @Test
    void suggestAlternativeSlots_ReturnsValidSlots() {
        // Arrange
        when(scheduleRepository.findByStartTimeBetween(any(), any())).thenReturn(new ArrayList<>());

        // Act
        List<LocalDateTime> slots = trainerAvailabilityService.suggestAlternativeSlots(
                "trainer123", 2, 5
        );

        // Assert
        assertNotNull(slots);
        assertTrue(slots.size() <= 5);

        for (LocalDateTime slot : slots) {
            // Should be within working hours
            int hour = slot.getHour();
            assertTrue(hour >= 9 && hour <= 16); // 2-hour session must end by 18:00
        }
    }
}
