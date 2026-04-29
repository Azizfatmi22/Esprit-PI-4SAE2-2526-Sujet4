package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.mucroservice.msenrollment.entities.RefundRequest;
import tn.esprit.mucroservice.msenrollment.entities.RefundStatus;

import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByLearnerId(String learnerId);
    List<RefundRequest> findByStatus(RefundStatus status);
    Optional<RefundRequest> findByInvoiceIdAndStatus(Long invoiceId, RefundStatus status);
}