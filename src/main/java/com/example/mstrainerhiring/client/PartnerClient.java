package com.example.mstrainerhiring.client;

import com.example.mstrainerhiring.dto.PartnerHiringDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerClient {

    private final RestTemplate restTemplate;

    @Value("${services.partner-hiring.url:http://MS-PARTNERHIRING}")
    private String partnerHiringUrl;

    public boolean existsById(UUID id) {
        try {
            restTemplate.getForObject(partnerHiringUrl + "/api/partners/" + id, Object.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to verify partner existence: {}", e.getMessage());
            return false;
        }
    }

    public PartnerHiringDTO getPartnerById(UUID id) {
        try {
            return restTemplate.getForObject(partnerHiringUrl + "/api/partners/" + id, PartnerHiringDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch partner details: {}", e.getMessage());
            return null;
        }
    }

    public boolean jobExistsById(UUID jobId) {
        try {
            restTemplate.getForObject(partnerHiringUrl + "/api/jobs/" + jobId, Object.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to verify job existence: {}", e.getMessage());
            return false;
        }
    }

    public com.example.mstrainerhiring.dto.JobDTO getJobById(UUID id) {
        try {
            return restTemplate.getForObject(partnerHiringUrl + "/api/jobs/" + id,
                    com.example.mstrainerhiring.dto.JobDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch job details: {}", e.getMessage());
            return null;
        }
    }
}
