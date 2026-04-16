package com.example.mstrainerhiring.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponClaimDTO {
    private UUID id;
    private String couponCode;
    private Long courseId;
    private String courseName;
    private String userId;
    private LocalDateTime claimedAt;
    private boolean used;
    private long minutesRemaining;
}
