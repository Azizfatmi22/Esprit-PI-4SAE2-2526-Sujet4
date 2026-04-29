package tn.esprit.mucroservice.msenrollment.DTO;
import lombok.Data;
import tn.esprit.mucroservice.msenrollment.entities.InstallmentStatus;

import java.util.Date;

@Data
public class InstallmentDTO {
    private Long id;
    private Integer installmentNumber;
    private Double amount;
    private Date dueDate;
    private Date paidDate;
    private InstallmentStatus status;
    private String invoiceNumber;
}