package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.mucroservice.msenrollment.entities.InstallmentPlan;
import tn.esprit.mucroservice.msenrollment.entities.InstallmentPlanStatus;

import java.util.List;
import java.util.Optional;

public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {
    List<InstallmentPlan> findByLearnerId(String learnerId);
    List<InstallmentPlan> findByLearnerIdAndStatus(String learnerId, InstallmentPlanStatus status);
    Optional<InstallmentPlan> findByPaymentId(Long paymentId);
}