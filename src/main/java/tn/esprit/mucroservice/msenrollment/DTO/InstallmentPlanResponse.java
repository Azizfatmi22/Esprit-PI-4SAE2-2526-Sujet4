package tn.esprit.mucroservice.msenrollment.DTO;

import lombok.Data;
import tn.esprit.mucroservice.msenrollment.entities.InstallmentPlanStatus;

import java.util.Date;
import java.util.List;

@Data
public class InstallmentPlanResponse {
    private Long planId;
    private String learnerId;       // ✅ AJOUTEZ
    private Date createdAt;
    private Double totalAmount;
    private Double amountWithFees;
    private Double feePercentage;
    private Integer numberOfInstallments;
    private Double installmentAmount;
    private InstallmentPlanStatus status;
    private List<InstallmentDTO> installments;
    private String message;

}