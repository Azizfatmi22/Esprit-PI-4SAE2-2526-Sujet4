package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.Job;
import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.LegalForm;
import com.example.mstrainerhiring.enums.PartnerStatus;
import com.example.mstrainerhiring.enums.PartnershipType;
import com.example.mstrainerhiring.enums.Technology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PartnerHiringRepository partnerHiringRepository;

    private PartnerHiring savedPartner;

    @BeforeEach
    void setUp() {
        PartnerHiring partner = PartnerHiring.builder()
                .organizationName("Tech Solutions")
                .legalForm(LegalForm.SARL)
                .email("jobs@techsolutions.com")
                .phone("98123456")
                .website("https://techsolutions.com")
                .city(City.SOUSSE)
                .address("456 Job Avenue")
                .partnershipType(PartnershipType.TRAINING_PARTNER)
                .status(PartnerStatus.ACCEPTED)
                .build();
        savedPartner = partnerHiringRepository.save(partner);

        Job job1 = Job.builder()
                .title("Senior Backend Developer")
                .partner(savedPartner)
                .technology(Technology.JAVA)
                .minExperience(5)
                .location(City.TUNIS)
                .build();

        Job job2 = Job.builder()
                .title("Angular Developer")
                .partner(savedPartner)
                .technology(Technology.JAVASCRIPT)
                .minExperience(2)
                .location(City.TUNIS)
                .build();

        jobRepository.save(job1);
        jobRepository.save(job2);
    }

    @Test
    void shouldFindJobsByPartnerId() {
        List<Job> jobs = jobRepository.findByPartnerId(savedPartner.getId());
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getPartner().getId()).isEqualTo(savedPartner.getId());
    }

    @Test
    void shouldReturnEmptyListForNonExistentPartnerId() {
        List<Job> jobs = jobRepository.findByPartnerId(UUID.randomUUID());
        assertThat(jobs).isEmpty();
    }
}
