package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.EnrollmentSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAccessServiceTest {

    @Mock
    private CourseClient courseClient;

    @Mock
    private EnrollmentClient enrollmentClient;

    @InjectMocks
    private SessionAccessService sessionAccessService;

    private CourseDTO sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = new CourseDTO();
        sampleCourse.setId(1L);
        sampleCourse.setTrainerId("trainer-123");
    }

    @Test
    void findCourseTrainerId_NullCourseId() {
        assertTrue(sessionAccessService.findCourseTrainerId(null).isEmpty());
    }

    @Test
    void findCourseTrainerId_CourseNotFound() {
        when(courseClient.getCourseById(1L)).thenReturn(null);
        assertTrue(sessionAccessService.findCourseTrainerId(1L).isEmpty());
    }

    @Test
    void findCourseTrainerId_NoTrainerId() {
        sampleCourse.setTrainerId(null);
        when(courseClient.getCourseById(1L)).thenReturn(sampleCourse);
        assertTrue(sessionAccessService.findCourseTrainerId(1L).isEmpty());

        sampleCourse.setTrainerId("  ");
        assertTrue(sessionAccessService.findCourseTrainerId(1L).isEmpty());
    }

    @Test
    void findCourseTrainerId_Success() {
        when(courseClient.getCourseById(1L)).thenReturn(sampleCourse);
        Optional<String> result = sessionAccessService.findCourseTrainerId(1L);
        assertTrue(result.isPresent());
        assertEquals("trainer-123", result.get());
    }

    @Test
    void findCourseTrainerId_Error() {
        when(courseClient.getCourseById(anyLong())).thenThrow(new RuntimeException("API error"));
        assertTrue(sessionAccessService.findCourseTrainerId(1L).isEmpty());
    }

    @Test
    void isCourseTrainer_InvalidUserId() {
        assertFalse(sessionAccessService.isCourseTrainer(1L, null));
        assertFalse(sessionAccessService.isCourseTrainer(1L, " "));
    }

    @Test
    void isCourseTrainer_NoTrainerFound() {
        when(courseClient.getCourseById(1L)).thenReturn(null);
        assertFalse(sessionAccessService.isCourseTrainer(1L, "user-1"));
    }

    @Test
    void isCourseTrainer_True() {
        when(courseClient.getCourseById(1L)).thenReturn(sampleCourse);
        assertTrue(sessionAccessService.isCourseTrainer(1L, "trainer-123"));
    }

    @Test
    void isCourseTrainer_False() {
        when(courseClient.getCourseById(1L)).thenReturn(sampleCourse);
        assertFalse(sessionAccessService.isCourseTrainer(1L, "other-user"));
    }

    @Test
    void hasPaidEnrollment_InvalidParams() {
        assertFalse(sessionAccessService.hasPaidEnrollment(null, "l1"));
        assertFalse(sessionAccessService.hasPaidEnrollment(1L, null));
        assertFalse(sessionAccessService.hasPaidEnrollment(1L, ""));
    }

    @Test
    void hasPaidEnrollment_ActiveRecord() {
        EnrollmentSummary summary = new EnrollmentSummary();
        summary.setCourseId(1L);
        summary.setStatus("ACTIVE");
        
        when(enrollmentClient.getEnrollmentsByLearner("l1")).thenReturn(Arrays.asList(summary));
        
        assertTrue(sessionAccessService.hasPaidEnrollment(1L, "l1"));
    }

    @Test
    void hasPaidEnrollment_CompletedRecord() {
        EnrollmentSummary summary = new EnrollmentSummary();
        summary.setCourseId(1L);
        summary.setStatus("COMPLETED");
        
        when(enrollmentClient.getEnrollmentsByLearner("l1")).thenReturn(Arrays.asList(summary));
        
        assertTrue(sessionAccessService.hasPaidEnrollment(1L, "l1"));
    }

    @Test
    void hasPaidEnrollment_FallbackSuccess() {
        // Records found but no match for courseId
        EnrollmentSummary other = new EnrollmentSummary();
        other.setCourseId(99L);
        when(enrollmentClient.getEnrollmentsByLearner("l1")).thenReturn(Arrays.asList(other));
        
        // Fallback returns true
        when(enrollmentClient.hasCourseAccess("l1", 1L)).thenReturn(true);
        
        assertTrue(sessionAccessService.hasPaidEnrollment(1L, "l1"));
    }

    @Test
    void hasPaidEnrollment_Failure() {
        when(enrollmentClient.getEnrollmentsByLearner("l1")).thenReturn(Collections.emptyList());
        when(enrollmentClient.hasCourseAccess("l1", 1L)).thenReturn(false);
        
        assertFalse(sessionAccessService.hasPaidEnrollment(1L, "l1"));
    }

    @Test
    void hasPaidEnrollment_ExceptionHandling() {
        // Exception in records triggers fallback
        when(enrollmentClient.getEnrollmentsByLearner(anyString())).thenThrow(new RuntimeException("DB error"));
        when(enrollmentClient.hasCourseAccess("l1", 1L)).thenReturn(true);
        
        assertTrue(sessionAccessService.hasPaidEnrollment(1L, "l1"));

        // Exception in fallback returns false
        when(enrollmentClient.hasCourseAccess("l1", 1L)).thenThrow(new RuntimeException("API error"));
        assertFalse(sessionAccessService.hasPaidEnrollment(1L, "l1"));
    }
}
