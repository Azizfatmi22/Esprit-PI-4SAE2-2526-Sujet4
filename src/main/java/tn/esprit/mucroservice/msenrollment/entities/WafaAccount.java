package tn.esprit.mucroservice.msenrollment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "wafa_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WafaAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "balance", nullable = false)
    private Double balance;

    @Column(name = "learner_id")
    private String learnerId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}