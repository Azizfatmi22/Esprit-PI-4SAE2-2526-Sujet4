package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.CouponClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponClaimRepository extends JpaRepository<CouponClaim, UUID> {
    List<CouponClaim> findByUserId(String userId);

    Optional<CouponClaim> findByUserIdAndCoupon_Code(String userId, String code);

    boolean existsByUserIdAndCoupon_Id(String userId, UUID couponId);
}
