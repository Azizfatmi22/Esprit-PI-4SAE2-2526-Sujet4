package com.example.mstrainerhiring.dto;

import com.example.mstrainerhiring.enums.CouponStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {
    private UUID id;
    private String code;
    private Long courseId;
    private String courseName;
    private Integer expirationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private CouponStatus status;
    private UUID partnerId;

    public long getMinutesRemaining() {
        if (expiresAt == null || LocalDateTime.now().isAfter(expiresAt)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), expiresAt);
    }
}
