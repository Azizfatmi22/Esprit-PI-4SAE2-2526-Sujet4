package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.mucroservice.msenrollment.entities.BakchichPayment;
import tn.esprit.mucroservice.msenrollment.entities.BakchichStatus;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface BakchichPaymentRepository
        extends JpaRepository<BakchichPayment, Long> {

    List<BakchichPayment> findByStatusOrderByCreatedAtDesc(BakchichStatus status);
    List<BakchichPayment> findByLearnerIdOrderByCreatedAtDesc(String learnerId);
    Optional<BakchichPayment> findByPaymentCode(String paymentCode);

    // Pour le scheduler — trouver les paiements expirés
    @Query("SELECT b FROM BakchichPayment b WHERE b.status = 'PENDING_CASH' " +
            "AND b.expiresAt < :now")
    List<BakchichPayment> findExpiredPayments(@Param("now") Date now);
}
