package com.formini.msliveclass.services;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.clients.EnrollmentClient;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.EnrollmentSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SessionAccessService {

    private static final Logger log = LoggerFactory.getLogger(SessionAccessService.class);

    private final CourseClient courseClient;
    private final EnrollmentClient enrollmentClient;

    @Autowired
    public SessionAccessService(CourseClient courseClient, EnrollmentClient enrollmentClient) {
        this.courseClient = courseClient;
        this.enrollmentClient = enrollmentClient;
    }

    public Optional<String> findCourseTrainerId(Long courseId) {
        if (courseId == null) {
            log.warn("Course ID is null");
            return Optional.empty();
        }

        try {
            log.info("Fetching trainer for course ID: {}", courseId);
            CourseDTO course = courseClient.getCourseById(courseId);

            if (course == null) {
                log.warn("Course not found for ID: {}", courseId);
                return Optional.empty();
            }

            String trainerId = course.getTrainerId();
            if (trainerId == null || trainerId.trim().isEmpty()) {
                log.warn("Trainer ID is null or empty for course ID: {}", courseId);
                return Optional.empty();
            }

            trainerId = trainerId.trim();
            log.info("Found trainer ID: {} for course ID: {}", trainerId, courseId);
            return Optional.of(trainerId);
        } catch (Exception e) {
            log.error("Error fetching trainer for course ID: {}", courseId, e);
            return Optional.empty();
        }
    }

    public boolean isCourseTrainer(Long courseId, String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("User ID is null or blank");
            return false;
        }

        String normalizedUserId = userId.trim();
        log.info("Checking if user {} is trainer for course {}", normalizedUserId, courseId);
        
        Optional<String> trainerIdOpt = findCourseTrainerId(courseId);
        if (trainerIdOpt.isEmpty()) {
            log.warn("Could not find trainer ID for course {}", courseId);
            return false;
        }
        
        boolean isTrainer = trainerIdOpt.get().equals(normalizedUserId);
        log.info("User {} is {} trainer for course {}", normalizedUserId, isTrainer ? "the" : "NOT the", courseId);
        return isTrainer;
    }

    public boolean hasPaidEnrollment(Long courseId, String learnerId) {
        if (courseId == null || learnerId == null || learnerId.isBlank()) {
            log.warn("Invalid parameters - courseId: {}, learnerId: {}", courseId, learnerId);
            return false;
        }

        String normalizedLearnerId = learnerId.trim();
        log.info("========== CHECKING PAID ENROLLMENT ==========");
        log.info("Course ID: {}", courseId);
        log.info("Learner ID: {}", normalizedLearnerId);

        try {
            log.info("Attempting to fetch enrollments from enrollment service...");
            List<EnrollmentSummary> enrollments = enrollmentClient.getEnrollmentsByLearner(normalizedLearnerId);
            log.info("Received {} enrollment(s) from service", enrollments != null ? enrollments.size() : 0);
            
            if (enrollments != null && !enrollments.isEmpty()) {
                log.info("Enrollment records:");
                for (EnrollmentSummary enrollment : enrollments) {
                    log.info("  - Course ID: {}, Status: {}", enrollment.getCourseId(), enrollment.getStatus());
                }
                
                boolean hasPaidEnrollmentRecord = enrollments.stream().anyMatch(enrollment ->
                        Objects.equals(courseId, enrollment.getCourseId())
                                && isPaidEnrollmentStatus(enrollment.getStatus())
                );

                if (hasPaidEnrollmentRecord) {
                    log.info("✅ Found paid enrollment record for course {}", courseId);
                    return true;
                } else {
                    log.warn("❌ No paid enrollment found for course {} in enrollment records", courseId);
                }
            } else {
                log.warn("No enrollment records found for learner {}", normalizedLearnerId);
            }
        } catch (Exception e) {
            log.error("Error fetching enrollments from service: {}", e.getMessage(), e);
            // fall through to access endpoint fallback
        }

        log.info("Trying fallback: hasCourseAccess endpoint...");
        try {
            Boolean hasAccess = enrollmentClient.hasCourseAccess(normalizedLearnerId, courseId);
            log.info("hasCourseAccess returned: {}", hasAccess);
            
            if (Boolean.TRUE.equals(hasAccess)) {
                log.info("✅ Access granted via hasCourseAccess endpoint");
                return true;
            } else {
                log.warn("❌ Access denied via hasCourseAccess endpoint");
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking course access: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean isPaidEnrollmentStatus(String status) {
        if (status == null) {
            return false;
        }

        String normalized = status.trim().toUpperCase();
        return "ACTIVE".equals(normalized) || "COMPLETED".equals(normalized);
    }
}
