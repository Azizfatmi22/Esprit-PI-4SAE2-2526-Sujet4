import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EvaluationService } from '../../services/evaluation.service';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { Location } from '@angular/common';

@Component({
  selector: 'app-evaluation-stats',
  templateUrl: './evaluation-stats.component.html',
  styleUrls: ['./evaluation-stats.component.scss'],
})
export class EvaluationStatsComponent implements OnInit {
  evaluationId!: string;
  evaluationTitle: string = '';
  difficultyStats: any[] = [];
  totalQuestions: number = 0;
  isLoading = true;
  analysisMessage: string = '';
  hasHardQuestions = false;
  hardQuestionsCount = 0;
  isRemediating = false;

  // Configuration modifiée pour une COURBE (Line Chart)
  public lineChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { backgroundColor: '#2b579a' },
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: { display: true, color: '#f0f0f0' },
        title: {
          display: true,
          text: "Nombre d'étudiants",
          color: '#666',
          font: { weight: 'bold' },
        },
      },
      x: {
        grid: { display: false },
        title: {
          display: true,
          text: 'Tranches de scores (%)',
          color: '#666',
          font: { weight: 'bold' },
        },
      },
    },
  };

  public lineChartData: ChartData<'line'> = {
    labels: ['0-20%', '21-40%', '41-60%', '61-80%', '81-100%'],
    datasets: [
      {
        data: [],
        borderColor: '#3498db',
        backgroundColor: 'rgba(52, 152, 219, 0.2)',
        fill: true, // Pour remplir sous la courbe
        tension: 0.4, // Pour arrondir la ligne
        pointBackgroundColor: '#2980b9',
        pointBorderColor: '#fff',
        pointHoverRadius: 6,
      },
    ],
  };

  constructor(
    private route: ActivatedRoute,
    private evaluationService: EvaluationService,
    private location: Location,
  ) {}

  ngOnInit(): void {
    this.evaluationId = this.route.snapshot.paramMap.get('id')!;
    this.loadEvaluationInfo();
    this.loadStats();
  }

  loadEvaluationInfo() {
    this.evaluationService
      .getEvaluationById(this.evaluationId)
      .subscribe((data) => {
        this.evaluationTitle = data.title;
      });
  }

  loadStats() {
    this.isLoading = true;

    this.evaluationService
      .getQuestionAnalysis(this.evaluationId)
      .subscribe((data) => {
        this.totalQuestions = data.length;
        this.difficultyStats = data;
        const hardQs = data.filter((q) => q.difficultyLevel === 'HARD');
        this.hasHardQuestions = hardQs.length > 0;
        this.hardQuestionsCount = hardQs.length;
      });

    this.evaluationService
      .getScoreDistribution(this.evaluationId)
      .subscribe((data) => {
        const counts = data.map((d: any) => d.count);
        this.lineChartData.datasets[0].data = counts;
        this.generateAnalysisMessage(data);
        this.isLoading = false;
      });
  }

  onRemediate() {
    this.isRemediating = true;
    this.evaluationService.triggerRemediation(this.evaluationId).subscribe({
      next: () => {
        this.isRemediating = false;
        alert(
          '🎯 Succès : Les ressources de remédiation ont été envoyées aux étudiants en difficulté !',
        );
      },
      error: () => {
        this.isRemediating = false;
        alert("Erreur lors de l'envoi de la remédiation.");
      },
    });
  }

  generateAnalysisMessage(data: any[]) {
    const total = data.reduce((acc, curr) => acc + curr.count, 0);
    if (total === 0) return;

    const lowScores = data[0].count + data[1].count; // 0-40%
    const highScores = data[3].count + data[4].count; // 61-100%

    if (lowScores > highScores) {
      this.analysisMessage =
        "L'examen semble difficile : une majorité d'étudiants se situe dans les tranches de scores inférieures.";
    } else if (highScores > lowScores) {
      this.analysisMessage =
        'Excellente performance globale : la courbe montre une forte concentration de réussite dans les tranches élevées.';
    } else {
      this.analysisMessage =
        "La répartition des scores est équilibrée sur l'ensemble de la promotion.";
    }
  }

  getDifficultyColor(level: string): string {
    switch (level) {
      case 'HARD':
        return '#e74c3c';
      case 'MEDIUM':
        return '#f39c12';
      case 'EASY':
        return '#27ae60';
      default:
        return '#7f8c8d';
    }
  }

  goBack() {
    this.location.back();
  }
}
