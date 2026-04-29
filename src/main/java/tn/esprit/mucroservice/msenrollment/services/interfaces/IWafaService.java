package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.WafaAccount;

public interface IWafaService {
    Double getBalance(String phoneNumber);
    boolean hasSufficientBalance(String phoneNumber, Double amount);
    WafaAccount debitAccount(String phoneNumber, Double amount);
    WafaAccount creditAccount(String phoneNumber, Double amount); // pour remboursement
    WafaAccount getOrCreateAccount(String phoneNumber, String learnerId);
}