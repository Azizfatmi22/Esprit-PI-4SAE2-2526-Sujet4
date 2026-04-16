package com.example.mstrainerhiring.entities;

import com.example.mstrainerhiring.enums.CouponStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Coupon code is required")
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @NotNull(message = "Course ID is required")
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @NotBlank(message = "Course name is required")
    @Column(name = "course_name", nullable = false)
    private String courseName;

    @NotNull(message = "Expiration minutes is required")
    @Column(name = "expiration_minutes", nullable = false)
    private Integer expirationMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.example.mstrainerhiring.enums.CouponStatus status;

    @Column(name = "partner_id")
    private UUID partnerId;

    // --- PROMO CODE EXTENSIONS ---
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private com.example.mstrainerhiring.enums.DiscountType discountType;

    @Column(name = "discount_value")
    private Double discountValue;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount;

    @Column(name = "max_usages")
    private Integer maxUsages;

    @Column(name = "current_usages")
    private Integer currentUsages;

    @Column(name = "learner_id")
    private String learnerId;

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private com.example.mstrainerhiring.enums.CouponType type;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.expirationMinutes != null) {
            this.expiresAt = this.createdAt.plusMinutes(this.expirationMinutes);
        } else if (this.expiresAt == null) {
            // Default 1 year if not set via minutes
            this.expiresAt = this.createdAt.plusYears(1);
        }
        
        if (this.status == null) {
            this.status = com.example.mstrainerhiring.enums.CouponStatus.ACTIVE;
        }
        if (this.currentUsages == null) {
            this.currentUsages = 0;
        }
        if (this.type == null) {
            this.type = com.example.mstrainerhiring.enums.CouponType.LOYALTY_REWARD;
        }
        if (this.code != null) {
            this.code = this.code.toUpperCase();
        }
    }
}
