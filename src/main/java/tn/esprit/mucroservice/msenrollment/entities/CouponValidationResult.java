package tn.esprit.mucroservice.msenrollment.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CouponValidationResult {
    private boolean valid;
    private String message;
    private Double originalAmount;
    private Double discountAmount;
    private Double finalAmount;
    private String couponCode;
}