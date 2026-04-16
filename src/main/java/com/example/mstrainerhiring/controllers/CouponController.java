package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.CouponClaimDTO;
import com.example.mstrainerhiring.dto.CouponDTO;
import com.example.mstrainerhiring.services.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon Management API")
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @Operation(summary = "Create a new coupon (Partner)")
    public ResponseEntity<CouponDTO> createCoupon(@RequestBody CouponDTO couponDTO) {
        return new ResponseEntity<>(couponService.createCoupon(couponDTO), HttpStatus.CREATED);
    }

    @GetMapping("/available")
    @Operation(summary = "Get all active coupons (Student)")
    public ResponseEntity<List<CouponDTO>> getAvailable(@RequestParam(required = false) String userId) {
        return ResponseEntity.ok(couponService.getAvailableCoupons(userId));
    }

    @PostMapping("/{code}/claim")
    @Operation(summary = "Claim a coupon (Student)")
    public ResponseEntity<CouponClaimDTO> claim(@PathVariable String code, @RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        return ResponseEntity.ok(couponService.claimCoupon(code, userId));
    }

    @GetMapping("/my/{userId}")
    @Operation(summary = "Get student's claimed coupons")
    public ResponseEntity<List<CouponClaimDTO>> getMyClaims(@PathVariable String userId) {
        return ResponseEntity.ok(couponService.getMyClaims(userId));
    }

    @PostMapping("/{code}/redeem")
    @Operation(summary = "Redeem a coupon at checkout")
    public ResponseEntity<CouponClaimDTO> redeem(
            @PathVariable String code,
            @RequestBody Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        return ResponseEntity.ok(couponService.redeemCoupon(code, userId, courseId));
    }

    @PostMapping("/admin/trigger-loyalty-rewards")
    @Operation(summary = "Admin trigger for Platinum Loyalty Coupons auto-dispensing")
    public ResponseEntity<Void> triggerLoyaltyRewards() {
        couponService.triggerLoyaltyRewards();
        return ResponseEntity.ok().build();
    }

    // --- CONSOLIDATED PROMO LOGIC ---

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon (called from Frontend during checkout)")
    public ResponseEntity<com.example.mstrainerhiring.dto.CouponValidationResult> validateCoupon(
            @RequestBody Map<String, Object> body) {
        String code = body.get("code").toString();
        String learnerId = body.get("learnerId") != null ? body.get("learnerId").toString() : null;
        Double amount = Double.valueOf(body.get("amount").toString());
        return ResponseEntity.ok(couponService.validateAndApply(code, learnerId, amount));
    }

    @GetMapping("/all")
    @Operation(summary = "Admin: Get all coupons (Rewards and Promo Codes)")
    public ResponseEntity<List<CouponDTO>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin: Update a coupon")
    public ResponseEntity<CouponDTO> updateCoupon(@PathVariable java.util.UUID id, @RequestBody CouponDTO couponDTO) {
        return ResponseEntity.ok(couponService.updateCoupon(id, couponDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Admin: Delete a coupon")
    public ResponseEntity<Void> deleteCoupon(@PathVariable java.util.UUID id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/welcome/{learnerId}")
    @Operation(summary = "Auto: Create welcome coupon for new learner")
    public ResponseEntity<Void> createWelcome(@PathVariable String learnerId) {
        couponService.createWelcomeCoupon(learnerId);
        return ResponseEntity.ok().build();
    }
}
