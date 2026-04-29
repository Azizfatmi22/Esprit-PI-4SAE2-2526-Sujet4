package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.BakchichPayment;

import java.util.List;

public interface IBakchichService {
    BakchichPayment generatePaymentCode(String learnerId,
                                        String phoneNumber,
                                        Double amount,
                                        String courseTitles);
    BakchichPayment generateInstallmentCode(String learnerId, String phoneNumber, Double amount, String courseTitles, Long planId);
    BakchichPayment confirmPayment(Long bakchichId, String confirmedBy);
    BakchichPayment cancelPayment(Long bakchichId);
    List<BakchichPayment> getPendingPayments();
    List<BakchichPayment> getPaymentsByLearner(String learnerId);
    void expireOldPayments(); // appelé par scheduler

}