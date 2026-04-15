import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface EvaluationStats {
  totalEvaluations: number;
  totalQuizzes: number;
  totalExams: number;
  avgSuccessRate: number;
}

@Injectable({
  providedIn: 'root',
})
export class AdminEvaluationService {
  private apiUrl = 'http://localhost:8085/evaluations';

  constructor(private http: HttpClient) {}

  // Récupère les statistiques calculées par le backend
  getAdminStats(): Observable<EvaluationStats> {
    return this.http.get<EvaluationStats>(`${this.apiUrl}/admin/stats`);
  }

  // Récupère toutes les évaluations du système
  getAllEvaluations(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  // Suppression administrative
  deleteEvaluation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
