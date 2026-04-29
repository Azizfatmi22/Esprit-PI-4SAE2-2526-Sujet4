package tn.esprit.mucroservice.msenrollment.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.mucroservice.msenrollment.entities.*;
import tn.esprit.mucroservice.msenrollment.repositories.CouponRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    // =========================
    // 1. VALID COUPON (PERCENTAGE)
    // =========================
    @Test
    void shouldApplyPercentageCouponSuccessfully() {

        Coupon coupon = Coupon.builder()
                .code("PROMO10")
                .isActive(true)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10.0)
                .currentUsages(0)   // ✅ FIX NULL
                .maxUsages(10)
                .build();

        when(couponRepository.findByCodeIgnoreCase("PROMO10"))
                .thenReturn(Optional.of(coupon));

        CouponValidationResult result =
                couponService.validateAndApply("PROMO10", "L1", 100.0);

        assertTrue(result.isValid());
        assertEquals(10.0, result.getDiscountAmount());
        assertEquals(90.0, result.getFinalAmount());

        verify(couponRepository).save(any(Coupon.class));
    }

    // =========================
    // 2. INVALID CODE
    // =========================
    @Test
    void shouldRejectInvalidCoupon() {

        when(couponRepository.findByCodeIgnoreCase("BAD"))
                .thenReturn(Optional.empty());

        CouponValidationResult result =
                couponService.validateAndApply("BAD", "L1", 100.0);

        assertFalse(result.isValid());
        assertEquals("Code promo invalide", result.getMessage());

        verify(couponRepository, never()).save(any());
    }

    // =========================
    // 3. EXPIRED COUPON
    // =========================
    @Test
    void shouldRejectExpiredCoupon() {

        Coupon coupon = Coupon.builder()
                .code("OLD")
                .isActive(true)
                .validUntil(LocalDateTime.now().minusDays(1))
                .discountType(DiscountType.FIXED)
                .discountValue(20.0)
                .currentUsages(0)   // ✅ FIX
                .build();

        when(couponRepository.findByCodeIgnoreCase("OLD"))
                .thenReturn(Optional.of(coupon));

        CouponValidationResult result =
                couponService.validateAndApply("OLD", "L1", 100.0);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("expiré"));

        verify(couponRepository, never()).save(any());
    }

    // =========================
    // 4. MIN ORDER AMOUNT
    // =========================
    @Test
    void shouldRejectBelowMinimumAmount() {

        Coupon coupon = Coupon.builder()
                .code("MIN50")
                .isActive(true)
                .minOrderAmount(50.0)
                .discountType(DiscountType.FIXED)
                .discountValue(10.0)
                .currentUsages(0)   // ✅ FIX
                .build();

        when(couponRepository.findByCodeIgnoreCase("MIN50"))
                .thenReturn(Optional.of(coupon));

        CouponValidationResult result =
                couponService.validateAndApply("MIN50", "L1", 20.0);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Montant minimum"));

        verify(couponRepository, never()).save(any());
    }

    // =========================
    // 5. FIXED DISCOUNT (BUG FIXED HERE)
    // =========================
    @Test
    void shouldApplyFixedDiscount() {

        Coupon coupon = Coupon.builder()
                .code("FIXED")
                .isActive(true)
                .discountType(DiscountType.FIXED)
                .discountValue(30.0)
                .currentUsages(0)   // ✅ FIX IMPORTANT
                .build();

        when(couponRepository.findByCodeIgnoreCase("FIXED"))
                .thenReturn(Optional.of(coupon));

        CouponValidationResult result =
                couponService.validateAndApply("FIXED", "L1", 100.0);

        assertTrue(result.isValid());
        assertEquals(30.0, result.getDiscountAmount());
        assertEquals(70.0, result.getFinalAmount());

        verify(couponRepository).save(any(Coupon.class));
    }

    // =========================
    // 6. VALIDATE ONLY (NO SAVE)
    // =========================
    @Test
    void shouldValidateOnlyWithoutSaving() {

        Coupon coupon = Coupon.builder()
                .code("TEST")
                .isActive(true)
                .discountType(DiscountType.FIXED)
                .discountValue(10.0)
                .currentUsages(0)   // ✅ FIX
                .build();

        when(couponRepository.findByCodeIgnoreCase("TEST"))
                .thenReturn(Optional.of(coupon));

        CouponValidationResult result =
                couponService.validateOnly("TEST", "L1", 100.0);

        assertTrue(result.isValid());
        assertEquals(90.0, result.getFinalAmount());

        verify(couponRepository, never()).save(any());
    }

    // =========================
    // 7. CREATE COUPON
    // =========================
    @Test
    void shouldCreateCoupon() {

        Coupon input = Coupon.builder()
                .code("new")
                .build();

        when(couponRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        Coupon result = couponService.createCoupon(input);

        assertEquals("NEW", result.getCode());
        assertTrue(result.getIsActive());
        assertEquals(0, result.getCurrentUsages());
    }
}