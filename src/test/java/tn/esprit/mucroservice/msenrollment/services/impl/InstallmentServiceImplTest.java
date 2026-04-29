package tn.esprit.mucroservice.msenrollment.services.impl;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.DTO.InstallmentDTO;
import tn.esprit.mucroservice.msenrollment.DTO.InstallmentPlanRequest;
import tn.esprit.mucroservice.msenrollment.DTO.InstallmentPlanResponse;
import tn.esprit.mucroservice.msenrollment.DTO.PayInstallmentRequest;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.*;
import tn.esprit.mucroservice.msenrollment.services.interfaces.ICartService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEnrollmentService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IInvoiceService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.INotificationService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceImplTest {

    @Mock private InstallmentPlanRepository planRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private IInvoiceService invoiceService;
    @Mock private ICartService cartService;
    @Mock private IEnrollmentService enrollmentService;
    @Mock private INotificationService notificationService;

    @InjectMocks
    private InstallmentServiceImpl installmentService;

    // =========================
    // 1. CREATE INSTALLMENT PLAN
    // =========================
    @Test
    void shouldCreateInstallmentPlanSuccessfully() {

        InstallmentPlanRequest request = new InstallmentPlanRequest();
        request.setLearnerId("L1");
        request.setTotalAmount(300.0);
        request.setNumberOfInstallments(3);
        request.setPaymentMethod("BAKCHICH");

        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());

        when(paymentRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(planRepository.save(any()))
                .thenAnswer(i -> {
                    InstallmentPlan p = i.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        // 🔥 FIX IMPORTANT: éviter null installment
        when(installmentRepository.save(any()))
                .thenAnswer(i -> {
                    Installment ins = i.getArgument(0);
                    ins.setId(10L);
                    return ins;
                });

        when(cartService.getCartByLearnerId("L1"))
                .thenReturn(cart);

        InstallmentPlanResponse response =
                installmentService.createInstallmentPlan(request, List.of("Course1"));

        assertNotNull(response);
        assertEquals("L1", response.getLearnerId());
        assertEquals(3, response.getNumberOfInstallments());

        verify(planRepository, atLeastOnce()).save(any());
        verify(installmentRepository, atLeastOnce()).save(any());
    }
    // =========================
    // 2. INVALID INSTALLMENTS
    // =========================
    @Test
    void shouldThrowExceptionForInvalidInstallments() {

        InstallmentPlanRequest request = new InstallmentPlanRequest();
        request.setNumberOfInstallments(5); // ❌ invalide

        assertThrows(IllegalArgumentException.class,
                () -> installmentService.createInstallmentPlan(request, List.of()));
    }

    // =========================
    // 3. PAY INSTALLMENT SUCCESS
    // =========================
    // =========================
// 3. PAY INSTALLMENT SUCCESS
// =========================
    @Test
    void shouldPayInstallmentSuccessfully() {

        InstallmentPlan plan = new InstallmentPlan();
        plan.setId(1L);
        plan.setLearnerId("L1");
        plan.setNumberOfInstallments(3);
        plan.setStatus(InstallmentPlanStatus.ACTIVE);

        Installment installment = new Installment();
        installment.setId(10L);
        installment.setAmount(100.0);
        installment.setInstallmentNumber(1);
        installment.setStatus(InstallmentStatus.PENDING);
        installment.setInstallmentPlan(plan);

        when(installmentRepository.findById(10L))
                .thenReturn(Optional.of(installment));

        when(installmentRepository.findNextPendingInstallment(1L))
                .thenReturn(List.of(installment));

        when(paymentRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(invoiceService.generateInvoice(any(), any(), any(), any(), any()))
                .thenReturn(new Invoice());

        when(installmentRepository.findByInstallmentPlanId(1L))
                .thenReturn(List.of(installment));

        // 🔥 FIX ENUM ICI
        PayInstallmentRequest request = new PayInstallmentRequest();
        request.setPaymentMethod("BAKCHICH"); // ❌ PAS CARD

        InstallmentDTO dto =
                installmentService.payInstallment(10L, request);

        assertNotNull(dto);
        assertEquals(InstallmentStatus.PAID, installment.getStatus());

        verify(installmentRepository).save(any());
    }
    // =========================
    // 4. INSTALLMENT ALREADY PAID
    // =========================
    @Test
    void shouldThrowIfAlreadyPaid() {

        Installment installment = new Installment();
        installment.setId(1L);
        installment.setStatus(InstallmentStatus.PAID);

        when(installmentRepository.findById(1L))
                .thenReturn(Optional.of(installment));

        PayInstallmentRequest request = new PayInstallmentRequest();
        request.setPaymentMethod("CARD");

        assertThrows(RuntimeException.class,
                () -> installmentService.payInstallment(1L, request));
    }
    // =========================
    // 5. ACCESS BLOCKED IF DEFAULTED PLAN
    // =========================
    @Test
    void shouldBlockAccessIfPlanDefaulted() {

        InstallmentPlan plan = new InstallmentPlan();
        plan.setStatus(InstallmentPlanStatus.DEFAULTED);

        when(planRepository.findByLearnerId("L1"))
                .thenReturn(List.of(plan));

        boolean access = installmentService.hasAccessToCourse("L1", 1L);

        assertFalse(access);
    }

    // =========================
    // 6. DELETE PLAN
    // =========================
    @Test
    void shouldDeletePlanSuccessfully() {

        InstallmentPlan plan = new InstallmentPlan();
        plan.setId(1L);

        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));

        doNothing().when(installmentRepository).deleteByInstallmentPlanId(1L);
        doNothing().when(planRepository).delete(plan);

        installmentService.deletePlan(1L);

        verify(planRepository).delete(plan);
    }
}