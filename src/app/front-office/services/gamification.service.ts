import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class GamificationService {
  private readonly reportingUrl = 'http://localhost:8085/reporting';
  constructor(private http: HttpClient) {}

  getLearnerProgression(learnerId: string): Observable<any> {
    return this.http.get(
      `${this.reportingUrl}/gamification/profile/${learnerId}`,
    );
  }

  getLeaderboard(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.reportingUrl}/gamification/leaderboard`,
    );
  }
  getLearnerBadges(learnerId: string): Observable<any[]> {
  return this.http.get<any[]>(`${this.reportingUrl}/badges/achievements/${learnerId}`);
}

}
