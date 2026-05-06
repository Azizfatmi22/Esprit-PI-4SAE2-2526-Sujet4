package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.EnrollmentSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAccessServiceTest {

    @Mock
    private CourseClient courseClient;

    @Mock
    private EnrollmentClient enrollmentClient;

    private SessionAccessService sessionAccessService;

    @BeforeEach
    void setUp() {
        sessionAccessService = new SessionAccessService(courseClient, enrollmentClient);
    }

    @Test
    void findCourseTrainerId_Success() {
        Long courseId = 1L;
        CourseDTO course = new CourseDTO();
        course.setTrainerId("trainer123");
        
        when(courseClient.getCourseById(courseId)).thenReturn(course);

        Optional<String> result = sessionAccessService.findCourseTrainerId(courseId);

        assertTrue(result.isPresent());
        assertEquals("trainer123", result.get());
        verify(courseClient).getCourseById(courseId);
    }

    @Test
    void findCourseTrainerId_NullId() {
        Optional<String> result = sessionAccessService.findCourseTrainerId(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findCourseTrainerId_NotFound() {
        when(courseClient.getCourseById(1L)).thenReturn(null);
        Optional<String> result = sessionAccessService.findCourseTrainerId(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void isCourseTrainer_True() {
        Long courseId = 1L;
        String userId = "trainer123";
        CourseDTO course = new CourseDTO();
        course.setTrainerId(userId);
        
        when(courseClient.getCourseById(courseId)).thenReturn(course);

        boolean result = sessionAccessService.isCourseTrainer(courseId, userId);

        assertTrue(result);
    }

    @Test
    void isCourseTrainer_False() {
        Long courseId = 1L;
        String userId = "wrongUser";
        CourseDTO course = new CourseDTO();
        course.setTrainerId("trainer123");
        
        when(courseClient.getCourseById(courseId)).thenReturn(course);

        boolean result = sessionAccessService.isCourseTrainer(courseId, userId);

        assertFalse(result);
    }

    @Test
    void hasPaidEnrollment_Success() {
        Long courseId = 1L;
        String learnerId = "learner123";
        EnrollmentSummary enrollment = new EnrollmentSummary();
        enrollment.setCourseId(courseId);
        enrollment.setStatus("ACTIVE");
        
        when(enrollmentClient.getEnrollmentsByLearner(learnerId)).thenReturn(List.of(enrollment));

        boolean result = sessionAccessService.hasPaidEnrollment(courseId, learnerId);

        assertTrue(result);
    }

    @Test
    void hasPaidEnrollment_FallbackAccess() {
        Long courseId = 1L;
        String learnerId = "learner123";
        
        when(enrollmentClient.getEnrollmentsByLearner(learnerId)).thenThrow(new RuntimeException("Service down"));
        when(enrollmentClient.hasCourseAccess(learnerId, courseId)).thenReturn(true);

        boolean result = sessionAccessService.hasPaidEnrollment(courseId, learnerId);

        assertTrue(result);
    }

    @Test
    void hasPaidEnrollment_NoAccess() {
        Long courseId = 1L;
        String learnerId = "learner123";
        
        when(enrollmentClient.getEnrollmentsByLearner(learnerId)).thenReturn(Collections.emptyList());
        when(enrollmentClient.hasCourseAccess(learnerId, courseId)).thenReturn(false);

        boolean result = sessionAccessService.hasPaidEnrollment(courseId, learnerId);

        assertFalse(result);
    }
}
