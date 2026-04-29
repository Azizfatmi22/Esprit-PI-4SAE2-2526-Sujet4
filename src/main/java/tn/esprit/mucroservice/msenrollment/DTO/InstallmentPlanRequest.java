package tn.esprit.mucroservice.msenrollment.DTO;

import lombok.Data;

import java.util.List;

@Data
public class
InstallmentPlanRequest {
    private String learnerId;
    private Double totalAmount;
    private Integer numberOfInstallments; // 3 ou 6
    private String paymentMethod;         // FLOUCI, WAFA_CASH, BAKCHICH
    private String phoneNumber;
    private List<String> courseTitles;

}
