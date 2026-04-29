package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.mucroservice.msenrollment.entities.FlouciTransaction;

import java.util.List;
import java.util.Optional;

public interface FlouciTransactionRepository
        extends JpaRepository<FlouciTransaction, Long> {

    List<FlouciTransaction> findByLearnerIdOrderByCreatedAtDesc(String learnerId);
    Optional<FlouciTransaction> findByTransactionRef(String ref);
}