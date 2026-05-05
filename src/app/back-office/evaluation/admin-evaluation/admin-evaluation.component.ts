import { Component, OnInit } from '@angular/core';
import { AdminEvaluationService } from '../../services/admin-evaluation.service';

@Component({
  selector: 'app-admin-evaluation',
  templateUrl: './admin-evaluation.component.html',
  styleUrl: './admin-evaluation.component.scss',
})
export class AdminEvaluationComponent implements OnInit {
  evaluations: any[] = [];
  isLoading = true;

  stats: any = {
    totalEvaluations: 0,
    totalQuizzes: 0,
    totalExams: 0,
    avgSuccessRate: 0,
  };

  constructor(private adminService: AdminEvaluationService) {}

  ngOnInit(): void {
    this.refreshDashboard();
  }

  refreshDashboard() {
    this.isLoading = true;

    // 1. Charger les statistiques réelles
    this.adminService.getAdminStats().subscribe({
      next: (data) => {
        this.stats.totalEvaluations = data.totalEvaluations;
        this.stats.totalQuizzes = data.totalQuizzes;
        this.stats.totalExams = data.totalExams;
        this.stats.avgSuccessRate = data.avgSuccessRate;
      },
      error: (err) => console.error('Erreur Stats Backend:', err),
    });

    // 2. Charger la liste complète
    this.adminService.getAllEvaluations().subscribe({
      next: (data) => {
        this.evaluations = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur Liste:', err);
        this.isLoading = false;
      },
    });
  }

  deleteEvaluation(id: string) {
    if (
      confirm(
        "Attention : Cette action supprimera définitivement l'évaluation et tous les résultats associés.",
      )
    ) {
      this.adminService.deleteEvaluation(id).subscribe({
        next: () => this.refreshDashboard(),
        error: (err) => alert('Erreur lors de la suppression : ' + err.message),
      });
    }
  }
}
