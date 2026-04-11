package org.example.msreportingcertification.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerProgression {
    @Id
    private String learnerId;

    private String learnerName;
    private Long totalXp = 0L;
    private Integer currentLevel = 1;
    private Integer totalCertificates = 0;

    private LocalDateTime lastActivity = LocalDateTime.now();

}
