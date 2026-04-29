package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.mucroservice.msenrollment.entities.Installment;
import tn.esprit.mucroservice.msenrollment.entities.InstallmentStatus;

import java.util.Date;
import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findByInstallmentPlanId(Long planId);

    // Chercher les échéances en retard (dueDate dépassée et status PENDING)
    @Query("SELECT i FROM Installment i WHERE i.dueDate < :today AND i.status = :status")
    List<Installment> findOverdueInstallments(Date today, InstallmentStatus status);

    // Prochaine échéance à payer pour un plan
    @Query("SELECT i FROM Installment i WHERE i.installmentPlan.id = :planId AND i.status = 'PENDING' ORDER BY i.installmentNumber ASC")
    List<Installment> findNextPendingInstallment(Long planId);

    // Trouver les échéances entre deux dates
    @Query("SELECT i FROM Installment i WHERE i.dueDate BETWEEN :start AND :end AND i.status = :status")
    List<Installment> findByDueDateBetweenAndStatus(Date start, Date end, InstallmentStatus status);
    void deleteByInstallmentPlanId(Long installmentPlanId);
}