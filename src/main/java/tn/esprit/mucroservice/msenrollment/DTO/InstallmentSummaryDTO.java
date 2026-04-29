package tn.esprit.mucroservice.msenrollment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstallmentSummaryDTO {
    private Long planId;
    private Double totalAmount;
    private Double amountPaid;
    private Double amountRemaining;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer remainingInstallments;
    private String status;
    private String nextDueDate;
    private Double nextInstallmentAmount;
}