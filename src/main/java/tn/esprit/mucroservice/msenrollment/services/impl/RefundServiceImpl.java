package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.Invoice;
import tn.esprit.mucroservice.msenrollment.entities.InvoiceStatus;
import tn.esprit.mucroservice.msenrollment.entities.RefundRequest;
import tn.esprit.mucroservice.msenrollment.entities.RefundStatus;
import tn.esprit.mucroservice.msenrollment.repositories.RefundRequestRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IEnrollmentService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IInvoiceService;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IRefundService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefundServiceImpl implements IRefundService {

    @Autowired
    private RefundRequestRepository refundRepository;
    @Autowired private IInvoiceService invoiceService;
    @Autowired private IEnrollmentService enrollmentService;

    @Override
    public RefundRequest createRefundRequest(String learnerId, Long invoiceId, String reason) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        // Vérifier délai 14 jours
        if (invoice.getIssueDate().plusDays(14).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Délai de remboursement dépassé (14 jours)");
        }

        // Vérifier pas déjà une demande en cours
        boolean alreadyRequested = refundRepository
                .findByInvoiceIdAndStatus(invoiceId, RefundStatus.PENDING).isPresent();
        if (alreadyRequested) {
            throw new RuntimeException("Une demande de remboursement est déjà en cours");
        }

        RefundRequest refund = RefundRequest.builder()
                .learnerId(learnerId)
                .invoiceId(invoiceId)
                .paymentId(invoice.getPaymentId())
                .reason(reason)
                .refundAmount(invoice.getTotalAmount())
                .status(RefundStatus.PENDING)
                .requestDate(LocalDateTime.now())
                .build();

        return refundRepository.save(refund);
    }

    @Override
    public RefundRequest approveRefund(Long refundId, String adminName) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));

        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedDate(LocalDateTime.now());
        refund.setProcessedBy(adminName);

        // Générer numéro d'avoir
        String creditNote = "AVOIR-" + LocalDateTime.now().getYear()
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        refund.setCreditNoteNumber(creditNote);

        // Mettre à jour le statut de la facture
        Invoice invoice = invoiceService.getInvoiceById(refund.getInvoiceId());
        invoice.setStatus(InvoiceStatus.REFUNDED);
        // invoiceRepository.save(invoice) — via service

        // Révoquer les enrollments
        enrollmentService.getEnrollmentsByLearner(refund.getLearnerId())
                .forEach(e -> enrollmentService.cancelEnrollment(e.getId()));

        return refundRepository.save(refund);
    }

    @Override
    public RefundRequest rejectRefund(Long refundId, String adminName, String reason) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));

        refund.setStatus(RefundStatus.REJECTED);
        refund.setProcessedDate(LocalDateTime.now());
        refund.setProcessedBy(adminName);
        refund.setRejectionReason(reason);

        return refundRepository.save(refund);
    }

    @Override
    public List<RefundRequest> getPendingRefunds() {
        return refundRepository.findByStatus(RefundStatus.PENDING);
    }

    @Override
    public List<RefundRequest> getRefundsByLearner(String learnerId) {
        return refundRepository.findByLearnerId(learnerId);
    }
    @Override
    public List<RefundRequest> getAllRefunds() {
        return refundRepository.findAll();  // ✅ refundRepository, pas refundRequestRepository
    }
}