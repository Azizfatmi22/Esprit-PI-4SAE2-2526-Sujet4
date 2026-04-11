package org.example.msreportingcertification.services.interfaces;

import org.example.msreportingcertification.entities.CertificateTemplate;
import org.example.msreportingcertification.entities.EvaluationHistory;

public interface ICertificateService {

    CertificateTemplate saveOrUpdateTemplate(CertificateTemplate template);
    byte[] generateCertificatePdf(EvaluationHistory history, CertificateTemplate template) throws Exception;
}
