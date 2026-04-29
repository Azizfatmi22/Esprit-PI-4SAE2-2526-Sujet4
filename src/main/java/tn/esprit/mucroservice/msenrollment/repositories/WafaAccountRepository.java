package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.mucroservice.msenrollment.entities.WafaAccount;

import java.util.Optional;

public interface WafaAccountRepository
        extends JpaRepository<WafaAccount, Long> {
    Optional<WafaAccount> findByPhoneNumber(String phoneNumber);
}