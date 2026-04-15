import { isPlatformBrowser } from '@angular/common';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { ReportingService } from '../../services/reporting.service';

@Component({
  selector: 'app-evaluation-result',
  templateUrl: './evaluation-result.component.html',
  styleUrl: './evaluation-result.component.scss',
})
export class EvaluationResultComponent implements OnInit {
  result: any;
  isDownloading: boolean = false;

  constructor(
    private router: Router,
    private reportingService: ReportingService,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.result = window.history.state?.result;
      if (!this.result) {
        this.router.navigate(['/dashboard']);
      }
    }
  }

  getCirclePercentage(): number {
    return (this.result.scoreObtained / this.result.totalPossiblePoints) * 100;
  }

  getCircleColor(): string {
    console.log("Résultat de l'évaluation :", this.result);
    return this.result.isPassed ? 'var(--success)' : 'var(--danger)';
  }

  generateCertificate() {
    if (!this.result.isPassed) {
      alert("Vous n'avez pas obtenu le score nécessaire pour le certificat.");
      return;
    }

    this.isDownloading = true;

    // 1. Trouver l'historique correspondant
    this.reportingService
      .getHistoryByLearnerAndEvaluation(
        this.result.learnerId,
        this.result.evaluationId,
      )
      .subscribe({
        next: (history) => {
          if (history && history.id) {
            // 2. Lancer le téléchargement avec l'ID trouvé
            this.downloadPdf(history.id);
          } else {
            alert('Historique introuvable.');
            this.isDownloading = false;
          }
        },
        error: (err) => {
          console.error("Erreur lors de la recherche de l'historique", err);
          this.isDownloading = false;
        },
      });
  }

  private downloadPdf(historyId: number) {
    this.reportingService.downloadCertificate(historyId).subscribe({
      next: (blob: Blob) => {
        // Création d'un lien invisible pour déclencher le téléchargement navigateur
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Certificat_${this.result.evaluationTitle.replace(/\s+/g, '_')}.pdf`;
        link.click();

        window.URL.revokeObjectURL(url);
        this.isDownloading = false;
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement du PDF', err);
        alert(
          'Erreur lors de la génération du PDF. Vérifiez que le template existe.',
        );
        this.isDownloading = false;
      },
    });
  }
}
