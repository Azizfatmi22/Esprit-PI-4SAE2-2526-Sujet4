package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.Invoice;
import java.util.List;

public interface IInvoiceService {
    Invoice generateInvoice(String learnerId, Long paymentId, Double amount, List<String> courses);
    Invoice generateInvoice(String learnerId, Long paymentId, Double amount,
                            List<String> courses, Long installmentPlanId);
    Invoice getInvoiceById(Long id);
    Invoice getInvoiceByNumber(String invoiceNumber);
    List<Invoice> getAllInvoices();
    List<Invoice> getInvoicesByLearner(String learnerId);
    List<Invoice> getDirectInvoices();
    List<Invoice> getInstallmentInvoices();
    void deleteInvoice(Long id);
    List<Invoice> getAllInvoicesByLearner(String learnerId);
    // ✅ AJOUTER cette nouvelle signature dans l'interface
    Invoice generateInvoice(String learnerId, Long paymentId, Double amount,
                            List<String> courses, String couponCode,
                            Double discountAmount, Double originalAmount);
}