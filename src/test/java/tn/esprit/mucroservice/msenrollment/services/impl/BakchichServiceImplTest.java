package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.BakchichPaymentRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BakchichServiceImplTest {

    @Mock
    private BakchichPaymentRepository bakchichRepo;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private BakchichServiceImpl service;

    private BakchichPayment payment;

    @BeforeEach
    void setUp() {
        payment = new BakchichPayment();
        payment.setId(1L);
        payment.setLearnerId("learner1");
        payment.setAmount(100.0);
        payment.setPaymentCode("BKC-2026-ABC");
        payment.setStatus(BakchichStatus.PENDING_CASH);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        payment.setExpiresAt(cal.getTime());
    }

    @Test
    void shouldGeneratePaymentCode() {
        when(bakchichRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        BakchichPayment result = service.generatePaymentCode(
                "learner1",
                "12345678",
                50.0,
                "Java Course"
        );

        assertNotNull(result.getPaymentCode());
        assertEquals(BakchichStatus.PENDING_CASH, result.getStatus());

        verify(notificationService)
                .notifyBakchichCodeGenerated(any(), any(), any(), any());
    }

    @Test
    void shouldConfirmPayment() {
        when(bakchichRepo.findById(1L))
                .thenReturn(Optional.of(payment));

        BakchichPayment result =
                service.confirmPayment(1L, "admin");

        assertEquals(BakchichStatus.CONFIRMED, result.getStatus());
        assertEquals("admin", result.getConfirmedBy());

        verify(notificationService)
                .notifyBakchichConfirmed(any(), any(), any());
    }

    @Test
    void shouldRejectExpiredPayment() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        payment.setExpiresAt(cal.getTime());

        when(bakchichRepo.findById(1L))
                .thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.confirmPayment(1L, "admin"));

        assertTrue(ex.getMessage().contains("expiré"));
    }

    @Test
    void shouldRejectWrongStatus() {
        payment.setStatus(BakchichStatus.CANCELLED);

        when(bakchichRepo.findById(1L))
                .thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.confirmPayment(1L, "admin"));

        assertTrue(ex.getMessage().contains("ne peut pas être confirmé"));
    }

    @Test
    void shouldCancelPayment() {
        when(bakchichRepo.findById(1L))
                .thenReturn(Optional.of(payment));

        when(bakchichRepo.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        BakchichPayment result =
                service.cancelPayment(1L);

        assertEquals(BakchichStatus.CANCELLED, result.getStatus());
    }

    @Test
    void shouldReturnPendingPayments() {
        when(bakchichRepo.findByStatusOrderByCreatedAtDesc(
                BakchichStatus.PENDING_CASH))
                .thenReturn(List.of(payment));

        List<BakchichPayment> result =
                service.getPendingPayments();

        assertEquals(1, result.size());
    }

    @Test
    void shouldExpireOldPayments() {
        when(bakchichRepo.findExpiredPayments(any()))
                .thenReturn(List.of(payment));

        service.expireOldPayments();

        verify(bakchichRepo).save(payment);
        assertEquals(BakchichStatus.EXPIRED, payment.getStatus());
    }

    @Test
    void shouldGenerateInstallmentCode() {
        when(bakchichRepo.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        BakchichPayment result =
                service.generateInstallmentCode(
                        "learner1",
                        "12345678",
                        50.0,
                        "Java",
                        10L
                );

        assertEquals(10L, result.getPlanId());
        assertEquals(BakchichStatus.PENDING_CASH, result.getStatus());
    }
}