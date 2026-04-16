import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AdminGamificationService {
  private readonly baseUrl = 'http://localhost:8085/reporting/badges';

  constructor(private http: HttpClient) {}

  getBadges(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  getBadge(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  createBadge(badge: any): Observable<any> {
    return this.http.post<any>(this.baseUrl, badge);
  }

  updateBadge(id: number, badge: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, badge);
  }

  deleteBadge(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }
}
