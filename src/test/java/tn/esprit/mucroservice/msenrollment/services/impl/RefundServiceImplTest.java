package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.RefundRequestRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEnrollmentService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IInvoiceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private RefundRequestRepository refundRepository;

    @Mock
    private IInvoiceService invoiceService;

    @Mock
    private IEnrollmentService enrollmentService;

    @InjectMocks
    private RefundServiceImpl refundService;

    // =========================
    // 1. CREATE REFUND SUCCESS
    // =========================
    @Test
    void shouldCreateRefundRequestSuccessfully() {

        Invoice invoice = Invoice.builder()
                .id(1L)
                .paymentId(10L)
                .totalAmount(200.0)
                .issueDate(LocalDateTime.now())
                .build();

        when(invoiceService.getInvoiceById(1L)).thenReturn(invoice);
        when(refundRepository.findByInvoiceIdAndStatus(1L, RefundStatus.PENDING))
                .thenReturn(Optional.empty());

        RefundRequest saved = RefundRequest.builder()
                .id(1L)
                .learnerId("L1")
                .invoiceId(1L)
                .refundAmount(200.0)
                .status(RefundStatus.PENDING)
                .build();

        when(refundRepository.save(any(RefundRequest.class))).thenReturn(saved);

        RefundRequest result = refundService.createRefundRequest(
                "L1", 1L, "Reason"
        );

        assertNotNull(result);
        assertEquals(RefundStatus.PENDING, result.getStatus());

        verify(refundRepository).save(any(RefundRequest.class));
    }

    // =========================
    // 2. REFUND EXPIRED
    // =========================
    @Test
    void shouldThrowWhenRefundPeriodExpired() {

        Invoice invoice = Invoice.builder()
                .id(1L)
                .issueDate(LocalDateTime.now().minusDays(20))
                .build();

        when(invoiceService.getInvoiceById(1L)).thenReturn(invoice);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                refundService.createRefundRequest("L1", 1L, "reason")
        );

        assertTrue(ex.getMessage().contains("Délai de remboursement dépassé"));
    }

    // =========================
    // 3. DUPLICATE REFUND
    // =========================
    @Test
    void shouldThrowWhenRefundAlreadyExists() {

        Invoice invoice = Invoice.builder()
                .id(1L)
                .issueDate(LocalDateTime.now())
                .build();

        when(invoiceService.getInvoiceById(1L)).thenReturn(invoice);

        when(refundRepository.findByInvoiceIdAndStatus(1L, RefundStatus.PENDING))
                .thenReturn(Optional.of(new RefundRequest()));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                refundService.createRefundRequest("L1", 1L, "reason")
        );

        assertTrue(ex.getMessage().contains("déjà en cours"));
    }

    // =========================
    // 4. APPROVE REFUND
    // =========================
    @Test
    void shouldApproveRefundSuccessfully() {

        RefundRequest refund = RefundRequest.builder()
                .id(1L)
                .learnerId("L1")
                .invoiceId(1L)
                .status(RefundStatus.PENDING)
                .build();

        Invoice invoice = Invoice.builder()
                .id(1L)
                .paymentId(10L)
                .totalAmount(200.0)
                .build();

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(invoiceService.getInvoiceById(1L)).thenReturn(invoice);
        when(refundRepository.save(any())).thenReturn(refund);
        when(enrollmentService.getEnrollmentsByLearner("L1"))
                .thenReturn(List.of());

        RefundRequest result = refundService.approveRefund(1L, "ADMIN");

        assertEquals(RefundStatus.PROCESSED, result.getStatus());
        assertNotNull(result.getCreditNoteNumber());

        verify(refundRepository).save(refund);
        verify(enrollmentService).getEnrollmentsByLearner("L1");
    }

    // =========================
    // 5. REJECT REFUND
    // =========================
    @Test
    void shouldRejectRefundSuccessfully() {

        RefundRequest refund = RefundRequest.builder()
                .id(1L)
                .status(RefundStatus.PENDING)
                .build();

        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any())).thenReturn(refund);

        RefundRequest result = refundService.rejectRefund(1L, "ADMIN", "Invalid reason");

        assertEquals(RefundStatus.REJECTED, result.getStatus());
        assertEquals("Invalid reason", result.getRejectionReason());

        verify(refundRepository).save(refund);
    }

    // =========================
    // 6. LIST PENDING
    // =========================
    @Test
    void shouldReturnPendingRefunds() {

        when(refundRepository.findByStatus(RefundStatus.PENDING))
                .thenReturn(List.of(new RefundRequest()));

        List<RefundRequest> result = refundService.getPendingRefunds();

        assertEquals(1, result.size());
    }

    // =========================
    // 7. LIST BY LEARNER
    // =========================
    @Test
    void shouldReturnRefundsByLearner() {

        when(refundRepository.findByLearnerId("L1"))
                .thenReturn(List.of(new RefundRequest()));

        List<RefundRequest> result = refundService.getRefundsByLearner("L1");

        assertEquals(1, result.size());
    }

    // =========================
    // 8. GET ALL REFUNDS
    // =========================
    @Test
    void shouldReturnAllRefunds() {

        when(refundRepository.findAll())
                .thenReturn(List.of(new RefundRequest(), new RefundRequest()));

        List<RefundRequest> result = refundService.getAllRefunds();

        assertEquals(2, result.size());
    }
}