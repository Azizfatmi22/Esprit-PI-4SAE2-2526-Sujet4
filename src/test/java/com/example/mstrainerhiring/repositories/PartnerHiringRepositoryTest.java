package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.LegalForm;
import com.example.mstrainerhiring.enums.PartnerStatus;
import com.example.mstrainerhiring.enums.PartnershipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PartnerHiringRepositoryTest {

    @Autowired
    private PartnerHiringRepository partnerHiringRepository;

    private PartnerHiring partner;

    @BeforeEach
    void setUp() {
        partner = PartnerHiring.builder()
                .organizationName("Tech Innovators")
                .legalForm(LegalForm.SARL)
                .email("contact@techinnovators.com")
                .phone("98765432")
                .website("https://techinnovators.com")
                .city(City.TUNIS)
                .address("123 Innovation Drive")
                .partnershipType(PartnershipType.TRAINING_PARTNER)
                .status(PartnerStatus.PENDING)
                .build();
        partnerHiringRepository.save(partner);
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        boolean exists = partnerHiringRepository.existsByEmail("contact@techinnovators.com");
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = partnerHiringRepository.existsByEmail("nonexistent@example.com");
        assertThat(exists).isFalse();
    }

    @Test
    void shouldReturnTrueWhenEmailExistsForDifferentId() {
        // Since we only have one record, let's pass a random UUID. It should return true because the email exists and the ID is different.
        boolean exists = partnerHiringRepository.existsByEmailAndIdNot("contact@techinnovators.com", UUID.randomUUID());
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailExistsButIdIsSame() {
        boolean exists = partnerHiringRepository.existsByEmailAndIdNot("contact@techinnovators.com", partner.getId());
        assertThat(exists).isFalse();
    }

    @Test
    void shouldReturnPartnersByStatus() {
        Page<PartnerHiring> result = partnerHiringRepository.findByStatus(PartnerStatus.PENDING, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("contact@techinnovators.com");
    }

    @Test
    void shouldNotReturnPartnersWhenStatusDoesNotMatch() {
        Page<PartnerHiring> result = partnerHiringRepository.findByStatus(PartnerStatus.ACCEPTED, PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }
}
