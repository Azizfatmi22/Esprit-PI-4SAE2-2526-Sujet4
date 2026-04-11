package org.example.msreportingcertification.repositories;

import org.example.msreportingcertification.entities.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {

    Optional<CertificateTemplate> findByEvaluationId(Long evaluationId);
    @Query("SELECT t FROM CertificateTemplate t WHERE t.isTemplateDefault = true")
    Optional<CertificateTemplate> findDefaultTemplate();

}
