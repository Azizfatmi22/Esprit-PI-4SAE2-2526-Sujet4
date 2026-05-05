import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ReportingService } from '../../services/reporting.service';
import { UserService } from '../../services/user.service';
import { GamificationService } from '../../services/gamification.service';

@Component({
  selector: 'app-learner-certifications',
  templateUrl: './learner-certifications.component.html',
  styleUrl: './learner-certifications.component.scss',
})
export class LearnerCertificationsComponent implements OnInit {
  cleanCertificates: any[] = [];
  bannedCertificates: any[] = [];
  selectedCert: any = null;
  isLoading = true;
  previewScale = 0.8;
  progression: any = null;
  leaderboard: any[] = [];
  badges: any[] = [];

  constructor(
    private reportingService: ReportingService,
    private sanitizer: DomSanitizer,
    private userService: UserService,
    private gamificationService: GamificationService,
  ) {}

  ngOnInit(): void {
    this.loadAll();
    this.loadProgression();
    this.loadBadges();
    console.log(this.badges);
  }

  loadBadges() {
    const learnerId = this.userService.getUser()?.id;
    if (learnerId) {
      this.gamificationService.getLearnerBadges(learnerId).subscribe({
        next: (data) => (this.badges = data),
        error: (err) =>
          console.error('Erreur lors du chargement des badges', err),
      });
    }
  }

  loadProgression() {
    const learnerId = this.userService.getUser()?.id;
    if (learnerId) {
      this.gamificationService.getLearnerProgression(learnerId).subscribe({
        next: (data) => {
          this.progression = data;
        },
        error: (err) => console.error('Erreur gamification', err),
      });

      this.gamificationService.getLeaderboard().subscribe({
        next: (data) => (this.leaderboard = data),
        error: (err) => console.error('Erreur leaderboard', err),
      });
    }
  }

  loadAll() {
    const learnerId = this.userService.getUser()?.id;
    if (!learnerId) {
      this.isLoading = false;
      return;
    }

    this.reportingService.getUserEvaluationHistory(learnerId).subscribe({
      next: (data) => {
        this.cleanCertificates = data.filter(
          (c) => c.vigilanceStatus === 'CLEAN',
        );
        this.bannedCertificates = data.filter(
          (c) => c.vigilanceStatus === 'BANNED',
        );

        // Générer le HTML sécurisé pour chaque certificat
        this.cleanCertificates.forEach((cert) => {
          if (cert.templateHtml) {
            cert.safeHtml = this.generateSafeHtml(cert);
          }
        });
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Erreur lors de la récupération de l'historique", err);
        this.isLoading = false;
      },
    });
  }

  generateSafeHtml(cert: any): SafeHtml {
    let html = cert.templateHtml;

    if (!html)
      return this.sanitizer.bypassSecurityTrustHtml(
        '<div>Template non disponible</div>',
      );

    // Remplacer les placeholders
    html = html
      .replace(/\[\[LEARNER_NAME\]\]/g, cert.learnerName || 'N/A')
      .replace(/\[\[EVALUATION_TITLE\]\]/g, cert.evaluationTitle || 'N/A')
      .replace(/\[\[SCORE\]\]/g, cert.percentage + '%')
      .replace(/\[\[DATE\]\]/g, this.formatDate(cert.receivedAt))
      .replace(/\[\[ID\]\]/g, cert.id);

    // QR code temporaire pour la prévisualisation
    const tempQrData = `Cert-ID:${cert.id}`;
    const tempQrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=100x100&data=${encodeURIComponent(tempQrData)}`;
    html = html.replace(/data:image\/png;base64,\[\[QR_CODE\]\]/g, tempQrUrl);

    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  formatDate(date: string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }

  openPreview(cert: any) {
    if (cert.vigilanceStatus === 'CLEAN') {
      this.selectedCert = cert;
      this.adjustPreviewScale();
    }
  }

  adjustPreviewScale() {
    const screenWidth = window.innerWidth;
    if (screenWidth < 768) {
      this.previewScale = 0.5;
    } else if (screenWidth < 1024) {
      this.previewScale = 0.65;
    } else if (screenWidth < 1440) {
      this.previewScale = 0.75;
    } else {
      this.previewScale = 0.85;
    }
  }

  closePreview() {
    this.selectedCert = null;
  }

  download(event: Event, certId: number) {
    // 1. Empêcher l'ouverture de la modale de preview
    event.stopPropagation();

    this.reportingService.downloadCertificate(certId).subscribe({
      next: (response: any) => {
        // 2. Créer le Blob à partir de la réponse du backend
        const blob = new Blob([response], { type: 'application/pdf' });

        // 3. Créer une URL temporaire pour ce fichier
        const url = window.URL.createObjectURL(blob);

        // 4. Créer un élément <a> invisible pour déclencher le téléchargement
        const link = document.createElement('a');
        link.href = url;

        // Nom du fichier qui apparaîtra sur ton Mac
        link.download = `Certificat_Formini_${certId}.pdf`;

        // 5. Simuler le clic (nécessaire de l'ajouter au body sur certains navigateurs)
        document.body.appendChild(link);
        link.click();

        // 6. Nettoyage : retirer l'élément et libérer la mémoire
        document.body.removeChild(link);
        setTimeout(() => window.URL.revokeObjectURL(url), 100);
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement du PDF:', err);
        // Optionnel : afficher une petite alerte à l'utilisateur
      },
    });
  }
}
