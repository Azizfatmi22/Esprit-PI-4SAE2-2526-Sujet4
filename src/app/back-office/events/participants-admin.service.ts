import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { EventAnalytics } from '../../front-office/events/models/event-analytics.model';

export interface AdminEvent {
  id: number;
  name: string;
  location?: string;
  date?: string;
  status?: string;
}

export interface AdminParticipant {
  id: number;
  name: string;
  email: string;
}

import { environment } from '../../front-office/environment/envirement';

@Injectable({ providedIn: 'root' })
export class ParticipantsAdminService {
  private apiUrl = `${environment.apiGatewayUrl}/api/events`;
  private analyticsUrl = `${environment.apiGatewayUrl}/api/backoffice/events/analytics`;

  constructor(private http: HttpClient) { }

  getAnalytics(): Observable<EventAnalytics> {
    return this.http.get<EventAnalytics>(this.analyticsUrl).pipe(
      catchError(() => of({
        totalEvents: 0,
        upcomingEvents: 0,
        ongoingEvents: 0,
        finishedEvents: 0,
        averageParticipationRate: 0,
        mostPopularEventName: 'N/A'
      }))
    );
  }

  getEvents(): Observable<AdminEvent[]> {
    return this.http.get<AdminEvent[]>(this.apiUrl).pipe(
      catchError(() => of([]))
    );
  }

  getParticipants(eventId: number): Observable<AdminParticipant[]> {
    return this.http.get<AdminParticipant[]>(`${this.apiUrl}/${eventId}/participants`).pipe(
      catchError(() => this.http.get<AdminParticipant[]>(`${this.apiUrl}/${eventId}/participants/all`)),
      catchError(() => of([]))
    );
  }

  addParticipant(eventId: number, payload: { name: string; email: string }): Observable<AdminParticipant> {
    return this.http.post<AdminParticipant>(`${this.apiUrl}/${eventId}/participants`, payload);
  }

  deleteParticipant(eventId: number, participantId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}/participants/${participantId}`);
  }

  updateEvent(eventId: number, event: Partial<AdminEvent>): Observable<AdminEvent> {
    return this.http.put<AdminEvent>(`${this.apiUrl}/${eventId}`, event);
  }

  deleteEvent(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}`);
  }
}
