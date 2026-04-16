// template-editor.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ReportingService } from '../../services/reporting.service';
import { KeycloakService } from '../../services/keycloak.service';

@Component({
  selector: 'app-template-editor',
  templateUrl: './template-editor.component.html',
  styleUrl: './template-editor.component.scss',
})
export class TemplateEditorComponent implements OnInit {
  evaluationId!: number;
  isLoading: boolean = true;

  platformLogoUrl: string =
    'https://formini.dz/wp-content/uploads/2016/12/formini-final-logo.png';

  // Toutes les données sont maintenant centralisées ici
  template: any = {
    id: null,
    evaluationId: null,
    htmlContent: '',
    trainerName: '',
    trainerSignatureBase64: '',
    isTemplateDefault: false,
    borderColor: '#2563eb', // Valeur par défaut
    certificateTitle: 'Certificate of Completion', // Valeur par défaut
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private certificateService: ReportingService,
    private keycloakService: KeycloakService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.evaluationId = +id;
      this.template.evaluationId = this.evaluationId;
      this.loadExistingTemplate();
    }
  }

  loadExistingTemplate() {
    this.certificateService
      .getTemplateByEvaluationId(this.evaluationId)
      .subscribe({
        next: (existingTemplate) => {
          if (existingTemplate) {
            this.template = existingTemplate;
            this.template.trainerName = this.keycloakService.getUsername();

            // --- HACK : Extraire les valeurs depuis le HTML sauvegardé ---
            this.extractDataFromHtml(existingTemplate.htmlContent);
          }
          this.isLoading = false;
          this.generateFinalHtml();
        },
        error: (err) => {
          this.isLoading = false;
          this.generateFinalHtml();
        },
      });
  }

  // Cette fonction va "deviner" les anciennes valeurs en lisant le code HTML
  extractDataFromHtml(html: string) {
    if (!html) return;

    // 1. Extraire la couleur (cherche le premier 'border: 1px solid #')
    const colorMatch = html.match(/border: 1px solid (#\w+)/);
    if (colorMatch && colorMatch[1]) {
      this.template.borderColor = colorMatch[1];
    }

    // 2. Extraire le titre (cherche le contenu entre <h1> et </h1>)
    const titleMatch = html.match(/<h1[^>]*>([^<]+)<\/h1>/);
    if (titleMatch && titleMatch[1]) {
      this.template.certificateTitle = titleMatch[1].trim();
    }
  }

  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        this.template.trainerSignatureBase64 = (reader.result as string).split(
          ',',
        )[1];
        this.generateFinalHtml();
      };
      reader.readAsDataURL(file);
    }
  }

  generateFinalHtml() {
    const borderColor = this.template.borderColor || '#2563eb';
    const title = this.template.certificateTitle || 'Certificate of Completion';

    this.template.htmlContent = `<!DOCTYPE html>
    <html lang="fr">
    <head>
      <meta charset="UTF-8" />
      <style>
        /* Reset et configuration de base */
        * {
          margin: 0;
          padding: 0;
        }
        
        /* Configuration de la page pour l'impression */
        @page {
          size: A4 landscape;
          margin: 0;
        }
        
        html, body {
          margin: 0;
          padding: 0;
          width: 100%;
          height: 100%;
          background: white;
          font-family: 'Arial', 'Helvetica', sans-serif;
        }

        /* Conteneur principal avec dimensions exactes */
        .certificate-wrapper {
          width: 100%;
          height: 100%;
          position: relative;
          background: white;
        }
        
        /* Conteneur avec la bordure externe */
        .certificate-container {
          width: 100%;
          height: 100%;
          position: relative;
          border: 4px solid ${borderColor};
          box-sizing: border-box;
        }
        
        /* Cadre intérieur décoratif */
        .inner-border {
          position: absolute;
          top: 15px;
          left: 15px;
          right: 15px;
          bottom: 15px;
          border: 1px solid ${borderColor};
        }
        
        /* Table principale pour le centrage vertical */
        .main-table {
          width: 100%;
          height: 100%;
          border-collapse: collapse;
          table-layout: fixed;
        }
        
        .main-table td {
          vertical-align: middle;
          text-align: center;
          padding: 0;
        }
        
        /* Contenu principal centré */
        .content-wrapper {
          width: 100%;
          padding: 20mm;
          display: inline-block;
        }
        
        /* Header avec logo - positionné en haut */
        .header-container {
          position: absolute;
          top: 15mm;
          left: 0;
          right: 0;
          text-align: center;
        }
        
        .logo-img {
          height: 70px;
          width: auto;
          max-width: 200px;
          margin-bottom: 8px;
        }
        
        .badge-text {
          display: block;
          font-size: 11px;
          color: #94a3b8;
          font-weight: bold;
          text-transform: uppercase;
          letter-spacing: 2px;
          margin-top: 5px;
        }
        
        /* Contenu central */
        .center-content {
          text-align: center;
        }
        
        .main-title {
          text-transform: uppercase;
          font-size: 32px;
          font-weight: bold;
          letter-spacing: 4px;
          color: #1e293b;
          margin-bottom: 12px;
        }
        
        .title-divider {
          width: 80px;
          height: 3px;
          background: ${borderColor};
          margin: 0 auto 20px auto;
        }
        
        .certify-text {
          font-size: 18px;
          color: #64748b;
          margin-bottom: 15px;
        }
        
        .learner-name {
          font-size: 48px;
          font-weight: bold;
          color: #0f172a;
          margin: 20px 0;
        }
        
        .achievement-text {
          font-size: 16px;
          color: #64748b;
          margin: 15px 0;
        }
        
        .evaluation-title {
          font-size: 28px;
          font-weight: bold;
          color: ${borderColor};
          margin: 12px 0;
        }
        
        .score-container {
          margin-top: 20px;
        }
        
        .score-badge {
          display: inline-block;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          padding: 10px 30px;
          font-weight: bold;
          font-size: 16px;
          color: #1e293b;
          border-radius: 4px;
        }
        
        /* Footer avec signature et QR code - positionné en bas */
        .footer-container {
          position: absolute;
          bottom: 15mm;
          left: 0;
          right: 0;
        }
        
        .footer-table {
          width: 80%;
          margin: 0 auto;
          border-collapse: collapse;
          table-layout: fixed;
        }
        
        .footer-table td {
          width: 50%;
          text-align: center;
          vertical-align: bottom;
          padding: 0 15px;
        }
        
        .signature-area {
          text-align: center;
        }
        
        .signature-image {
          height: 65px;
          margin-bottom: 8px;
          display: block;
        }
        
        .signature-image img {
          max-height: 65px;
          max-width: 180px;
          width: auto;
          height: auto;
        }
        
        .signature-line {
          width: 180px;
          border-top: 1px solid #cbd5e1;
          margin: 8px auto 5px;
          padding-top: 5px;
        }
        
        .trainer-name {
          font-size: 13px;
          font-weight: bold;
          color: #1e293b;
        }
        
        .trainer-title {
          font-size: 9px;
          color: #64748b;
          text-transform: uppercase;
          font-weight: bold;
        }
        
        .qr-area {
          text-align: center;
        }
        
        .qr-code {
          width: 85px;
          height: 85px;
          margin: 0 auto;
        }
        
        .qr-code img {
          width: 85px;
          height: 85px;
          display: block;
        }
        
        .verification-info {
          font-size: 8px;
          color: #94a3b8;
          margin-top: 8px;
          line-height: 1.4;
        }
        
        /* Media print */
        @media print {
          body {
            margin: 0;
            padding: 0;
          }
          .certificate-wrapper {
            margin: 0;
            padding: 0;
          }
        }
        
        /* Styles pour l'affichage dans Angular */
        .a4-preview {
          background: white;
          box-shadow: 0 0 20px rgba(0,0,0,0.1);
          margin: 0 auto;
        }
      </style>
    </head>
    <body>
      <div class="certificate-wrapper">
        <div class="certificate-container">
          <div class="inner-border"></div>
          
          <!-- Logo et badge en haut -->
          <div class="header-container">
            <img class="logo-img" src="${this.platformLogoUrl}" alt="Logo" />
            <div class="badge-text">Authorized Certification</div>
          </div>
          
          <!-- Table pour le centrage vertical du contenu principal -->
          <table class="main-table" cellpadding="0" cellspacing="0">
            <tr>
              <td>
                <div class="center-content">
                  <!-- Titre -->
                  <div class="main-title">${title}</div>
                  <div class="title-divider"></div>
                  
                  <!-- Corps du certificat -->
                  <div class="certify-text">This is to certify that</div>
                  <div class="learner-name">[[LEARNER_NAME]]</div>
                  <div class="achievement-text">has successfully passed the evaluation for</div>
                  <div class="evaluation-title">[[EVALUATION_TITLE]]</div>
                  
                  <div class="score-container">
                    <div class="score-badge">Grade: [[SCORE]]</div>
                  </div>
                </div>
              </td>
            </tr>
          </table>
          
          <!-- Footer avec signature et QR code en bas -->
          <div class="footer-container">
            <table class="footer-table" cellpadding="0" cellspacing="0">
              <tr>
                <td class="signature-area">
                  <div class="signature-image">
                    ${this.template.trainerSignatureBase64 ? `<img src="data:image/png;base64,${this.template.trainerSignatureBase64}" alt="Signature" />` : ''}
                  </div>
                  <div class="signature-line"></div>
                  <div class="trainer-name">${this.template.trainerName || 'Trainer Name'}</div>
                  <div class="trainer-title">Certified Trainer</div>
                </td>
                <td class="qr-area">
                  <div class="qr-code">
                    <img src="data:image/png;base64,[[QR_CODE]]" alt="QR Code" />
                  </div>
                  <div class="verification-info">
                    Issued on [[DATE]]<br />
                    Verification ID: #FM-[[ID]]
                  </div>
                </td>
              </tr>
            </table>
          </div>
        </div>
      </div>
    </body>
    </html>`;
  }

  save() {
    this.generateFinalHtml();

    // On envoie tout l'objet template qui contient maintenant borderColor et certificateTitle
    const payload = {
      ...this.template,
      evaluationId: Number(this.evaluationId),
    };

    this.certificateService.saveTemplate(payload).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => console.error('Error saving template:', err),
    });
  }

  backToDashboard() {
    this.router.navigate(['/dashboard']);
  }
}
