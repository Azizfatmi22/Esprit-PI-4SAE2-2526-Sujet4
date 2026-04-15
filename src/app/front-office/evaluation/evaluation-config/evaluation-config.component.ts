import { Component } from '@angular/core';
import { EvaluationConfig, EvaluationType } from '../models/evaluation.model';
import { EvaluationService } from '../../services/evaluation.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-evaluation-config',
  templateUrl: './evaluation-config.component.html',
  styleUrl: './evaluation-config.component.scss',
})
export class EvaluationConfigComponent {
  isEditMode = false;
  editId: string | null = null;
  evaluationConfig: EvaluationConfig = {
    courseId: 1,
    title: '',
    duration: 60,
    date: new Date(),
    type: 'QUIZ',
    minSuccessScore: 70,
  };

  selectedType: EvaluationType = 'QUIZ';
  minDate: string;
  courseIdParam: number | null = null;

  constructor(
    private router: Router,
    private evaluationService: EvaluationService,
    private route: ActivatedRoute,
  ) {
    const today = new Date();
    this.minDate = today.toISOString().slice(0, 16);
  }

  ngOnInit() {
    this.route.queryParamMap.subscribe((params) => {
      this.courseIdParam = Number(params.get('courseId'));
    });

    this.editId = this.route.snapshot.paramMap.get('id');

    if (this.editId) {
      this.isEditMode = true;
      const existingConfig = this.evaluationService.getCurrentEvaluation();
      if (existingConfig) {
        this.evaluationConfig = { ...existingConfig };
        this.selectedType = existingConfig.type;
      }
    } else {
      this.evaluationService.clear();
      const now = new Date();
      const formattedNow = now.toISOString().slice(0, 16);
      this.evaluationConfig = {
        courseId: this.courseIdParam || 1,
        title: '',
        duration: 60,
        date: formattedNow as any,
        type: 'QUIZ',
        minSuccessScore: 70,
      };
      this.selectedType = 'QUIZ';
    }
  }

  selectType(type: EvaluationType) {
    this.selectedType = type;
    this.evaluationConfig.type = type;
  }

  getSuccessMessage(): string {
    return `L'apprenant valide l'évaluation s'il dépasse ${this.evaluationConfig.minSuccessScore}%`;
  }

  proceedToNextStep() {
    if (!this.validateConfig()) {
      alert('Veuillez remplir tous les champs');
      return;
    }
    this.evaluationService.setEvaluation(this.evaluationConfig, this.editId);
    const folder = this.selectedType === 'QUIZ' ? 'quiz' : 'exam';
    const action = this.isEditMode ? 'edit' : 'create';
    const url = this.isEditMode
      ? [`/${folder}/${action}`, this.editId]
      : [`/${folder}/${action}`];

    this.router.navigate(url);
  }

  private validateConfig(): boolean {
    return (
      !!this.evaluationConfig.title.trim() &&
      this.evaluationConfig.duration > 0 &&
      !!this.evaluationConfig.date
    );
  }
}
