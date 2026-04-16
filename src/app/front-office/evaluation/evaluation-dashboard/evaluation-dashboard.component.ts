import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EvaluationService } from '../../services/evaluation.service';
import { CourseService } from '../../services/course.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export enum EvaluationType {
  QUIZ = 'QUIZ',
  EXAM = 'EXAM',
}

export interface EvaluationConfig {
  id?: string;
  courseId: number;
  title: string;
  duration: number;
  date: Date;
  type: EvaluationType;
  minSuccessScore: number;
  createdAt?: Date;
  courseName?: string;
}
@Component({
  selector: 'app-evaluation-dashboard',
  templateUrl: './evaluation-dashboard.component.html',
  styleUrl: './evaluation-dashboard.component.scss',
})
export class EvaluationDashboardComponent implements OnInit {
  evaluations: EvaluationConfig[] = [];
  filteredEvaluations: EvaluationConfig[] = [];
  searchTerm: string = '';
  selectedType: string = 'all';
  activeMenuId: string | null = null;

  // Pour l'affichage de l'enum dans le template
  EvaluationType = EvaluationType;

  showCertificateEditor = false;
  selectedEvaluationId: number | null = null;

  constructor(
    private router: Router,
    private evaluationService: EvaluationService,
    private courseService: CourseService,
  ) {}

  ngOnInit() {
    this.evaluationService.clear();
    this.loadEvaluations();
  }

  // Créez une interface locale pour gérer l'édition
  editingDateId: string | null = null;

  // Méthode pour activer l'édition
  startEditingDate(evaluationId: string) {
    this.editingDateId = evaluationId;
  }

  // Méthode pour sauvegarder
  saveNewDate(evaluation: EvaluationConfig) {
    const dateStr = evaluation.date.toString();

    this.evaluationService.updateDate(evaluation.id!, dateStr).subscribe({
      next: () => {
        this.activeMenuId = null;
        evaluation.date = new Date(dateStr);
      },
      error: (err) => alert('Erreur lors de la mise à jour'),
    });
  }

  loadEvaluations() {
    this.evaluationService.getEvaluationsByTrainer().subscribe({
      next: (data) => {
        // 1. On prépare la liste de base
        this.evaluations = data.map((e: any) => ({
          ...e,
          type: e.typeAssessment as EvaluationType,
          courseName: 'Chargement...',
        }));

        const requests = this.evaluations.map((evaluation) => {
          if (evaluation.courseId) {
            return this.courseService
              .getCourseTitle(evaluation.courseId)
              .pipe(catchError(() => of('Aucun cours lié')));
          }
          return of('Aucun cours lié');
        });

        forkJoin(requests).subscribe((titles) => {
          titles.forEach((title, index) => {
            this.evaluations[index].courseName = title;
          });
          this.applyFilter();
        });
      },
      error: (err) => console.error('Erreur lors du chargement', err),
    });
  }
  applyFilter() {
    let filtered = [...this.evaluations];
    if (this.searchTerm && this.searchTerm.trim() !== '') {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter((e) => e.title.toLowerCase().includes(term));
    }
    if (this.selectedType !== 'all') {
      filtered = filtered.filter(
        (e) => e.type && e.type.toString() === this.selectedType,
      );
    }
    this.filteredEvaluations = filtered;
  }

  toggleMenu(evaluationId: string, event: MouseEvent) {
    event.stopPropagation();
    this.activeMenuId =
      this.activeMenuId === evaluationId ? null : evaluationId;
  }

  closeMenu() {
    this.activeMenuId = null;
  }

  viewEvaluation(evaluation: EvaluationConfig) {
    const basePath =
      evaluation.type === EvaluationType.QUIZ ? '/quizPlayer' : '/examPlayer';
    this.router.navigate([basePath, evaluation.id]);
    this.closeMenu();
  }

  editEvaluation(evaluation: EvaluationConfig) {
    this.evaluationService.setEvaluation(evaluation, evaluation.id);
    this.router.navigate(['/evaluation/edit', evaluation.id]);
    this.closeMenu();
  }

  deleteEvaluation(evaluation: EvaluationConfig) {
    const confirmDelete = window.confirm(
      `Êtes-vous sûr de vouloir supprimer l'évaluation "${evaluation.title}" ainsi que toutes ses questions ?`,
    );
    if (confirmDelete && evaluation.id) {
      this.evaluationService.deleteEvaluation(evaluation.id).subscribe({
        next: () => {
          this.evaluations = this.evaluations.filter(
            (e) => e.id !== evaluation.id,
          );
          this.applyFilter();
          this.closeMenu();
          console.log('Suppression réussie');
        },
        error: (err) => {
          console.error('Erreur lors de la suppression:', err);
          alert('Erreur serveur : Impossible de supprimer l évaluation.');
        },
      });
    }
  }

  createNewEvaluation() {
    this.router.navigate(['/evaluation']);
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getFileIcon(type: EvaluationType): string {
    return type === EvaluationType.QUIZ ? '📋' : '📄';
  }

  getTypeLabel(type: EvaluationType): string {
    return type === EvaluationType.QUIZ ? 'Quiz' : 'Examen';
  }

  getTypeColor(type: EvaluationType): string {
    return type === EvaluationType.QUIZ ? '#8b5cf6' : '#3b82f6';
  }

  viewStats(evaluation: any) {
    this.activeMenuId = null; // Ferme le menu
    this.router.navigate(['/evaluations/stats', evaluation.id]);
  }

  getResults(id: any) {
    this.router.navigate(['/evaluations/history', id]);
  }
}
