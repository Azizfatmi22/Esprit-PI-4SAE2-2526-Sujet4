package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.services.interfaces.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/msenrollment/flouci")
public class FlouciController {

    @Autowired
    private IFlouciService flouciService; // ✅ interface, pas l'impl

    @Autowired private ICartService cartService;
    @Autowired private IPaymentService paymentService;
    @Autowired private IInvoiceService invoiceService;
    @Autowired private INotificationService notificationService;
    @Autowired private ICouponService couponService;        // ✅ AJOUTER
    @Autowired private IEnrollmentService enrollmentService;

    @PostMapping("/initiate/{learnerId}")
    public ResponseEntity<?> initiate(
            @PathVariable String learnerId,
            @RequestBody Map<String, Object> body) {
        try {
            String phone = body.get("phoneNumber").toString();
            Double amount = Double.valueOf(body.get("amount").toString());

            FlouciTransaction tx = flouciService.initiatePayment(learnerId, phone, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("transactionRef", tx.getTransactionRef());
            response.put("status", tx.getStatus());
            response.put("message", "Code OTP envoyé au " + phone);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, Object> body) {
        try {
            String ref = body.get("transactionRef").toString();
            String otp = body.get("otp").toString();
            String learnerId = String.valueOf(body.get("learnerId").toString());

            boolean verified = flouciService.verifyOtp(ref, otp);
            if (!verified) {
                return ResponseEntity.badRequest().body("Code OTP incorrect");
            }

            Cart cart = cartService.getCartByLearnerId(learnerId);

            String couponCode = (body.containsKey("couponCode") && body.get("couponCode") != null)
                    ? body.get("couponCode").toString().trim()
                    : null;

            Double originalAmount = cart.getItems().stream()
                    .mapToDouble(CartItem::getCoursePrice)
                    .sum();

            Double finalAmount = originalAmount;
            Double discountAmount = 0.0;

            if (couponCode != null && !couponCode.isEmpty()) {
                CouponValidationResult couponResult = couponService.validateOnly(couponCode, learnerId, originalAmount); // ✅
                if (couponResult.isValid()) {
                    finalAmount = couponResult.getFinalAmount();
                    discountAmount = couponResult.getDiscountAmount();
                } else {
                    couponCode = null;
                }
            }

            Payment payment = paymentService.processPayment(learnerId, finalAmount, PaymentMethod.FLOUCI);

            List<String> titles = cart.getItems().stream()
                    .map(CartItem::getCourseTitle)
                    .collect(Collectors.toList());

            for (CartItem item : cart.getItems()) {
                enrollmentService.createEnrollment(learnerId, item.getCourseId()); // ✅
            }

            Invoice invoice = invoiceService.generateInvoice(
                    learnerId, payment.getId(), finalAmount, titles,
                    couponCode, discountAmount, originalAmount
            );

            cartService.clearCart(learnerId);
            notificationService.notifyPaymentSuccess(learnerId, finalAmount, invoice.getInvoiceNumber(), payment.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Paiement Flouci confirmé !");
            response.put("invoiceNumber", invoice.getInvoiceNumber());
            response.put("paymentStatus", "SUCCESS");
            if (couponCode != null) {
                response.put("couponApplied", couponCode);
                response.put("discountAmount", discountAmount);
            }
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        try {
            String ref = body.get("transactionRef");
            flouciService.resendOtp(ref);
            return ResponseEntity.ok(Map.of("message", "Nouveau code OTP envoyé"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/transactions/{learnerId}")
    public ResponseEntity<?> getHistory(@PathVariable String learnerId) {
        try {
            return ResponseEntity.ok(flouciService.getHistory(learnerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
