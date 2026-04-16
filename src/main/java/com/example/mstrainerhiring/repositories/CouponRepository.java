package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.Coupon;
import com.example.mstrainerhiring.enums.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCode(String code);

    List<Coupon> findAllByStatusAndExpiresAtAfter(CouponStatus status, LocalDateTime now);

    List<Coupon> findAllByStatusAndExpiresAtBefore(CouponStatus status, LocalDateTime now);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Coupon c WHERE c.status = :status AND c.expiresAt > :now AND c.id NOT IN (SELECT cl.coupon.id FROM CouponClaim cl WHERE cl.userId = :userId)")
    List<Coupon> findAvailableCouponsForUser(
            @org.springframework.data.repository.query.Param("status") CouponStatus status,
            @org.springframework.data.repository.query.Param("now") LocalDateTime now,
            @org.springframework.data.repository.query.Param("userId") String userId);
}
