package com.example.mstrainerhiring.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResult {
    private boolean valid;
    private String message;
    private Double discountAmount;
    private Double finalAmount;
    private Double originalAmount;
    private String couponCode;
}
