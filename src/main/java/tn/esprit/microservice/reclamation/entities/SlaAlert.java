package tn.esprit.microservice.reclamation.entities;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reclamation_id", nullable = false)
    private Long reclamationId;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "sla_minutes")
    private Integer slaMinutes;

    @Column(name = "elapsed_minutes")
    private Long elapsedMinutes;

    @Column(name = "alert_date", nullable = false)
    private LocalDateTime alertDate;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "resolved")
    private Boolean resolved = false;
}