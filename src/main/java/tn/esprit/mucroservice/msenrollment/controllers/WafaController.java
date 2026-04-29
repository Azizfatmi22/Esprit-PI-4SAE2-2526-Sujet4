package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.PaymentRepository;
import tn.esprit.mucroservice.msenrollment.repositories.WafaRefundRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msenrollment/wafa")
public class WafaController {

    @Autowired
    private IWafaService wafaService;
    @Autowired private ICartService cartService;
    @Autowired private IPaymentService paymentService;
    @Autowired private IInvoiceService invoiceService;
    @Autowired private INotificationService notificationService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private WafaRefundRepository wafaRefundRepository;

    // ✅ Vérifier le solde
    @GetMapping("/balance/{phoneNumber}")
    public ResponseEntity<?> getBalance(@PathVariable String phoneNumber) {
        try {
            Double balance = wafaService.getBalance(phoneNumber);
            Map<String, Object> response = new HashMap<>();
            response.put("phoneNumber", phoneNumber);
            response.put("balance", balance);
            response.put("currency", "TND");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Vérifier si solde suffisant pour un montant
    @GetMapping("/check-balance/{phoneNumber}")
    public ResponseEntity<?> checkBalance(
            @PathVariable String phoneNumber,
            @RequestParam Double amount) {
        try {
            // Créer le compte si inexistant (simulation)
            wafaService.getOrCreateAccount(phoneNumber, null);

            Double balance = wafaService.getBalance(phoneNumber);
            boolean sufficient = balance >= amount;

            Map<String, Object> response = new HashMap<>();
            response.put("balance", balance);
            response.put("requiredAmount", amount);
            response.put("sufficient", sufficient);
            response.put("currency", "TND");

            if (!sufficient) {
                response.put("message",
                        "Solde insuffisant. Disponible : " + balance
                                + " TND, Requis : " + amount + " TND"
                );
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Confirmer le paiement Wafa Cash
    @PostMapping("/pay/{learnerId}")
    public ResponseEntity<?> pay(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> body) {
        try {
            String phoneNumber = body.get("phoneNumber").toString();
            Double amount = Double.valueOf(body.get("amount").toString());

            // Débiter le compte
            wafaService.debitAccount(phoneNumber, amount);

            // Finaliser le paiement
            Cart cart = cartService.getCartByLearnerId(learnerId);
            Payment payment = paymentService.processPayment(
                    learnerId, amount, PaymentMethod.WAFA_CASH
            );

            List<String> titles = cart.getItems().stream()
                    .map(CartItem::getCourseTitle)
                    .collect(Collectors.toList());

            Invoice invoice = invoiceService.generateInvoice(
                    learnerId, payment.getId(), amount, titles
            );


            cartService.clearCart(learnerId);

            notificationService.notifyPaymentSuccess(
                    learnerId, amount,
                    invoice.getInvoiceNumber(), payment.getId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Paiement Wafa Cash réussi !");
            response.put("invoiceNumber", invoice.getInvoiceNumber());
            response.put("paymentStatus", "SUCCESS");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // ✅ Demander un remboursement
    @PostMapping("/refund/{learnerId}")
    public ResponseEntity<?> requestRefund(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> body) {
        try {
            String paymentId = String.valueOf(body.get("paymentId").toString());
            String phoneNumber = body.get("phoneNumber").toString();
            String courseTitle = body.get("courseTitle").toString();
            Double amount = Double.valueOf(body.get("amount").toString());

            // Vérifier si le paiement existe
            Payment payment = paymentRepository.findById(Long.valueOf(paymentId))
                    .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

            // Vérifier la fenêtre de 48h
            long heuresDepuisPaiement = ChronoUnit.HOURS.between(
                    payment.getPaymentDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime(),
                    LocalDateTime.now()
            );

            if (heuresDepuisPaiement > 48) {
                return ResponseEntity.badRequest().body(
                        "Remboursement impossible : délai de 48h dépassé. "
                                + "Paiement effectué il y a " + heuresDepuisPaiement + "h."
                );
            }

            // Créer la demande de remboursement
            WafaRefund refund = new WafaRefund();
            refund.setLearnerId(learnerId);
            refund.setPhoneNumber(phoneNumber);
            refund.setAmount(amount);
            refund.setCourseTitle(courseTitle);
            refund.setPaymentId(Long.valueOf(paymentId));
            refund.setStatus(RefundStatus.PENDING);
            wafaRefundRepository.save(refund);

            // Créditer le compte immédiatement
            wafaService.creditAccount(phoneNumber, amount);

            refund.setStatus(RefundStatus.PROCESSED);
            refund.setProcessedAt(new Date());
            wafaRefundRepository.save(refund);

            // Envoyer notification
            notificationService.notifyWafaRefund(
                    learnerId, amount, courseTitle, phoneNumber
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Remboursement de " + amount + " TND effectué !");
            response.put("phoneNumber", phoneNumber);
            response.put("status", "PROCESSED");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}