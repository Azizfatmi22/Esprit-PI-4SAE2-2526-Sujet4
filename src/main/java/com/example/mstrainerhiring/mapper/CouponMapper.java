package com.example.mstrainerhiring.mapper;

import com.example.mstrainerhiring.dto.CouponClaimDTO;
import com.example.mstrainerhiring.dto.CouponDTO;
import com.example.mstrainerhiring.entities.Coupon;
import com.example.mstrainerhiring.entities.CouponClaim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponDTO toCouponDTO(Coupon coupon);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Coupon toCouponEntity(CouponDTO couponDTO);

    @Mapping(target = "couponCode", source = "coupon.code")
    @Mapping(target = "courseId", source = "coupon.courseId")
    @Mapping(target = "courseName", source = "coupon.courseName")
    @Mapping(target = "minutesRemaining", expression = "java(calculateMinutesRemaining(claim.getCoupon()))")
    CouponClaimDTO toCouponClaimDTO(CouponClaim claim);

    default long calculateMinutesRemaining(Coupon coupon) {
        if (coupon == null || coupon.getExpiresAt() == null
                || java.time.LocalDateTime.now().isAfter(coupon.getExpiresAt())) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.MINUTES.between(java.time.LocalDateTime.now(), coupon.getExpiresAt());
    }
}
