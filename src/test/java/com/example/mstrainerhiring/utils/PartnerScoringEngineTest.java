package com.example.mstrainerhiring.utils;

import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.enums.PartnerTier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerScoringEngineTest {

    @Test
    void shouldFlagDisposableEmailAsSuspicious() {
        PartnerHiring partner = new PartnerHiring();
        partner.setEmail("test@yopmail.com");
        
        boolean suspicious = PartnerScoringEngine.isSuspicious(partner);
        
        assertThat(suspicious).isTrue();
    }

    @Test
    void shouldNotFlagValidEmailAsSuspicious() {
        PartnerHiring partner = new PartnerHiring();
        partner.setEmail("john.doe@gmail.com");
        
        boolean suspicious = PartnerScoringEngine.isSuspicious(partner);
        
        assertThat(suspicious).isFalse();
    }

    @Test
    void shouldFlagKeyboardSmashOrganizationName() {
        PartnerHiring partner = new PartnerHiring();
        partner.setOrganizationName("asdfghjkl"); // many consecutive consonants
        
        boolean suspicious = PartnerScoringEngine.isSuspicious(partner);
        
        assertThat(suspicious).isTrue();
    }

    @Test
    void shouldDeterminePlatinumTierForHighScores() {
        assertThat(PartnerScoringEngine.determineTier(85)).isEqualTo(PartnerTier.PLATINUM);
        assertThat(PartnerScoringEngine.determineTier(100)).isEqualTo(PartnerTier.PLATINUM);
    }
    
    @Test
    void shouldDetermineGoldTierForMidHighScores() {
        assertThat(PartnerScoringEngine.determineTier(60)).isEqualTo(PartnerTier.GOLD);
        assertThat(PartnerScoringEngine.determineTier(79)).isEqualTo(PartnerTier.GOLD);
    }

    @Test
    void shouldDetermineBronzeTierForLowScores() {
        assertThat(PartnerScoringEngine.determineTier(20)).isEqualTo(PartnerTier.BRONZE);
        assertThat(PartnerScoringEngine.determineTier(0)).isEqualTo(PartnerTier.BRONZE);
    }
}
