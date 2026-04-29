package tn.esprit.mucroservice.msenrollment.DTO;

import lombok.Data;

@Data
public class PayInstallmentRequest {
    private String paymentMethod;
    private String phoneNumber;
}