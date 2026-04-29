package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.Notification;
import java.util.List;

public interface INotificationService {
    void notifyPaymentSuccess(String learnerId, Double amount, String invoiceNumber, Long paymentId);
    void notifyCartAbandoned(String learnerId, int itemCount);
    void notifyInstallmentReminder(String learnerId, Double amount, String dueDate, int installmentNumber, Long installmentId);
    void notifyInstallmentOverdue(String learnerId, Double amount, int installmentNumber, Long installmentId);
    void notifyCourseAddedToCart(String learnerId, String courseTitle);
    void sendOtpEmail(String learnerId, String otpCode, String phoneNumber);
    List<Notification> getNotificationsByLearner(String learnerId);
    List<Notification> getUnreadNotifications(String learnerId);
    long getUnreadCount(String learnerId);
    void markAsRead(Long notificationId);
    void markAllAsRead(String learnerId);
    void notifyWafaRefund(String learnerId, Double amount,
                          String courseTitle, String phoneNumber);
    void notifyBakchichCodeGenerated(String learnerId, String code,
                                     Double amount, String phoneNumber);
    void notifyBakchichConfirmed(String learnerId, Double amount, String code);
    void sendInvoiceEmail(String learnerId, tn.esprit.mucroservice.msenrollment.entities.Invoice invoice);

}