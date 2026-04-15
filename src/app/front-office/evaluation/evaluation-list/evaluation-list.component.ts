import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EvaluationService } from '../../services/evaluation.service';

@Component({
  selector: 'app-evaluation-list',
  templateUrl: './evaluation-list.component.html',
  styleUrl: './evaluation-list.component.scss'
})
export class EvaluationListComponent implements OnInit {
  evaluations: any[] = [];

  constructor(
    private evaluationService: EvaluationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.evaluationService.getAllEvaluations().subscribe(data => {
      this.evaluations = data;
    });
  }

  // Changement ici : 'evaluation' au lieu de 'eval'
  onSelectEvaluation(evaluation: any) {
    if (evaluation.typeAssessment === 'QUIZ') {
      this.router.navigate(['/quizPlayer', evaluation.id]);
    } else if (evaluation.typeAssessment === 'EXAM') {
      this.router.navigate(['/examPlayer', evaluation.id]);
    }
  }
}