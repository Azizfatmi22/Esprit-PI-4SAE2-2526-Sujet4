package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.mucroservice.msenrollment.DTO.*;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.*;
import tn.esprit.mucroservice.msenrollment.services.interfaces.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InstallmentServiceImpl implements IInstallmentService {

    @Autowired private InstallmentPlanRepository planRepository;
    @Autowired private InstallmentRepository installmentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private IInvoiceService invoiceService;
    @Autowired private ICartService cartService;
    @Autowired private IEnrollmentService enrollmentService;
    @Autowired private INotificationService notificationService;
    @Autowired private NotificationServiceImpl notificationServiceImpl;

    private static final int INSTALLMENTS_3X = 3;
    private static final int INSTALLMENTS_6X = 6;
    private static final double FEE_3X = 0.0;
    private static final double FEE_6X = 5.0;
    private static final int DAYS_BETWEEN_INSTALLMENTS = 30;

    // ----------------------------------------------------------------
    // CRÉATION DU PLAN
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public InstallmentPlanResponse createInstallmentPlan(
            InstallmentPlanRequest request,
            List<String> courseTitles) {

        // 1. Validation
        if (request.getNumberOfInstallments() != INSTALLMENTS_3X
                && request.getNumberOfInstallments() != INSTALLMENTS_6X) {
            throw new IllegalArgumentException("Nombre d'échéances invalide. Choisissez 3 ou 6.");
        }

        // 2. Calcul des frais
        double feePercentage = (request.getNumberOfInstallments() == INSTALLMENTS_6X)
                ? FEE_6X : FEE_3X;
        double feeAmount = request.getTotalAmount() * (feePercentage / 100.0);
        double totalWithFees = request.getTotalAmount() + feeAmount;
        double installmentAmount = totalWithFees / request.getNumberOfInstallments();

        // 3. Créer le Payment initial
        Payment payment = new Payment();
        payment.setLearnerId(request.getLearnerId());
        payment.setAmount(installmentAmount);
        payment.setMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment = paymentRepository.save(payment);

        // 4. Créer le plan
        InstallmentPlan plan = new InstallmentPlan();
        plan.setPayment(payment);
        plan.setLearnerId(request.getLearnerId());
        plan.setTotalAmount(request.getTotalAmount());
        plan.setNumberOfInstallments(request.getNumberOfInstallments());
        plan.setFeePercentage(feePercentage);
        plan.setAmountWithFees(totalWithFees);
        plan.setInstallmentAmount(Math.round(installmentAmount * 100.0) / 100.0);
        plan.setStatus(InstallmentPlanStatus.ACTIVE);
        plan.setCreatedAt(new Date());
        plan = planRepository.save(plan);

        // 5. Générer les échéances
        List<Installment> installments = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        for (int i = 1; i <= request.getNumberOfInstallments(); i++) {
            Installment installment = new Installment();
            installment.setInstallmentPlan(plan);
            installment.setInstallmentNumber(i);
            installment.setAmount(Math.round(installmentAmount * 100.0) / 100.0);

            Date dueDate = new Date(calendar.getTime().getTime()
                    + (long)(i - 1) * DAYS_BETWEEN_INSTALLMENTS * 24 * 60 * 60 * 1000);
            installment.setDueDate(dueDate);

            if (i == 1) {
                // ✅ Pour Bakchich : 1ère échéance reste PENDING
                // Pour Flouci/Wafa : 1ère échéance PAID immédiatement
                if ("BAKCHICH".equalsIgnoreCase(request.getPaymentMethod())) {
                    installment.setStatus(InstallmentStatus.PENDING);
                    // ✅ Pas de mail, pas de facture ici — ça se fait à la confirmation admin
                } else {
                    installment.setStatus(InstallmentStatus.PAID);
                    installment.setPaidDate(new Date());
                    installment.setTransactionId(payment.getTransactionId());

                    Invoice invoice = invoiceService.generateInvoice(
                            request.getLearnerId(),
                            payment.getId(),
                            installmentAmount,
                            courseTitles,
                            plan.getId()  // ✅ plan est la variable correcte
                    );
                    installment.setInvoiceNumber(invoice.getInvoiceNumber());

                    notificationService.notifyPaymentSuccess(
                            request.getLearnerId(),
                            installmentAmount,
                            invoice.getInvoiceNumber(),
                            payment.getId()
                    );
                }
            } else {
                installment.setStatus(InstallmentStatus.PENDING);
            }
            installments.add(installmentRepository.save(installment));
        }

        // 6. Enrollments + vider panier
        Cart cart = cartService.getCartByLearnerId(request.getLearnerId());
        if (!"BAKCHICH".equalsIgnoreCase(request.getPaymentMethod())) {
            for (CartItem item : cart.getItems()) {
                enrollmentService.createEnrollment(request.getLearnerId(), item.getCourseId());
            }

            cartService.clearCart(request.getLearnerId());
        } else {
            System.out.println("Panier déjà vide pour learnerId: " + request.getLearnerId()
                    + " — enrollments déjà créés");
        }

        return buildResponse(plan, installments);

    } // ✅ FIN createInstallmentPlan

    // ----------------------------------------------------------------
    // PAIEMENT D'UNE ÉCHÉANCE
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public InstallmentDTO payInstallment(Long installmentId, PayInstallmentRequest request) {

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Échéance introuvable: " + installmentId));

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new RuntimeException("Cette échéance est déjà payée.");
        }

        InstallmentPlan plan = installment.getInstallmentPlan();

        List<Installment> nextPending = installmentRepository
                .findNextPendingInstallment(plan.getId());

        if (nextPending.isEmpty() || !nextPending.get(0).getId().equals(installmentId)) {
            throw new RuntimeException(
                    "Vous devez payer les échéances dans l'ordre. " +
                            "Prochaine échéance: #" + (nextPending.isEmpty() ? "N/A"
                            : nextPending.get(0).getInstallmentNumber())
            );
        }

        Payment payment = new Payment();
        payment.setLearnerId(plan.getLearnerId());
        payment.setAmount(installment.getAmount());
        payment.setMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment = paymentRepository.save(payment);

        installment.setStatus(InstallmentStatus.PAID);
        installment.setPaidDate(new Date());
        installment.setTransactionId(payment.getTransactionId());

        List<String> label = Collections.singletonList(
                "Échéance " + installment.getInstallmentNumber()
                        + "/" + plan.getNumberOfInstallments()
        );
        Invoice invoice = invoiceService.generateInvoice(
                plan.getLearnerId(),
                payment.getId(),
                installment.getAmount(),
                label,
                plan.getId()
        );
        installment.setInvoiceNumber(invoice.getInvoiceNumber());
        installmentRepository.save(installment);

        List<Installment> allInstallments = installmentRepository
                .findByInstallmentPlanId(plan.getId());
        boolean allPaid = allInstallments.stream()
                .allMatch(i -> i.getStatus() == InstallmentStatus.PAID);

        if (allPaid) {
            plan.setStatus(InstallmentPlanStatus.COMPLETED);
            planRepository.save(plan);
        }

        if (plan.getStatus() == InstallmentPlanStatus.DEFAULTED) {
            plan.setStatus(InstallmentPlanStatus.ACTIVE);
            planRepository.save(plan);
        }

        return toDTO(installment);

    } // ✅ FIN payInstallment

    // ----------------------------------------------------------------
    // SCHEDULER
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public void checkAndMarkOverdueInstallments() {
        Date today = new Date();

        List<Installment> overdueInstallments = installmentRepository
                .findOverdueInstallments(today, InstallmentStatus.PENDING);

        for (Installment installment : overdueInstallments) {
            installment.setStatus(InstallmentStatus.OVERDUE);
            installmentRepository.save(installment);

            InstallmentPlan plan = installment.getInstallmentPlan();
            if (plan.getStatus() == InstallmentPlanStatus.ACTIVE) {
                plan.setStatus(InstallmentPlanStatus.DEFAULTED);
                planRepository.save(plan);
                System.out.println("Plan " + plan.getId() + " marqué DEFAULTED - " +
                        "accès bloqué pour learnerId: " + plan.getLearnerId());
            }
        }
    } // ✅ FIN checkAndMarkOverdueInstallments

    // ----------------------------------------------------------------
    // VÉRIFICATION D'ACCÈS
    // ----------------------------------------------------------------
    @Override
    public boolean hasAccessToCourse(String learnerId, Long courseId) {
        List<InstallmentPlan> plans = planRepository.findByLearnerId(learnerId);

        boolean hasDefaultedPlan = plans.stream()
                .anyMatch(p -> p.getStatus() == InstallmentPlanStatus.DEFAULTED);

        return !hasDefaultedPlan;
    }

    // ----------------------------------------------------------------
    // CONSULTATION
    // ----------------------------------------------------------------
    @Override
    public InstallmentPlanResponse getPlanById(Long planId) {
        InstallmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable: " + planId));
        List<Installment> installments = installmentRepository.findByInstallmentPlanId(planId);
        return buildResponse(plan, installments);
    }

    @Override
    public List<InstallmentPlanResponse> getPlansByLearner(String learnerId) {
        return planRepository.findByLearnerId(learnerId).stream()
                .map(plan -> {
                    List<Installment> installments =
                            installmentRepository.findByInstallmentPlanId(plan.getId());
                    return buildResponse(plan, installments);
                })
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // HELPERS
    // ----------------------------------------------------------------
    private InstallmentPlanResponse buildResponse(InstallmentPlan plan,
                                                  List<Installment> installments) {
        InstallmentPlanResponse response = new InstallmentPlanResponse();
        response.setLearnerId(plan.getLearnerId());
        response.setCreatedAt(plan.getCreatedAt());
        response.setPlanId(plan.getId());
        response.setTotalAmount(plan.getTotalAmount());
        response.setAmountWithFees(plan.getAmountWithFees());
        response.setFeePercentage(plan.getFeePercentage());
        response.setNumberOfInstallments(plan.getNumberOfInstallments());
        response.setInstallmentAmount(plan.getInstallmentAmount());
        response.setStatus(plan.getStatus());
        response.setInstallments(installments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
        return response;
    }

    private InstallmentDTO toDTO(Installment installment) {
        InstallmentDTO dto = new InstallmentDTO();
        dto.setId(installment.getId());
        dto.setInstallmentNumber(installment.getInstallmentNumber());
        dto.setAmount(installment.getAmount());
        dto.setDueDate(installment.getDueDate());
        dto.setPaidDate(installment.getPaidDate());
        dto.setStatus(installment.getStatus());
        dto.setInvoiceNumber(installment.getInvoiceNumber());
        return dto;
    }
    @Override
    @Transactional
    public void deletePlan(Long planId) {
        InstallmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan introuvable: " + planId));
        // Supprimer les échéances liées
        installmentRepository.deleteByInstallmentPlanId(planId);
        planRepository.delete(plan);
    }
    @Override
    public List<InstallmentSummaryDTO> getInstallmentSummary(String learnerId) {
        List<InstallmentPlan> plans = planRepository.findByLearnerId(learnerId);
        return plans.stream().map(plan -> {
            List<Installment> installments = plan.getInstallments();

            double amountPaid = installments.stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                    .mapToDouble(Installment::getAmount).sum();

            double amountRemaining = plan.getAmountWithFees() - amountPaid;

            long paidCount = installments.stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.PAID).count();

            // Prochaine échéance
            Installment next = installments.stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                            || i.getStatus() == InstallmentStatus.OVERDUE)
                    .min(Comparator.comparing(Installment::getDueDate))
                    .orElse(null);

            return new InstallmentSummaryDTO(
                    plan.getId(),
                    plan.getTotalAmount(),
                    amountPaid,
                    amountRemaining,
                    plan.getNumberOfInstallments(),
                    (int) paidCount,
                    (int)(plan.getNumberOfInstallments() - paidCount),
                    plan.getStatus().name(),
                    next != null ? next.getDueDate().toString() : null,
                    next != null ? next.getAmount() : null
            );
        }).collect(Collectors.toList());
    }

    @Override
    public List<InstallmentPlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(plan -> {
                    List<Installment> installments =
                            installmentRepository.findByInstallmentPlanId(plan.getId());
                    return buildResponse(plan, installments);
                })
                .collect(Collectors.toList());
    }


} // ✅ FIN de la classe