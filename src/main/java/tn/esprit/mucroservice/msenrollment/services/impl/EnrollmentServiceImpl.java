package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.Enrollment;
import tn.esprit.mucroservice.msenrollment.repositories.EnrollmentRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEnrollmentService;

import java.util.Date;
import java.util.List;

@Service
public class EnrollmentServiceImpl implements IEnrollmentService {

    @Autowired private EnrollmentRepository enrollmentRepository;


    @Override
    @Transactional
    public Enrollment createEnrollment(String learnerId, Long courseId) {
        try {
            System.out.println("Création d'enrollment - LearnerId: " + learnerId + ", CourseId: " + courseId);
            
            Enrollment enrollment = new Enrollment();
            enrollment.setLearnerId(learnerId);
            enrollment.setCourseId(courseId);
            enrollment.setEnrolledDate(new Date());
            enrollment.setProgress(0.0);
            enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

            Enrollment saved = enrollmentRepository.save(enrollment);
            System.out.println("Enrollment créé avec ID: " + saved.getId());
            return saved;
        } catch (Exception e) {
            System.err.println("ERREUR lors de la création d'enrollment: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création d'enrollment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Enrollment> getEnrollmentsByLearner(String learnerId) {
        return enrollmentRepository.findByLearnerId(learnerId);
    }



    @Override
    public void updateProgress(Long enrollmentId, Double progress) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment non trouvé"));
        enrollment.setProgress(progress);
        if (progress >= 100.0) {
            enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
            enrollment.setCompletedDate(new Date());
        }
        enrollmentRepository.save(enrollment);
    }

    @Override
    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment non trouvé"));
        enrollment.setStatus(Enrollment.EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);
    }
    @Override
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Override
    public Enrollment updateStatus(Long enrollmentId, String status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment introuvable"));
        enrollment.setStatus(Enrollment.EnrollmentStatus.valueOf(status));
        if (status.equals("COMPLETED")) {
            enrollment.setCompletedDate(new Date());
            enrollment.setProgress(100.0);
        }
        return enrollmentRepository.save(enrollment);
    }
}