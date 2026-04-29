package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEmailService;

@Service
public class EmailService implements IEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Formini}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("Email envoyé à: " + to);
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
    }

    // ===== TEMPLATES EMAIL =====

    public String buildPaymentSuccessEmail(String learnerId, Double amount, String invoiceNumber) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #7c3aed, #6d28d9); padding: 30px; text-align: center; border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0;">✅ Paiement Confirmé</h1>
                    <p style="color: #e9d5ff; margin: 8px 0 0;">Formini - Plateforme de Formation</p>
                </div>
                <div style="background: #ffffff; padding: 30px; border: 1px solid #e5e7eb;">
                    <p style="font-size: 16px; color: #374151;">Bonjour,</p>
                    <p style="color: #374151;">Votre paiement de <strong style="color: #7c3aed;">%s TND</strong> a été traité avec succès.</p>
                    <div style="background: #f5f0ff; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <p style="margin: 0; color: #6d28d9;"><strong>Numéro de facture:</strong> %s</p>
                        <p style="margin: 8px 0 0; color: #6d28d9;"><strong>Apprenant ID:</strong> #%s</p>
                    </div>
                    <p style="color: #374151;">Vous pouvez maintenant accéder à vos cours.</p>
                    <a href="%s/trainer_course/list" style="display: inline-block; background: #7c3aed; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; margin-top: 16px;">
                        Accéder à mes cours →
                    </a>
                </div>
                <div style="background: #f9fafb; padding: 16px; text-align: center; border-radius: 0 0 12px 12px; border: 1px solid #e5e7eb; border-top: none;">
                    <p style="color: #9ca3af; font-size: 12px; margin: 0;">© 2025 Formini | contact@formini.tn</p>
                </div>
            </div>
            """.formatted(amount, invoiceNumber, learnerId, frontendUrl);
    }

    public String buildCartAbandonedEmail(String learnerId, int itemCount) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #f59e0b, #d97706); padding: 30px; text-align: center; border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0;">🛒 Votre panier vous attend !</h1>
                </div>
                <div style="background: white; padding: 30px; border: 1px solid #e5e7eb;">
                    <p style="color: #374151;">Bonjour,</p>
                    <p style="color: #374151;">Vous avez <strong>%d cours</strong> dans votre panier depuis plus de 24h.</p>
                    <p style="color: #374151;">Ne laissez pas passer cette opportunité d'apprentissage !</p>
                    <a href="%s/cart" style="display: inline-block; background: #f59e0b; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; margin-top: 16px;">
                        Finaliser mon achat →
                    </a>
                </div>
                <div style="background: #f9fafb; padding: 16px; text-align: center; border-radius: 0 0 12px 12px;">
                    <p style="color: #9ca3af; font-size: 12px; margin: 0;">© 2025 Formini</p>
                </div>
            </div>
            """.formatted(itemCount, frontendUrl);
    }

    public String buildInstallmentReminderEmail(String learnerId, Double amount, String dueDate, int installmentNumber) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #3b82f6, #2563eb); padding: 30px; text-align: center; border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0;">📅 Rappel d'Échéance</h1>
                </div>
                <div style="background: white; padding: 30px; border: 1px solid #e5e7eb;">
                    <p style="color: #374151;">Bonjour,</p>
                    <p style="color: #374151;">Votre <strong>échéance n°%d</strong> de <strong style="color: #3b82f6;">%s TND</strong> est due le <strong>%s</strong>.</p>
                    <div style="background: #eff6ff; border-radius: 8px; padding: 16px; margin: 20px 0; border-left: 4px solid #3b82f6;">
                        <p style="margin: 0; color: #1d4ed8;">⚠️ En cas de non-paiement, votre accès aux cours sera suspendu.</p>
                    </div>
                    <a href="%s/cart/my-installments" style="display: inline-block; background: #3b82f6; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none;">
                        Payer maintenant →
                    </a>
                </div>
            </div>
            """.formatted(installmentNumber, amount, dueDate, frontendUrl);
    }

    public String buildOverdueEmail(Double amount, int installmentNumber) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #ef4444, #dc2626); padding: 30px; text-align: center; border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0;">🚨 Échéance en Retard</h1>
                </div>
                <div style="background: white; padding: 30px; border: 1px solid #e5e7eb;">
                    <p style="color: #374151;">Bonjour,</p>
                    <p style="color: #374151;">Votre <strong>échéance n°%d</strong> de <strong style="color: #ef4444;">%s TND</strong> est en retard.</p>
                    <div style="background: #fef2f2; border-radius: 8px; padding: 16px; margin: 20px 0; border-left: 4px solid #ef4444;">
                        <p style="margin: 0; color: #dc2626;"><strong>Votre accès aux cours est actuellement suspendu.</strong></p>
                    </div>
                    <a href="%s/cart/my-installments" style="display: inline-block; background: #ef4444; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none;">
                        Régulariser maintenant →
                    </a>
                </div>
            </div>
            """.formatted(installmentNumber, amount, frontendUrl);
    }
}