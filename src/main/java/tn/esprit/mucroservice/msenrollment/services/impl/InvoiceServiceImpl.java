package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.Invoice;
import tn.esprit.mucroservice.msenrollment.entities.InvoiceStatus;
import tn.esprit.mucroservice.msenrollment.repositories.InvoiceRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IInvoiceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class InvoiceServiceImpl implements IInvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    // ✅ Méthode à 4 params — délègue à la méthode à 5 params avec planId = null
    @Override
    public Invoice generateInvoice(String learnerId, Long paymentId, Double amount,
                                   List<String> courses) {
        return generateInvoice(learnerId, paymentId, amount, courses, null);
    }

    // ✅ Méthode à 5 params — méthode principale
    @Override
    public Invoice generateInvoice(String learnerId, Long paymentId, Double amount,
                                   List<String> courses, Long installmentPlanId) {
        System.out.println("===========================================");
        System.out.println("generateInvoice appelé avec installmentPlanId = " + installmentPlanId);
        System.out.println("learnerId = " + learnerId + " | paymentId = " + paymentId);
        System.out.println("===========================================");
        try {
            Invoice invoice = Invoice.builder()
                    .learnerId(learnerId)
                    .paymentId(paymentId)
                    .totalAmount(amount)
                    .purchasedCourses(courses)
                    .issueDate(LocalDateTime.now())
                    .currency("TND")
                    .status(InvoiceStatus.PAID)
                    .installmentPlanId(installmentPlanId) // null = direct, planId = installment
                    .build();

            String uniqueID = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            invoice.setInvoiceNumber("INV-" + LocalDateTime.now().getYear() + "-" + uniqueID);

            Invoice savedInvoice = invoiceRepository.save(invoice);
            System.out.println("Facture générée: " + savedInvoice.getInvoiceNumber()
                    + " | planId: " + installmentPlanId);
            return savedInvoice;

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération de la facture: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération de la facture: " + e.getMessage(), e);
        }
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'ID : " + id));
    }

    @Override
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec le numéro : " + invoiceNumber));
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public List<Invoice> getInvoicesByLearner(String learnerId) {
        return invoiceRepository.findByLearnerId(learnerId);
    }

    @Override
    public List<Invoice> getDirectInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .filter(i -> i.getInstallmentPlanId() == null)
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> getInstallmentInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .filter(i -> i.getInstallmentPlanId() != null)
                .collect(Collectors.toList());
    }
    @Override
    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }
    @Override
    public List<Invoice> getAllInvoicesByLearner(String learnerId) {
        return invoiceRepository.findByLearnerId(learnerId);
    }
    // ✅ NOUVELLE méthode — avec coupon (discountAmount + originalAmount)

    @Override
    public Invoice generateInvoice(String learnerId, Long paymentId, Double amount,
                                   List<String> courses, String couponCode,
                                   Double discountAmount, Double originalAmount) {
        Invoice invoice = Invoice.builder()
                .learnerId(learnerId)
                .paymentId(paymentId)
                .totalAmount(amount)
                .purchasedCourses(courses)
                .issueDate(LocalDateTime.now())
                .currency("TND")
                .status(InvoiceStatus.PAID)
                .installmentPlanId(null)
                .couponCode(couponCode)         // ✅ OBLIGATOIRE
                .discountAmount(discountAmount) // ✅ OBLIGATOIRE
                .originalAmount(originalAmount) // ✅ OBLIGATOIRE
                .build();

        String uniqueID = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        invoice.setInvoiceNumber("INV-" + LocalDateTime.now().getYear() + "-" + uniqueID);
        return invoiceRepository.save(invoice);
    }
}