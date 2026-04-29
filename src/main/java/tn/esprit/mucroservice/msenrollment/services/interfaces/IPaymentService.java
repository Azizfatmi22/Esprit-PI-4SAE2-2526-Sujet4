package tn.esprit.mucroservice.msenrollment.services.interfaces;

import jakarta.transaction.Transactional;
import tn.esprit.mucroservice.msenrollment.entities.Payment;
import tn.esprit.mucroservice.msenrollment.entities.PaymentMethod;

import java.util.List;

public interface IPaymentService {

    @Transactional
    Payment processPayment(String learnerId, Double amount, PaymentMethod method);
    List<Payment> getAllPayments();
    List<Payment> getPaymentsByLearner(String learnerId);
    void deletePayment(Long id);

}