package com.example.mstrainerhiring.services;

import com.example.mstrainerhiring.dto.CouponClaimDTO;
import com.example.mstrainerhiring.dto.CouponDTO;

import java.util.List;

public interface CouponService {
    CouponDTO createCoupon(CouponDTO couponDTO);

    List<CouponDTO> getAvailableCoupons(String userId);

    CouponClaimDTO claimCoupon(String code, String userId);

    List<CouponClaimDTO> getMyClaims(String userId);

    CouponClaimDTO redeemCoupon(String code, String userId, Long courseId);

    void expireStaleCoupons();

    void triggerLoyaltyRewards();

    // --- CONSOLIDATED PROMO LOGIC ---
    com.example.mstrainerhiring.dto.CouponValidationResult validateAndApply(String code, String learnerId, Double amount);
    
    List<CouponDTO> getAllCoupons();
    
    CouponDTO updateCoupon(java.util.UUID id, CouponDTO couponDTO);
    
    void deleteCoupon(java.util.UUID id);
    
    void createWelcomeCoupon(String learnerId);
}
