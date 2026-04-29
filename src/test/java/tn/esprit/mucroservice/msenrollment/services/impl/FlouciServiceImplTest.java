package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.FlouciTransactionRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlouciServiceImplTest {

    @Mock
    private FlouciTransactionRepository flouciRepo;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private FlouciServiceImpl flouciService;

    private FlouciTransaction tx;

    @BeforeEach
    void setUp() {
        tx = new FlouciTransaction();
        tx.setTransactionRef("REF123");
        tx.setLearnerId("learner1");
        tx.setPhoneNumber("12345678");
        tx.setOtpCode("111111");
        tx.setOtpExpiry(LocalDateTime.now().plusMinutes(2));
        tx.setStatus(FlouciStatus.PENDING_OTP);
    }

    @Test
    void shouldInitiatePayment() {
        when(flouciRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        FlouciTransaction result =
                flouciService.initiatePayment("learner1", "12345678", 50.0);

        assertNotNull(result.getOtpCode());
        assertEquals(FlouciStatus.PENDING_OTP, result.getStatus());

        verify(flouciRepo).save(any());
        verify(notificationService).sendOtpEmail(any(), any(), any());
    }

    @Test
    void shouldVerifyCorrectOtp() {
        when(flouciRepo.findByTransactionRef("REF123"))
                .thenReturn(Optional.of(tx));

        boolean result = flouciService.verifyOtp("REF123", "111111");

        assertTrue(result);
        assertEquals(FlouciStatus.VERIFIED, tx.getStatus());
    }

    @Test
    void shouldRejectWrongOtp() {
        when(flouciRepo.findByTransactionRef("REF123"))
                .thenReturn(Optional.of(tx));

        boolean result = flouciService.verifyOtp("REF123", "000000");

        assertFalse(result);
        assertEquals(FlouciStatus.FAILED, tx.getStatus());
    }

    @Test
    void shouldRejectExpiredOtp() {
        tx.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

        when(flouciRepo.findByTransactionRef("REF123"))
                .thenReturn(Optional.of(tx));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                flouciService.verifyOtp("REF123", "111111"));

        assertTrue(ex.getMessage().contains("OTP expiré"));
        assertEquals(FlouciStatus.EXPIRED, tx.getStatus());
    }

    @Test
    void shouldResendOtp() {
        when(flouciRepo.findByTransactionRef("REF123"))
                .thenReturn(Optional.of(tx));

        FlouciTransaction result = flouciService.resendOtp("REF123");

        assertNotNull(result.getOtpCode());
        assertEquals(FlouciStatus.PENDING_OTP, result.getStatus());

        verify(notificationService).sendOtpEmail(any(), any(), any());
    }

    @Test
    void shouldReturnHistory() {
        when(flouciRepo.findByLearnerIdOrderByCreatedAtDesc("learner1"))
                .thenReturn(List.of(tx));

        List<FlouciTransaction> result =
                flouciService.getHistory("learner1");

        assertEquals(1, result.size());
    }
}