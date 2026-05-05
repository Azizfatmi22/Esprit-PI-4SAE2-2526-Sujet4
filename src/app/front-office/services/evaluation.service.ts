import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { EvaluationConfig } from '../evaluation/models/evaluation.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { UserService } from './user.service';

export interface QuizResult {
  score: number;
  percentage: number;
  success: boolean;
}

import { environment } from '../environment/envirement';

@Injectable({
  providedIn: 'root',
})
export class EvaluationService {
  
  // --- Chemins de base ---
  private readonly baseUrl = `${environment.apiGatewayUrl}/evaluations`;

  // Chemins racines pour les réponses (sans le /submit pour réutilisation)
  private readonly examAnswersPath = `${this.baseUrl}/exam/answers`;
  private readonly quizAnswersPath = `${this.baseUrl}/quiz/answers`;

  private currentEvaluation = new BehaviorSubject<EvaluationConfig | null>(
    null,
  );
  private currentEvaluationId: string | null = null;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private userService: UserService,
  ) {}

  // Dans evaluation.service.ts
  triggerRemediation(evaluationId: string): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/stats/${evaluationId}/remediate`,
      {},
    );
  }
  // Dans evaluation.service.ts
  updateDate(id: string, dateValue: any): Observable<any> {
    // Si la dateValue est un objet Date, on le convertit en string ISO pour le Backend
    const formattedDate =
      dateValue instanceof Date ? dateValue.toISOString() : dateValue;

    return this.http.patch(`${this.baseUrl}/${id}/reschedule`, {
      date: formattedDate,
    });
  }
 

  setEvaluation(config: EvaluationConfig, id: string | null = null) {
    this.currentEvaluation.next(config);
    this.currentEvaluationId = id;
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.setItem('pending_evaluation', JSON.stringify(config));
      if (id) sessionStorage.setItem('pending_id', id);
      else sessionStorage.removeItem('pending_id');
    }
  }

  getCurrentEvaluation(): EvaluationConfig | null {
    if (this.currentEvaluation.value) return this.currentEvaluation.value;
    if (isPlatformBrowser(this.platformId)) {
      const saved = sessionStorage.getItem('pending_evaluation');
      if (saved) {
        const parsed = JSON.parse(saved);
        this.currentEvaluation.next(parsed);
        return parsed;
      }
    }
    return null;
  }

  getCurrentEvaluationId(): string | null {
    if (this.currentEvaluationId) return this.currentEvaluationId;
    if (isPlatformBrowser(this.platformId)) {
      return sessionStorage.getItem('pending_id');
    }
    return null;
  }

  clear() {
    this.currentEvaluation.next(null);
    this.currentEvaluationId = null;
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('pending_evaluation');
      sessionStorage.removeItem('pending_id');
    }
  }

  // --- Appels API : Évaluations (CRUD) ---

  getAllEvaluations(): Observable<EvaluationConfig[]> {
    return this.http.get<EvaluationConfig[]>(this.baseUrl);
  }

  getEvaluationById(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  getEvaluationDetails(id: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/details/${id}`);
  }

  saveCompleteEvaluation(questions: any[]): Observable<any> {
    const config = this.getCurrentEvaluation();
    if (!config) throw new Error('Configuration manquante');
    const payload = this.buildPayload(config, questions);
    return this.http.post(this.baseUrl, payload);
  }

  updateCompleteEvaluation(questions: any[]): Observable<any> {
    const config = this.getCurrentEvaluation();
    const id = this.getCurrentEvaluationId();
    if (!config || !id) throw new Error('Configuration ou ID manquant');
    const payload = { ...this.buildPayload(config, questions), id: id };
    return this.http.put(`${this.baseUrl}/${id}`, payload);
  }

  deleteEvaluation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // --- Appels API : Réponses (Submit) ---

  saveExamAnswers(answers: any[]): Observable<any> {
    return this.http.post(`${this.examAnswersPath}/submit`, answers);
  }

  saveQuizAnswers(answers: any[]): Observable<QuizResult> {
    // Utilisation dynamique du suffixe /submit
    return this.http.post<QuizResult>(
      `${this.quizAnswersPath}/submit`,
      answers,
    );
  }

  // --- Helper interne pour la construction du Payload ---

  private buildPayload(config: EvaluationConfig, questions: any[]) {
    const formattedDate =
      config.date instanceof Date
        ? config.date.toISOString().slice(0, 16)
        : config.date;
    const trainerId = this.userService.getUser()?.id;

    return {
      courseId: config.courseId,
      trainerId: trainerId,
      title: config.title,
      duration: config.duration,
      date: formattedDate,
      typeAssessment: config.type,
      minSuccessScore: config.minSuccessScore,
      quizzQuestions: config.type === 'QUIZ' ? questions : [],
      examQuestions: config.type === 'EXAM' ? questions : [],
    };
  }

  getEvaluationsByTrainer(): Observable<EvaluationConfig[]> {
    const trainerId = this.userService.getUser()?.id;
    return this.http.get<EvaluationConfig[]>(
      `${this.baseUrl}/trainer/${trainerId}`,
    );
  }

  // Récupère l'analyse de difficulté par question
  getQuestionAnalysis(id: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.baseUrl}/stats/${id}/questions-analysis`,
    );
  }

  // Récupère la distribution des scores (Buckets)
  getScoreDistribution(id: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/stats/${id}/distribution`);
  }
}
