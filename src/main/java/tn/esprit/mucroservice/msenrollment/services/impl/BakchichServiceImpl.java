package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.BakchichPayment;
import tn.esprit.mucroservice.msenrollment.entities.BakchichStatus;
import tn.esprit.mucroservice.msenrollment.repositories.BakchichPaymentRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IBakchichService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.time.Year;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BakchichServiceImpl implements IBakchichService {

    @Autowired
    private BakchichPaymentRepository bakchichRepo;
    @Autowired private INotificationService notificationService;

    @Override
    public BakchichPayment generatePaymentCode(String learnerId,
                                               String phoneNumber,
                                               Double amount,
                                               String courseTitles) {
        // Générer code unique
        String code = "BKC-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Données QR code
        String qrData = "BAKCHICH|CODE:" + code
                + "|AMOUNT:" + amount
                + "|PHONE:" + phoneNumber
                + "|EXPIRES:24H";

        // Calculer expiration 24h
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);

        BakchichPayment payment = new BakchichPayment();
        payment.setPaymentCode(code);
        payment.setQrCodeData(qrData);
        payment.setLearnerId(learnerId);
        payment.setPhoneNumber(phoneNumber);
        payment.setAmount(amount);
        payment.setStatus(BakchichStatus.PENDING_CASH);
        payment.setExpiresAt(cal.getTime());
        payment.setCourseTitles(courseTitles);

        bakchichRepo.save(payment);

        // Notifier le learner
        notificationService.notifyBakchichCodeGenerated(
                learnerId, code, amount, phoneNumber
        );

        System.out.println("💙 Code Bakchich généré : " + code
                + " pour " + amount + " TND");
        return payment;
    }

    @Override
    public BakchichPayment confirmPayment(Long bakchichId, String confirmedBy) {
        BakchichPayment payment = bakchichRepo.findById(bakchichId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        if (payment.getStatus() != BakchichStatus.PENDING_CASH) {
            throw new RuntimeException("Ce paiement ne peut pas être confirmé. Statut: " + payment.getStatus());
        }

        if (new Date().after(payment.getExpiresAt())) {
            payment.setStatus(BakchichStatus.EXPIRED);
            bakchichRepo.save(payment);
            throw new RuntimeException("Ce code est expiré");
        }

        payment.setStatus(BakchichStatus.CONFIRMED);
        payment.setConfirmedAt(new Date());
        payment.setConfirmedBy(confirmedBy);
        bakchichRepo.save(payment);

        // ✅ Mail seulement pour les paiements DIRECTS
        // Les installments sont gérés dans le controller
        if (payment.getPlanId() == null) {
            notificationService.notifyBakchichConfirmed(
                    payment.getLearnerId(),
                    payment.getAmount(),
                    payment.getPaymentCode()
            );
        }

        return payment;
    }

    @Override
    public BakchichPayment cancelPayment(Long bakchichId) {
        BakchichPayment payment = bakchichRepo.findById(bakchichId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        payment.setStatus(BakchichStatus.CANCELLED);
        return bakchichRepo.save(payment);
    }

    @Override
    public List<BakchichPayment> getPendingPayments() {
        return bakchichRepo.findByStatusOrderByCreatedAtDesc(
                BakchichStatus.PENDING_CASH
        );
    }

    @Override
    public List<BakchichPayment> getPaymentsByLearner(String learnerId) {
        return bakchichRepo.findByLearnerIdOrderByCreatedAtDesc(learnerId);
    }

    // Appelé automatiquement toutes les heures
    @Override
    @Scheduled(fixedRate = 3600000)
    public void expireOldPayments() {
        List<BakchichPayment> expired = bakchichRepo.findExpiredPayments(new Date());
        expired.forEach(p -> {
            p.setStatus(BakchichStatus.EXPIRED);
            bakchichRepo.save(p);
            System.out.println("⏰ Paiement Bakchich expiré : " + p.getPaymentCode());
        });
    }
    @Override
    public BakchichPayment generateInstallmentCode(String learnerId,
                                                   String phoneNumber,
                                                   Double amount,
                                                   String courseTitles,
                                                   Long planId) {
        String code = "BKC-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String qrData = "BAKCHICH|CODE:" + code
                + "|AMOUNT:" + amount
                + "|PHONE:" + phoneNumber
                + "|EXPIRES:24H";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);

        BakchichPayment payment = new BakchichPayment();
        payment.setPaymentCode(code);
        payment.setQrCodeData(qrData);
        payment.setLearnerId(learnerId);
        payment.setPhoneNumber(phoneNumber);
        payment.setAmount(amount);
        payment.setStatus(BakchichStatus.PENDING_CASH);
        payment.setExpiresAt(cal.getTime());
        payment.setCourseTitles(courseTitles);
        payment.setPlanId(planId);  // ✅ lié au plan installment

        bakchichRepo.save(payment);

        // ✅ Envoyer seulement le mail "code généré" — PAS le mail de succès
        notificationService.notifyBakchichCodeGenerated(
                learnerId, code, amount, phoneNumber
        );

        return payment;
    }
}