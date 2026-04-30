package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.FlouciStatus;
import tn.esprit.mucroservice.msenrollment.entities.FlouciTransaction;
import tn.esprit.mucroservice.msenrollment.repositories.FlouciTransactionRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IFlouciService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class FlouciServiceImpl implements IFlouciService {
    private static final Random RANDOM = new Random();

    @Autowired
    private FlouciTransactionRepository flouciRepo;

    @Autowired
    private INotificationService notificationService;

    // ✅ Étape 1 : Générer et envoyer l'OTP
    @Override
    public FlouciTransaction initiatePayment(String learnerId,
                                             String phoneNumber,
                                             Double amount) {
        // Générer OTP à 6 chiffres
        String otp = String.format("%06d", RANDOM.nextInt(999999));

        // Générer référence unique
        String ref = "FLC-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FlouciTransaction transaction = new FlouciTransaction();
        transaction.setLearnerId(learnerId);
        transaction.setPhoneNumber(phoneNumber);
        transaction.setAmount(amount);
        transaction.setOtpCode(otp);
        transaction.setTransactionRef(ref);
        transaction.setOtpExpiry(LocalDateTime.now().plusMinutes(2));
        transaction.setStatus(FlouciStatus.PENDING_OTP);

        flouciRepo.save(transaction);

        // Simuler envoi SMS en console
        System.out.println("📱 SMS simulé → " + phoneNumber + " : Votre code OTP est " + otp);

        // Envoyer email de notification
        notificationService.sendOtpEmail(learnerId, otp, phoneNumber);

        return transaction;
    }

    // ✅ Étape 2 : Vérifier l'OTP
    @Override
    public boolean verifyOtp(String transactionRef, String otpEntered) {
        FlouciTransaction transaction = flouciRepo
                .findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Transaction introuvable : " + transactionRef));

        // Vérifier expiration
        if (LocalDateTime.now().isAfter(transaction.getOtpExpiry())) {
            transaction.setStatus(FlouciStatus.EXPIRED);
            flouciRepo.save(transaction);
            throw new RuntimeException("OTP expiré. Veuillez renvoyer un nouveau code.");
        }

        // Vérifier le code saisi
        if (!transaction.getOtpCode().equals(otpEntered)) {
            transaction.setStatus(FlouciStatus.FAILED);
            flouciRepo.save(transaction);
            return false;
        }

        // OTP correct
        transaction.setStatus(FlouciStatus.VERIFIED);
        flouciRepo.save(transaction);
        return true;
    }

    // ✅ Renvoyer un nouveau code OTP
    @Override
    public FlouciTransaction resendOtp(String transactionRef) {
        FlouciTransaction transaction = flouciRepo
                .findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Transaction introuvable : " + transactionRef));

        // Générer nouveau code
        String newOtp = String.format("%06d", RANDOM.nextInt(999999));
        transaction.setOtpCode(newOtp);
        transaction.setOtpExpiry(LocalDateTime.now().plusMinutes(2));
        transaction.setStatus(FlouciStatus.PENDING_OTP);

        flouciRepo.save(transaction);

        System.out.println("📱 Renvoi SMS → " + transaction.getPhoneNumber() + " : " + newOtp);
        notificationService.sendOtpEmail(
                transaction.getLearnerId(),
                newOtp,
                transaction.getPhoneNumber()
        );

        return transaction;
    }

    // ✅ Historique des transactions
    @Override
    public List<FlouciTransaction> getHistory(String learnerId) {
        return flouciRepo.findByLearnerIdOrderByCreatedAtDesc(learnerId);
    }
}