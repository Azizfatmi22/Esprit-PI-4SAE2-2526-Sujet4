package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.Enrollment;
import tn.esprit.mucroservice.msenrollment.repositories.EnrollmentRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    // =========================
    // 1. CREATE ENROLLMENT
    // =========================
    @Test
    void shouldCreateEnrollmentSuccessfully() {

        Enrollment enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setLearnerId("L1");
        enrollment.setCourseId(10L);
        enrollment.setEnrolledDate(new Date());
        enrollment.setProgress(0.0);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.save(any()))
                .thenReturn(enrollment);

        Enrollment result = enrollmentService.createEnrollment("L1", 10L);

        assertNotNull(result);
        assertEquals("L1", result.getLearnerId());
        assertEquals(10L, result.getCourseId());
        assertEquals(Enrollment.EnrollmentStatus.ACTIVE, result.getStatus());

        verify(enrollmentRepository).save(any());
    }

    // =========================
    // 2. GET BY LEARNER
    // =========================
    @Test
    void shouldGetEnrollmentsByLearner() {

        Enrollment e1 = new Enrollment();
        e1.setLearnerId("L1");

        when(enrollmentRepository.findByLearnerId("L1"))
                .thenReturn(List.of(e1));

        List<Enrollment> result =
                enrollmentService.getEnrollmentsByLearner("L1");

        assertEquals(1, result.size());
        verify(enrollmentRepository).findByLearnerId("L1");
    }

    // =========================
    // 3. UPDATE PROGRESS -> COMPLETED
    // =========================
    @Test
    void shouldUpdateProgressToCompleted() {

        Enrollment enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setProgress(50.0);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findById(1L))
                .thenReturn(Optional.of(enrollment));

        when(enrollmentRepository.save(any()))
                .thenReturn(enrollment);

        enrollmentService.updateProgress(1L, 100.0);

        assertEquals(100.0, enrollment.getProgress());
        assertEquals(Enrollment.EnrollmentStatus.COMPLETED, enrollment.getStatus());
        assertNotNull(enrollment.getCompletedDate());

        verify(enrollmentRepository).save(enrollment);
    }

    // =========================
    // 4. CANCEL ENROLLMENT
    // =========================
    @Test
    void shouldCancelEnrollment() {

        Enrollment enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findById(1L))
                .thenReturn(Optional.of(enrollment));

        when(enrollmentRepository.save(any()))
                .thenReturn(enrollment);

        enrollmentService.cancelEnrollment(1L);

        assertEquals(Enrollment.EnrollmentStatus.CANCELLED, enrollment.getStatus());

        verify(enrollmentRepository).save(enrollment);
    }

    // =========================
    // 5. UPDATE STATUS
    // =========================
    @Test
    void shouldUpdateStatus() {

        Enrollment enrollment = new Enrollment();
        enrollment.setId(1L);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findById(1L))
                .thenReturn(Optional.of(enrollment));

        when(enrollmentRepository.save(any()))
                .thenReturn(enrollment);

        Enrollment result =
                enrollmentService.updateStatus(1L, "COMPLETED");

        assertEquals(Enrollment.EnrollmentStatus.COMPLETED, result.getStatus());
        assertEquals(100.0, result.getProgress());
        assertNotNull(result.getCompletedDate());

        verify(enrollmentRepository).save(enrollment);
    }

    // =========================
    // 6. NOT FOUND ERROR
    // =========================
    @Test
    void shouldThrowIfEnrollmentNotFound() {

        when(enrollmentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> enrollmentService.updateProgress(99L, 50.0));
    }
}