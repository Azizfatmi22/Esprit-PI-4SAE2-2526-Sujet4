package tn.esprit.mucroservice.msenrollment.services.interfaces;

public interface IEmailService {
    String buildPaymentSuccessEmail(String learnerId, Double amount, String invoiceNumber);
    String buildCartAbandonedEmail(String learnerId, int itemCount);
    String buildInstallmentReminderEmail(String learnerId, Double amount, String dueDate, int installmentNumber);
    String buildOverdueEmail(Double amount, int installmentNumber);

    void sendEmail(String to, String subject, String html);
}