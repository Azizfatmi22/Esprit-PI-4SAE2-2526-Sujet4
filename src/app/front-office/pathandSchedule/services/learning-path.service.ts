// services/learning-path.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LearningPath, PathAnalytics } from '../models/learning-path.model';

@Injectable({
  providedIn: 'root'
})
export class LearningPathService {
  private apiUrl = 'http://localhost:8085/api/learning-paths';

  constructor(private http: HttpClient) { }

  // ==================== CRUD OPERATIONS ====================

  getAllLearningPaths(): Observable<LearningPath[]> {
    return this.http.get<LearningPath[]>(this.apiUrl);
  }

  getLearningPath(id: number): Observable<LearningPath> {
    return this.http.get<LearningPath>(`${this.apiUrl}/${id}`);
  }

  createLearningPath(learningPath: LearningPath): Observable<LearningPath> {
    return this.http.post<LearningPath>(this.apiUrl, learningPath);
  }

  updateLearningPath(id: number, learningPath: LearningPath): Observable<LearningPath> {
    return this.http.put<LearningPath>(`${this.apiUrl}/${id}`, learningPath);
  }

  deleteLearningPath(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ==================== SESSION MANAGEMENT ====================

  addSessionToPath(pathId: number, sessionId: number): Observable<LearningPath> {
    return this.http.post<LearningPath>(`${this.apiUrl}/${pathId}/sessions/${sessionId}`, null);
  }

  removeSessionFromPath(pathId: number, sessionId: number): Observable<LearningPath> {
    return this.http.delete<LearningPath>(`${this.apiUrl}/${pathId}/sessions/${sessionId}`);
  }

  // ==================== NEW ADVANCED FUNCTIONS ====================

  /**
   * 1. Calculate Path Complexity
   * Returns complexity score, level, total sessions, total hours, and average hours per session
   */
  calculatePathComplexity(pathId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${pathId}/complexity`);
  }

  /**
   * 2. Predict Completion Rate
   * Returns predicted completion rate, risk level, total hours, and total sessions
   */
  predictCompletionRate(pathId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${pathId}/completion-rate`);
  }

  /**
   * 3. Generate Learning Summary
   * Returns a formatted summary with path details, objectives, and key figures
   */
  generateLearningSummary(pathId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/${pathId}/summary`, { responseType: 'text' });
  }

  /**
   * 4. Get Optimal Learning Order
   * Returns sessions sorted by hours (easier sessions first)
   */
  getOptimalLearningOrder(pathId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${pathId}/optimal-order`);
  }

  /**
   * 5. Filter Courses by Level
   * Returns courses filtered by level (BEGINNER, INTERMEDIATE, ADVANCED)
   */
  filterCoursesByLevel(level: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/courses/filter/by-level?level=${level}`);
  }

  /**
   * 6. Filter Courses by Description
   * Returns courses containing keyword in title or description
   */
  filterCoursesByDescription(keyword: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/courses/filter/by-description?keyword=${keyword}`);
  }

  // ==================== LEGACY FUNCTIONS ====================

  calculateTotalHours(pathId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${pathId}/hours`);
  }

  analyzePathDifficulty(pathId: number): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/${pathId}/difficulty`, { responseType: 'text' as 'json' });
  }

  detectPathRisks(pathId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/${pathId}/risks`);
  }

  getPathAnalytics(pathId: number): Observable<PathAnalytics> {
    return new Observable(observer => {
      const analytics: PathAnalytics = {
        totalHours: 0,
        difficulty: 'EASY',
        risks: [],
        sessionCount: 0,
        completionRate: 0
      };

      this.getLearningPath(pathId).subscribe({
        next: (path) => {
          analytics.sessionCount = path.sessionIds?.length || 0;
          
          this.calculateTotalHours(pathId).subscribe({
            next: (hours) => {
              analytics.totalHours = hours;
              
              this.analyzePathDifficulty(pathId).subscribe({
                next: (difficulty) => {
                  analytics.difficulty = difficulty as 'EASY' | 'MEDIUM' | 'HARD';
                  
                  this.detectPathRisks(pathId).subscribe({
                    next: (risks) => {
                      analytics.risks = risks;
                      observer.next(analytics);
                      observer.complete();
                    },
                    error: (err) => observer.error(err)
                  });
                },
                error: (err) => observer.error(err)
              });
            },
            error: (err) => observer.error(err)
          });
        },
        error: (err) => observer.error(err)
      });
    });
  }
}