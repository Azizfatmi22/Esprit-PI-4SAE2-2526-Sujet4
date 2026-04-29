package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.NotificationRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEmailService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.util.List;

@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired
    private IEmailService emailService;

    // Email fictif — à remplacer par votre UserService
    private String getEmailByLearnerId(String learnerId) {
        return "inesjlassi245@gmail.com";
        //return "learner" + learnerId + "@formini.tn";
    }

    // ===== 1. PAIEMENT RÉUSSI =====
    @Override
    public void notifyPaymentSuccess(String learnerId, Double amount, String invoiceNumber, Long paymentId) {
        // Éviter les doublons
        if (notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                learnerId, NotificationType.PAYMENT_SUCCESS, paymentId)) return;

        // Créer la notification en base
        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setTitle("Paiement confirmé !");
        notification.setMessage("Votre paiement de " + amount + " TND a été traité. Facture: " + invoiceNumber);
        notification.setRelatedId(paymentId);
        notification.setEmailSent(false);
        notificationRepository.save(notification);

        // Envoyer l'email
        String html = emailService.buildPaymentSuccessEmail(learnerId, amount, invoiceNumber);
        emailService.sendEmail("inesjlassi245@gmail.com", "✅ Paiement confirmé - " + invoiceNumber, html);

        notification.setEmailSent(true);
        notificationRepository.save(notification);
    }

    // ===== 2. PANIER ABANDONNÉ =====
    @Override
    public void notifyCartAbandoned(String learnerId, int itemCount) {
        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.CART_ABANDONED);
        notification.setTitle("Votre panier vous attend !");
        notification.setMessage("Vous avez " + itemCount + " cours dans votre panier.");
        notification.setRelatedId(null); // Pas de relatedId pour les paniers abandonnés
        notificationRepository.save(notification);

        String html = emailService.buildCartAbandonedEmail(learnerId, itemCount);
        emailService.sendEmail(getEmailByLearnerId(learnerId), "🛒 N'oubliez pas votre panier !", html);

        notification.setEmailSent(true);
        notificationRepository.save(notification);
    }

    // ===== 3. RAPPEL ÉCHÉANCE =====
    @Override
    public void notifyInstallmentReminder(String learnerId, Double amount, String dueDate, int installmentNumber, Long installmentId) {
        if (notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                learnerId, NotificationType.INSTALLMENT_REMINDER, installmentId)) return;

        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.INSTALLMENT_REMINDER);
        notification.setTitle("Rappel: Échéance #" + installmentNumber + " due le " + dueDate);
        notification.setMessage("Montant: " + amount + " TND à payer avant le " + dueDate);
        notification.setRelatedId(installmentId);
        notificationRepository.save(notification);

        String html = emailService.buildInstallmentReminderEmail(learnerId, amount, dueDate, installmentNumber);
        emailService.sendEmail(getEmailByLearnerId(learnerId), "📅 Rappel échéance #" + installmentNumber, html);

        notification.setEmailSent(true);
        notificationRepository.save(notification);
    }

    // ===== 4. ÉCHÉANCE EN RETARD =====
    @Override
    public void notifyInstallmentOverdue(String learnerId, Double amount, int installmentNumber, Long installmentId) {
        if (notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                learnerId, NotificationType.INSTALLMENT_OVERDUE, installmentId)) return;

        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.INSTALLMENT_OVERDUE);
        notification.setTitle("🚨 Échéance en retard !");
        notification.setMessage("Votre échéance #" + installmentNumber + " de " + amount + " TND est en retard. Accès suspendu.");
        notification.setRelatedId(installmentId);
        notificationRepository.save(notification);

        String html = emailService.buildOverdueEmail(amount, installmentNumber);
        emailService.sendEmail(getEmailByLearnerId(learnerId), "🚨 Échéance en retard - Action requise", html);

        notification.setEmailSent(true);
        notificationRepository.save(notification);
    }

    // ===== 5. COURS AJOUTÉ AU PANIER =====
    @Override
    public void notifyCourseAddedToCart(String learnerId, String courseTitle) {
        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.COURSE_ADDED_TO_CART);
        notification.setTitle("Cours ajouté au panier");
        notification.setMessage("\"" + courseTitle + "\" a été ajouté à votre panier.");
        notification.setRelatedId(null); // No related ID for cart additions
        notificationRepository.save(notification);
    }

    // ===== CONSULTATION =====
    @Override
    public List<Notification> getNotificationsByLearner(String learnerId) {
        return notificationRepository.findByLearnerIdOrderByCreatedAtDesc(learnerId);
    }

    @Override
    public List<Notification> getUnreadNotifications(String learnerId) {
        return notificationRepository.findByLearnerIdAndIsReadFalse(learnerId);
    }

    @Override
    public long getUnreadCount(String learnerId) {
        return notificationRepository.countByLearnerIdAndIsReadFalse(learnerId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    public void markAllAsRead(String learnerId) {
        List<Notification> unread = notificationRepository.findByLearnerIdAndIsReadFalse(learnerId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
    @Override
    public void sendOtpEmail(String learnerId, String otpCode, String phoneNumber) {
        try {
            // ✅ Sauvegarder la notification en base
            Notification notification = new Notification();
            notification.setLearnerId(learnerId);
            notification.setType(NotificationType.PAYMENT_SUCCESS); // ou crée un type OTP_SENT
            notification.setTitle("Code OTP Flouci envoyé");
            notification.setMessage("Code OTP envoyé au " + phoneNumber);
            notification.setRelatedId(null); // No related ID for OTP emails
            notification.setEmailSent(false);
            notificationRepository.save(notification);

            // ✅ Construire le contenu HTML de l'email
            String html = "<div style='font-family: Arial; padding: 20px;'>"
                    + "<h2 style='color: #8B5CF6;'>💜 Votre code Flouci</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Votre code de vérification est :</p>"
                    + "<div style='font-size: 36px; font-weight: bold; "
                    + "letter-spacing: 10px; color: #8B5CF6; padding: 20px; "
                    + "background: #F3E8FF; border-radius: 8px; text-align: center;'>"
                    + otpCode
                    + "</div>"
                    + "<p style='color: #666;'>⏱ Ce code est valable pendant <strong>2 minutes</strong>.</p>"
                    + "<p style='color: #666;'>📱 Numéro associé : <strong>" + phoneNumber + "</strong></p>"
                    + "<hr/>"
                    + "<p style='color: #999; font-size: 12px;'>"
                    + "Si vous n'avez pas demandé ce code, ignorez cet email.</p>"
                    + "</div>";

            // ✅ Utiliser emailService comme partout ailleurs dans ta classe
            emailService.sendEmail(
                    getEmailByLearnerId(learnerId),
                    "💜 Votre code Flouci : " + otpCode,
                    html
            );

            notification.setEmailSent(true);
            notificationRepository.save(notification);

            System.out.println("✅ Email OTP envoyé pour learnerId: " + learnerId);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email OTP: " + e.getMessage());
            // Ne pas bloquer le flux si l'email échoue
        }
    }
    // ✅ AJOUTER à la fin de NotificationServiceImpl
    @Override
    public void notifyWafaRefund(String learnerId, Double amount,
                                 String courseTitle, String phoneNumber) {
        try {
            // Sauvegarder la notification en base
            Notification notification = new Notification();
            notification.setLearnerId(learnerId);
            notification.setType(NotificationType.PAYMENT_SUCCESS);
            notification.setTitle("💚 Remboursement Wafa Cash effectué");
            notification.setMessage(
                    "Remboursement de " + amount + " TND pour \""
                            + courseTitle + "\" vers le " + phoneNumber
            );
            notification.setRelatedId(null); // No specific related entity
            notification.setEmailSent(false);
            notificationRepository.save(notification);

            // Construire l'email HTML
            String html = "<div style='font-family: Arial; padding: 20px;'>"
                    + "<h2 style='color: #10b981;'>💚 Remboursement Wafa Cash</h2>"
                    + "<p>Votre remboursement a été traité avec succès.</p>"
                    + "<div style='background: #f0fdf4; padding: 16px; "
                    + "border-radius: 8px; margin-top: 16px;'>"
                    + "<p>📱 Numéro : <strong>" + phoneNumber + "</strong></p>"
                    + "<p>💰 Montant remboursé : <strong>" + amount + " TND</strong></p>"
                    + "<p>📚 Cours : <strong>" + courseTitle + "</strong></p>"
                    + "</div>"
                    + "<p style='color:#666; margin-top:16px;'>"
                    + "Le montant a été crédité sur votre portefeuille Wafa Cash.</p>"
                    + "</div>";

            // Utiliser emailService comme partout ailleurs
            emailService.sendEmail(
                    getEmailByLearnerId(learnerId),
                    "💚 Remboursement Wafa Cash - " + amount + " TND",
                    html
            );

            notification.setEmailSent(true);
            notificationRepository.save(notification);

        } catch (Exception e) {
            System.err.println("❌ Erreur notification remboursement Wafa: " + e.getMessage());
        }
    }
    @Override
    public void notifyBakchichCodeGenerated(String learnerId, String code,
                                            Double amount, String phoneNumber) {
        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setTitle("💙 Code Bakchich généré");
        notification.setMessage("Code: " + code + " — " + amount + " TND à payer en agence sous 24h");
        notification.setRelatedId(null); // No related entity ID
        notification.setEmailSent(false);
        notificationRepository.save(notification);

        String html = "<div style='font-family: Arial; padding: 20px;'>"
                + "<h2 style='color: #3b82f6;'>💙 Votre code Bakchich</h2>"
                + "<p>Présentez ce code en agence Bakchich pour finaliser votre paiement.</p>"
                + "<div style='background: #eff6ff; padding: 24px; border-radius: 12px; "
                + "text-align: center; margin: 20px 0;'>"
                + "<p style='font-size: 32px; font-weight: bold; "
                + "letter-spacing: 4px; color: #2563eb;'>" + code + "</p>"
                + "</div>"
                + "<p>💰 Montant : <strong>" + amount + " TND</strong></p>"
                + "<p>📱 Téléphone : <strong>" + phoneNumber + "</strong></p>"
                + "<p style='color: #ef4444;'>⏰ Valable 24h seulement</p>"
                + "</div>";

        emailService.sendEmail(
                getEmailByLearnerId(learnerId),
                "💙 Votre code de paiement Bakchich : " + code,
                html
        );

        notification.setEmailSent(true);
        notificationRepository.save(notification);
    }

    @Override
    public void notifyBakchichConfirmed(String learnerId, Double amount, String code) {
        Notification notification = new Notification();
        notification.setLearnerId(learnerId);
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setTitle("✅ Paiement Bakchich confirmé");
        notification.setMessage("Paiement de " + amount + " TND confirmé en agence");
        notification.setRelatedId(null); // No related entity ID
        notification.setEmailSent(false);
        notificationRepository.save(notification);

        String html = "<div style='font-family: Arial; padding: 20px;'>"
                + "<h2 style='color: #10b981;'>✅ Paiement confirmé !</h2>"
                + "<p>Votre paiement Bakchich a été confirmé par notre agence.</p>"
                + "<div style='background: #f0fdf4; padding: 16px; border-radius: 8px;'>"
                + "<p>💰 Montant : <strong>" + amount + " TND</strong></p>"
                + "<p>🔑 Code : <strong>" + code + "</strong></p>"
                + "</div>"
                + "<p>Vos cours sont maintenant accessibles.</p>"
                + "</div>";

        emailService.sendEmail(
                getEmailByLearnerId(learnerId),
                "✅ Paiement Bakchich confirmé",
                html
        );

        notification.setEmailSent(true);
        notificationRepository.save(notification);
    }

    // ===== SEND INVOICE EMAIL =====
    @Override
    public void sendInvoiceEmail(String learnerId, Invoice invoice) {
        try {
            String email = getEmailByLearnerId(learnerId);
            String subject = "📄 Invoice " + invoice.getInvoiceNumber();

            // Build HTML email with invoice details
            String html = "<div style='font-family: Arial; padding: 20px;'>"
                    + "<h2 style='color: #1f2937;'>Invoice Details</h2>"
                    + "<div style='background: #f3f4f6; padding: 20px; border-radius: 8px;'>"
                    + "<p><strong>Invoice Number:</strong> " + invoice.getInvoiceNumber() + "</p>"
                    + "<p><strong>Amount:</strong> " + invoice.getTotalAmount() + " " + invoice.getCurrency() + "</p>"
                    + "<p><strong>Status:</strong> " + invoice.getStatus() + "</p>"
                    + "<p><strong>Issue Date:</strong> " + invoice.getIssueDate() + "</p>"
                    + "</div>"
                    + "<p>Thank you for your purchase!</p>"
                    + "</div>";

            emailService.sendEmail(email, subject, html);

            // Create and save notification
            Notification notification = new Notification();
            notification.setLearnerId(learnerId);
            notification.setType(NotificationType.PAYMENT_SUCCESS);
            notification.setTitle("Invoice sent");
            notification.setMessage("Invoice " + invoice.getInvoiceNumber() + " has been sent to your email");
            notification.setEmailSent(true);
            notificationRepository.save(notification);

        } catch (Exception e) {
            System.err.println("Error sending invoice email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}