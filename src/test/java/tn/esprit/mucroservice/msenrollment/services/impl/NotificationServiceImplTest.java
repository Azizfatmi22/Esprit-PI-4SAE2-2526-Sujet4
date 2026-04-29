package tn.esprit.mucroservice.msenrollment.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.Notification;
import tn.esprit.mucroservice.msenrollment.entities.NotificationType;
import tn.esprit.mucroservice.msenrollment.repositories.NotificationRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEmailService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // =========================
    // 1. PAYMENT SUCCESS TEST
    // =========================
    @Test
    void shouldNotifyPaymentSuccess() {

        String learnerId = "L1";
        Double amount = 100.0;
        String invoice = "INV-1";
        Long paymentId = 1L;

        when(notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                learnerId, NotificationType.PAYMENT_SUCCESS, paymentId
        )).thenReturn(false);

        when(emailService.buildPaymentSuccessEmail(anyString(), anyDouble(), anyString()))
                .thenReturn("HTML");

        notificationService.notifyPaymentSuccess(learnerId, amount, invoice, paymentId);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendEmail(anyString(), anyString(), eq("HTML"));
    }

    // =========================
    // 2. DUPLICATE PREVENTION
    // =========================
    @Test
    void shouldNotSendIfPaymentAlreadyExists() {

        when(notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                anyString(), any(), anyLong()
        )).thenReturn(true);

        notificationService.notifyPaymentSuccess("L1", 100.0, "INV", 1L);

        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    // =========================
    // 3. CART ABANDONED
    // =========================
    @Test
    void shouldNotifyCartAbandoned() {

        when(emailService.buildCartAbandonedEmail(anyString(), anyInt()))
                .thenReturn("HTML");

        notificationService.notifyCartAbandoned("L1", 3);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendEmail(anyString(), anyString(), eq("HTML"));
    }

    // =========================
    // 4. INSTALLMENT REMINDER
    // =========================
    @Test
    void shouldNotifyInstallmentReminder() {

        when(notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                anyString(), any(), anyLong()
        )).thenReturn(false);

        when(emailService.buildInstallmentReminderEmail(anyString(), anyDouble(), anyString(), anyInt()))
                .thenReturn("HTML");

        notificationService.notifyInstallmentReminder(
                "L1", 50.0, "2026-04-30", 1, 10L
        );

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendEmail(anyString(), contains("Rappel"), eq("HTML"));
    }

    // =========================
    // 5. OVERDUE
    // =========================
    @Test
    void shouldNotifyOverdue() {

        when(notificationRepository.existsByLearnerIdAndTypeAndRelatedId(
                anyString(), any(), anyLong()
        )).thenReturn(false);

        when(emailService.buildOverdueEmail(anyDouble(), anyInt()))
                .thenReturn("HTML");

        notificationService.notifyInstallmentOverdue(
                "L1", 50.0, 2, 10L
        );

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendEmail(anyString(), contains("retard"), eq("HTML"));
    }

    // =========================
    // 6. OTP EMAIL
    // =========================
    @Test
    void shouldSendOtpEmail() {

        notificationService.sendOtpEmail("L1", "123456", "12345678");

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService).sendEmail(anyString(), contains("Flouci"), anyString());
    }

    // =========================
    // 7. MARK AS READ
    // =========================
    @Test
    void shouldMarkAsRead() {

        Notification notification = new Notification();
        notification.setIsRead(false);

        when(notificationRepository.findById(1L))
                .thenReturn(java.util.Optional.of(notification));

        notificationService.markAsRead(1L);

        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
    }
}