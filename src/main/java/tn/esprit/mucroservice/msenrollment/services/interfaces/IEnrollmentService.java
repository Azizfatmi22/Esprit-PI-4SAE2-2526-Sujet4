package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.Enrollment;


import java.util.List;

public interface IEnrollmentService {
    Enrollment createEnrollment(String learnerId, Long courseId);
    List<Enrollment> getEnrollmentsByLearner(String learnerId);
    void updateProgress(Long enrollmentId, Double progress);
    void cancelEnrollment(Long enrollmentId);
    List<Enrollment> getAllEnrollments();
    Enrollment updateStatus(Long enrollmentId, String status);
}

