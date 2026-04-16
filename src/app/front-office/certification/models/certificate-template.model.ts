export interface CertificateTemplate {
  id?: number;
  evaluationId: number;
  htmlContent: string;
  platformLogoBase64: string;
  trainerSignatureBase64: string;
  isTemplateDefault: boolean;
}