package tn.esprit.mucroservice.msenrollment.services.interfaces;

import tn.esprit.mucroservice.msenrollment.entities.Coupon;
import tn.esprit.mucroservice.msenrollment.entities.CouponValidationResult;

import java.util.List;

public interface ICouponService {
    Coupon createCoupon(Coupon coupon);
    CouponValidationResult validateAndApply(String code, String learnerId, Double amount);
    CouponValidationResult validateOnly(String code, String learnerId, Double amount);
    List<Coupon> getAllCoupons();
    Coupon updateCoupon(Long id, Coupon coupon);
    void deleteCoupon(Long id);
    // ICouponService.java — ajouter

}