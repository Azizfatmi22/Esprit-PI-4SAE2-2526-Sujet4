package tn.esprit.mucroservice.msenrollment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mucroservice.msenrollment.entities.RefundRequest;
import tn.esprit.mucroservice.msenrollment.services.interfaces.IRefundService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msenrollment/refunds")
public class RefundController {

    @Autowired
    private IRefundService refundService;

    // Apprenant — faire une demande
    @PostMapping("/request")
    public ResponseEntity<?> requestRefund(@RequestBody Map<String, Object> body) {
        try {
            String learnerId = String.valueOf(body.get("learnerId").toString());
            Long invoiceId = Long.valueOf(body.get("invoiceId").toString());
            String reason = body.get("reason").toString();
            return ResponseEntity.ok(refundService.createRefundRequest(learnerId, invoiceId, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin — liste des demandes en attente
    @GetMapping("/pending")
    public ResponseEntity<List<RefundRequest>> getPending() {
        return ResponseEntity.ok(refundService.getPendingRefunds());
    }

    // Admin — approuver
    @PostMapping("/approve/{refundId}")
    public ResponseEntity<?> approve(
            @PathVariable Long refundId,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(refundService.approveRefund(refundId, body.get("adminName")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin — rejeter
    @PostMapping("/reject/{refundId}")
    public ResponseEntity<?> reject(
            @PathVariable Long refundId,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(refundService.rejectRefund(
                    refundId, body.get("adminName"), body.get("reason")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Apprenant — ses demandes
    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<List<RefundRequest>> getByLearner(@PathVariable String learnerId) {
        return ResponseEntity.ok(refundService.getRefundsByLearner(learnerId));
    }
    @GetMapping("/all")
    public ResponseEntity<List<RefundRequest>> getAllRefunds() {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }

}