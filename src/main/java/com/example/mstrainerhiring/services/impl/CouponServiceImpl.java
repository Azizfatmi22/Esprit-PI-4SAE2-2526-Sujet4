package com.example.mstrainerhiring.services.impl;

import com.example.mstrainerhiring.dto.CouponClaimDTO;
import com.example.mstrainerhiring.dto.CouponDTO;
import com.example.mstrainerhiring.entities.Coupon;
import com.example.mstrainerhiring.entities.CouponClaim;
import com.example.mstrainerhiring.enums.CouponStatus;
import com.example.mstrainerhiring.exception.ResourceNotFoundException;
import com.example.mstrainerhiring.mapper.CouponMapper;
import com.example.mstrainerhiring.repositories.CouponClaimRepository;
import com.example.mstrainerhiring.repositories.CouponRepository;
import com.example.mstrainerhiring.services.CouponService;
import com.example.mstrainerhiring.dto.CouponValidationResult;
import com.example.mstrainerhiring.enums.CouponType;
import com.example.mstrainerhiring.enums.DiscountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponClaimRepository claimRepository;
    private final CouponMapper couponMapper;
    private final com.example.mstrainerhiring.services.PartnerHiringService partnerService;
    private final com.example.mstrainerhiring.repositories.PartnerDocumentRepository documentRepository;
    private final com.example.mstrainerhiring.client.EnrollmentClient enrollmentClient;
    private final org.springframework.web.client.RestTemplate restTemplate;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private final java.security.SecureRandom random = new java.security.SecureRandom();

    @org.springframework.beans.factory.annotation.Value("${services.course.url:http://localhost:8083}")
    private String courseUrl;

    @Override
    @Transactional
    public CouponDTO createCoupon(CouponDTO couponDTO) {
        Coupon coupon = couponMapper.toCouponEntity(couponDTO);
        coupon.setCode(generateUniqueCode());
        coupon.setExpirationMinutes(couponDTO.getExpirationMinutes());
        Coupon saved = couponRepository.save(coupon);
        return couponMapper.toCouponDTO(saved);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (couponRepository.findByCode(code).isPresent());
        return code;
    }

    @Override
    public List<CouponDTO> getAvailableCoupons(String userId) {
        // 1. Fetch available unclaimed coupons natively from DB
        List<Coupon> available = couponRepository.findAvailableCouponsForUser(CouponStatus.ACTIVE,
                LocalDateTime.now(), userId);

        // 2. Pre-fetch partner details to avoid N+1 API and Disk load
        java.util.Set<java.util.UUID> partnerIds = available.stream().map(Coupon::getPartnerId).collect(Collectors.toSet());
        java.util.Map<java.util.UUID, String> partnerNames = new java.util.HashMap<>();
        java.util.Map<java.util.UUID, byte[]> partnerLogos = new java.util.HashMap<>();

        for (java.util.UUID pId : partnerIds) {
            try {
                partnerNames.put(pId, partnerService.getPartnerById(pId).getOrganizationName());
            } catch (Exception e) {
                log.warn("Partner name not found for {}: {}", pId, e.getMessage());
            }

            documentRepository.findByPartnerIdAndDocumentType(pId,
                    com.example.mstrainerhiring.enums.DocumentType.LOGO)
                    .ifPresent(doc -> {
                        try {
                            partnerLogos.put(pId,
                                    java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(doc.getFilePath())));
                        } catch (java.io.IOException e) {
                            log.warn("Failed to read partner logo for {}: {}", pId, e.getMessage());
                        }
                    });
        }

        // 3. Map efficiently
        return available.stream()
                .map(coupon -> {
                    CouponDTO dto = couponMapper.toCouponDTO(coupon);
                    dto.setPartnerName(partnerNames.get(coupon.getPartnerId()));
                    dto.setPartnerLogo(partnerLogos.get(coupon.getPartnerId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponClaimDTO claimCoupon(String code, String userId) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));

        if (coupon.getStatus() == CouponStatus.EXPIRED || LocalDateTime.now().isAfter(coupon.getExpiresAt())) {
            throw new IllegalStateException("This coupon has expired");
        }

        // Idempotent: check if already claimed
        return claimRepository.findByUserIdAndCoupon_Code(userId, code.toUpperCase())
                .map(couponMapper::toCouponClaimDTO)
                .orElseGet(() -> {
                    CouponClaim claim = CouponClaim.builder()
                            .coupon(coupon)
                            .userId(userId)
                            .build();
                    CouponClaim saved = claimRepository.save(claim);

                    // Trigger Auto-Enrollment Asynchronously to avoid blocking response
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        triggerAutoEnrollment(userId, coupon);
                    });

                    return couponMapper.toCouponClaimDTO(saved);
                });
    }

    private void triggerAutoEnrollment(String userId, Coupon coupon) {
        try {
            // Fetch Course details (title and price) from MS-Course
            String url = courseUrl + "/api/courses/" + coupon.getCourseId();
            var courseResponse = restTemplate.getForEntity(url, java.util.Map.class);

            if (courseResponse.getStatusCode().is2xxSuccessful() && courseResponse.getBody() != null) {
                java.util.Map<String, Object> course = courseResponse.getBody();
                String title = (String) course.get("title");
                Double price = Double.valueOf(course.get("price").toString());

                log.info("Auto-enrolling learner {} in course {} ({})", userId, coupon.getCourseId(), title);
                enrollmentClient.enrollLearner(userId, coupon.getCourseId(), title, price);
            } else {
                log.warn("Could not fetch course details for enrollment. Falling back to default.");
                enrollmentClient.enrollLearner(userId, coupon.getCourseId(), coupon.getCourseName(), 0.0);
            }
        } catch (Exception e) {
            log.error("Error during auto-enrollment trigger: {}", e.getMessage());
            // Fallback
            enrollmentClient.enrollLearner(userId, coupon.getCourseId(), coupon.getCourseName(), 0.0);
        }
    }

    @Override
    public List<CouponClaimDTO> getMyClaims(String userId) {
        return claimRepository.findByUserId(userId)
                .stream()
                .map(couponMapper::toCouponClaimDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponClaimDTO redeemCoupon(String code, String userId, Long courseId) {
        CouponClaim claim = claimRepository.findByUserIdAndCoupon_Code(userId, code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("CouponClaim", "code", code));

        if (claim.isUsed()) {
            throw new IllegalStateException("This coupon has already been used");
        }

        Coupon coupon = claim.getCoupon();
        if (coupon.getStatus() == CouponStatus.EXPIRED || LocalDateTime.now().isAfter(coupon.getExpiresAt())) {
            throw new IllegalStateException("This coupon has expired");
        }

        if (!coupon.getCourseId().equals(courseId)) {
            throw new IllegalStateException("This coupon is not valid for this course");
        }

        claim.setUsed(true);
        return couponMapper.toCouponClaimDTO(claimRepository.save(claim));
    }

    @Override
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void expireStaleCoupons() {
        log.info("Checking for expired coupons...");
        List<Coupon> expired = couponRepository.findAllByStatusAndExpiresAtBefore(CouponStatus.ACTIVE,
                LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(c -> c.setStatus(CouponStatus.EXPIRED));
            couponRepository.saveAll(expired);
            log.info("Expired {} coupons", expired.size());
        }
    }

    @Override
    @Transactional
    public void triggerLoyaltyRewards() {
        log.info("Starting Auto-Dispenser for Platinum Loyalty Rewards");
        
        // Fetch all partners
        org.springframework.data.domain.Page<com.example.mstrainerhiring.dto.PartnerHiringDTO> partnersPage = 
            partnerService.getAllPartners(org.springframework.data.domain.PageRequest.of(0, 1000), com.example.mstrainerhiring.enums.PartnerStatus.ACCEPTED);

        List<com.example.mstrainerhiring.dto.PartnerHiringDTO> platinumPartners = partnersPage.getContent().stream()
            .filter(p -> com.example.mstrainerhiring.enums.PartnerTier.PLATINUM.equals(p.getTier()))
            .collect(Collectors.toList());

        for (com.example.mstrainerhiring.dto.PartnerHiringDTO partner : platinumPartners) {
            Coupon coupon = Coupon.builder()
                .code(generateUniqueCode())
                .courseId(1L) // Masterpiece demo course ID
                .courseName("Platinum Partner Premium Bundle")
                .partnerId(partner.getId())
                .expirationMinutes(10080) // 1 week
                .status(CouponStatus.ACTIVE)
                .build();
            
            couponRepository.save(coupon);
            log.info("Generated Loyalty Coupon {} for Platinum Partner {}", coupon.getCode(), partner.getOrganizationName());
        }
    }

    // --- CONSOLIDATED PROMO LOGIC ---

    @Override
    @Transactional
    public CouponValidationResult validateAndApply(String code, String learnerId, Double amount) {
        CouponValidationResult result = CouponValidationResult.builder()
                .originalAmount(amount)
                .build();

        // 1. Trouver le coupon
        Optional<Coupon> optCoupon = couponRepository.findByCode(code.toUpperCase());
        if (optCoupon.isEmpty()) {
            result.setValid(false);
            result.setMessage("Code promo invalide");
            return result;
        }

        Coupon coupon = optCoupon.get();

        // 2. Vérifier si actif
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            result.setValid(false);
            result.setMessage("Ce coupon n'est plus actif");
            return result;
        }

        // 3. Vérifier expiration
        if (coupon.getExpiresAt() != null &&
                coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            result.setValid(false);
            result.setMessage("Ce coupon a expiré");
            return result;
        }

        // 4. Vérifier usage limité (pour les promo codes généraux)
        if (coupon.getMaxUsages() != null &&
                coupon.getCurrentUsages() >= coupon.getMaxUsages()) {
            result.setValid(false);
            result.setMessage("Ce coupon a atteint sa limite d'utilisation");
            return result;
        }

        // 5. Vérifier coupon personnel
        if (coupon.getLearnerId() != null &&
                !coupon.getLearnerId().equals(learnerId)) {
            result.setValid(false);
            result.setMessage("Ce coupon n'est pas valable pour votre compte");
            return result;
        }

        // 6. Vérifier montant minimum
        if (coupon.getMinOrderAmount() != null &&
                amount < coupon.getMinOrderAmount()) {
            result.setValid(false);
            result.setMessage("Montant minimum requis: " + coupon.getMinOrderAmount() + " TND");
            return result;
        }

        // 7. Calculer la réduction
        double discount = 0.0;
        if (coupon.getType() == CouponType.LOYALTY_REWARD) {
            // Pour les Rewards de Partner, on considère que c'est une réduction de 100% sur le cours spécifique
            // Mais ici on est dans un flux de validation de panier.
            // Habituellement ces coupons sont "CLAIMED" d'abord.
            // Si l'utilisateur tape le code d'un Reward directement dans le panier:
            discount = amount; // Gratuit
        } else {
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                discount = amount * (coupon.getDiscountValue() / 100);
                if (coupon.getMaxDiscountAmount() != null) {
                    discount = Math.min(discount, coupon.getMaxDiscountAmount());
                }
            } else if (coupon.getDiscountType() == DiscountType.FIXED) {
                discount = coupon.getDiscountValue();
            }
        }

        discount = Math.min(discount, amount);
        double finalAmount = Math.max(0, amount - discount);

        // 8. Incrémenter l'usage
        coupon.setCurrentUsages(coupon.getCurrentUsages() + 1);
        if (coupon.getMaxUsages() != null && coupon.getCurrentUsages() >= coupon.getMaxUsages()) {
            coupon.setStatus(CouponStatus.EXPIRED);
        }
        couponRepository.save(coupon);

        result.setValid(true);
        result.setDiscountAmount(discount);
        result.setFinalAmount(finalAmount);
        result.setCouponCode(code.toUpperCase());
        result.setMessage("Coupon appliqué !");
        
        return result;
    }

    @Override
    public List<CouponDTO> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(couponMapper::toCouponDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponDTO updateCoupon(UUID id, CouponDTO dto) {
        Coupon existing = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id.toString()));
        
        existing.setDiscountType(dto.getDiscountType());
        existing.setDiscountValue(dto.getDiscountValue());
        existing.setMaxUsages(dto.getMaxUsages());
        existing.setMinOrderAmount(dto.getMinOrderAmount());
        existing.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        existing.setExpiresAt(dto.getExpiresAt());
        existing.setStatus(dto.getStatus());
        
        return couponMapper.toCouponDTO(couponRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID id) {
        couponRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void createWelcomeCoupon(String learnerId) {
        Coupon welcome = Coupon.builder()
                .code("WELCOME-" + learnerId.substring(0, 8).toUpperCase())
                .courseId(0L) // General
                .courseName("Welcome Discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(20.0)
                .maxUsages(1)
                .currentUsages(0)
                .learnerId(learnerId)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .type(CouponType.WELCOME)
                .build();
        couponRepository.save(welcome);
    }
}
