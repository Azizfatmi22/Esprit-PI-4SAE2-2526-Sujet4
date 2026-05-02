package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.Notification;
import tn.esprit.mucroservice.msenrollment.entities.NotificationType;
import tn.esprit.mucroservice.msenrollment.entities.WafaAccount;
import tn.esprit.mucroservice.msenrollment.repositories.WafaAccountRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IWafaService;

import java.util.Random;

@Service
public class WafaServiceImpl implements IWafaService {

    @Autowired
    private WafaAccountRepository wafaRepo;
    private static final Random RANDOM = new Random();

    @Override
    public Double getBalance(String phoneNumber) {
        WafaAccount account = wafaRepo.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Aucun compte Wafa Cash trouvé pour ce numéro : " + phoneNumber
                ));
        return account.getBalance();
    }

    @Override
    public boolean hasSufficientBalance(String phoneNumber, Double amount) {
        try {
            Double balance = getBalance(phoneNumber);
            return balance >= amount;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public WafaAccount debitAccount(String phoneNumber, Double amount) {
        WafaAccount account = wafaRepo.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        if (account.getBalance() < amount) {
            throw new RuntimeException(
                    "Solde insuffisant. Solde disponible : "
                            + account.getBalance() + " TND"
            );
        }

        account.setBalance(account.getBalance() - amount);
        return wafaRepo.save(account);
    }

    @Override
    public WafaAccount creditAccount(String phoneNumber, Double amount) {
        WafaAccount account = wafaRepo.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        account.setBalance(account.getBalance() + amount);
        return wafaRepo.save(account);
    }

    @Override
    public WafaAccount getOrCreateAccount(String phoneNumber, String learnerId) {
        return wafaRepo.findByPhoneNumber(phoneNumber).orElseGet(() -> {
            WafaAccount newAccount = new WafaAccount();
            newAccount.setPhoneNumber(phoneNumber);
            newAccount.setLearnerId(learnerId);
            newAccount.setBalance(100.0 + RANDOM.nextInt(900));
            return wafaRepo.save(newAccount);
        });
    }
}