package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.mucroservice.msenrollment.entities.WafaRefund;
import java.util.List;

public interface WafaRefundRepository extends JpaRepository<WafaRefund, Long> {
    List<WafaRefund> findByLearnerIdOrderByRequestedAtDesc(String learnerId);
    List<WafaRefund> findByPaymentId(Long paymentId);
}
