package tn.esprit.mucroservice.msenrollment.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.mucroservice.msenrollment.entities.Coupon;
import tn.esprit.mucroservice.msenrollment.entities.CouponType;
import tn.esprit.mucroservice.msenrollment.entities.CouponValidationResult;
import tn.esprit.mucroservice.msenrollment.entities.DiscountType;
import tn.esprit.mucroservice.msenrollment.repositories.CouponRepository;
import tn.esprit.mucroservice.msenrollment.services.interfaces.ICouponService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CouponServiceImpl implements ICouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Override
    public CouponValidationResult validateAndApply(String code, String learnerId, Double amount) {
        CouponValidationResult result = new CouponValidationResult();
        result.setOriginalAmount(amount);

        // 1. Trouver le coupon
        Optional<Coupon> optCoupon = couponRepository.findByCodeIgnoreCase(code);
        if (optCoupon.isEmpty()) {
            result.setValid(false);
            result.setMessage("Code promo invalide");
            return result;
        }

        Coupon coupon = optCoupon.get();

        // 2. Vérifier si actif
        if (!coupon.getIsActive()) {
            result.setValid(false);
            result.setMessage("Ce coupon n'est plus actif");
            return result;
        }

        // 3. Vérifier expiration
        if (coupon.getValidUntil() != null &&
                coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            result.setValid(false);
            result.setMessage("Ce coupon a expiré");
            return result;
        }

        // 4. Vérifier usage limité
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
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = amount * (coupon.getDiscountValue() / 100);
            // Appliquer le plafond si défini
            if (coupon.getMaxDiscountAmount() != null) {
                discount = Math.min(discount, coupon.getMaxDiscountAmount());
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        // 8. S'assurer que le discount ne dépasse pas le montant
        discount = Math.min(discount, amount);
        double finalAmount = Math.max(0, amount - discount);

        // 9. Incrémenter le compteur d'utilisation
        coupon.setCurrentUsages(coupon.getCurrentUsages() + 1);
        couponRepository.save(coupon);

        result.setValid(true);
        result.setMessage("Coupon appliqué avec succès !");
        result.setDiscountAmount(discount);
        result.setFinalAmount(finalAmount);
        result.setCouponCode(code.toUpperCase());
        return result;
    }

    @Override
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCurrentUsages(0);
        coupon.setIsActive(true);
        coupon.setCode(coupon.getCode().toUpperCase());
        return couponRepository.save(coupon);
    }

    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public Coupon updateCoupon(Long id, Coupon updated) {
        Coupon existing = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon non trouvé"));
        existing.setCode(updated.getCode().toUpperCase());
        existing.setDescription(updated.getDescription());
        existing.setDiscountType(updated.getDiscountType());
        existing.setDiscountValue(updated.getDiscountValue());
        existing.setMaxUsages(updated.getMaxUsages());
        existing.setValidUntil(updated.getValidUntil());
        existing.setIsActive(updated.getIsActive());
        existing.setMinOrderAmount(updated.getMinOrderAmount());
        existing.setMaxDiscountAmount(updated.getMaxDiscountAmount());
        return couponRepository.save(existing);
    }

    @Override
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    // ✅ Coupon de bienvenue automatique
    public void createWelcomeCoupon(String learnerId) {
        Coupon welcome = Coupon.builder()
                .code("WELCOME-" + learnerId)
                .description("Coupon de bienvenue -20%")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(20.0)
                .maxUsages(1)
                .currentUsages(0)
                .learnerId(learnerId)
                .validUntil(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .type(CouponType.WELCOME)
                .build();
        couponRepository.save(welcome);
    }
    // CouponServiceImpl.java — ajouter cette méthode
    // ✅ AJOUTER cette méthode — identique à validateAndApply MAIS sans incrémenter
    @Override
    public CouponValidationResult validateOnly(String code, String learnerId, Double amount) {
        CouponValidationResult result = new CouponValidationResult();
        result.setOriginalAmount(amount);

        Optional<Coupon> optCoupon = couponRepository.findByCodeIgnoreCase(code);
        if (optCoupon.isEmpty()) {
            result.setValid(false);
            result.setMessage("Code promo invalide");
            return result;
        }
        Coupon coupon = optCoupon.get();

        if (!coupon.getIsActive()) {
            result.setValid(false); result.setMessage("Ce coupon n'est plus actif"); return result;
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            result.setValid(false); result.setMessage("Ce coupon a expiré"); return result;
        }
        if (coupon.getMaxUsages() != null && coupon.getCurrentUsages() >= coupon.getMaxUsages()) {
            result.setValid(false); result.setMessage("Ce coupon a atteint sa limite d'utilisation"); return result;
        }
        if (coupon.getLearnerId() != null && !coupon.getLearnerId().equals(learnerId)) {
            result.setValid(false); result.setMessage("Ce coupon n'est pas valable pour votre compte"); return result;
        }
        if (coupon.getMinOrderAmount() != null && amount < coupon.getMinOrderAmount()) {
            result.setValid(false); result.setMessage("Montant minimum requis: " + coupon.getMinOrderAmount() + " TND"); return result;
        }

        double discount = 0.0;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = amount * (coupon.getDiscountValue() / 100);
            if (coupon.getMaxDiscountAmount() != null)
                discount = Math.min(discount, coupon.getMaxDiscountAmount());
        } else {
            discount = coupon.getDiscountValue();
        }
        discount = Math.min(discount, amount);
        double finalAmount = Math.max(0, amount - discount);

        // ✅ PAS de couponRepository.save() ici — compteur NON incrémenté

        result.setValid(true);
        result.setMessage("Coupon appliqué avec succès !");
        result.setDiscountAmount(discount);
        result.setFinalAmount(finalAmount);
        result.setCouponCode(code.toUpperCase());
        return result;
    }
}