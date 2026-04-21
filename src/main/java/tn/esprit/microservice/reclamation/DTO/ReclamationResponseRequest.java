package tn.esprit.microservice.reclamation.DTO;


import lombok.Data;

@Data
public class ReclamationResponseRequest {
    private Long adminId;
    private String responseText;
    private Boolean isInternal;
    private String senderType;
    private String quotedText;
    private String quotedAuthor;
}
