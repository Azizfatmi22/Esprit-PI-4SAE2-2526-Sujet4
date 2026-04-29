package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.FlouciTransaction;
import java.util.List;

public interface IFlouciService {

    // Étape 1 : Initier le paiement et envoyer l'OTP
    FlouciTransaction initiatePayment(String learnerId, String phoneNumber, Double amount);

    // Étape 2 : Vérifier l'OTP saisi
    boolean verifyOtp(String transactionRef, String otpEntered);

    // Renvoyer un nouveau code OTP
    FlouciTransaction resendOtp(String transactionRef);

    // Historique des transactions d'un apprenant
    List<FlouciTransaction> getHistory(String learnerId);
}