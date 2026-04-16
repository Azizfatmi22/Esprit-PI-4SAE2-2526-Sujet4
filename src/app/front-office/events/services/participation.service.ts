import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, switchMap } from 'rxjs';
import { Participant } from '../models/participant.model';

interface AddParticipantRequest {
  name: string;
  email: string;
}

import { environment } from '../../environment/envirement';

@Injectable({
  providedIn: 'root'
})
export class ParticipationService {
  private readonly apiUrl = `${environment.apiGatewayUrl}/api/events`;
  private readonly http = inject(HttpClient);

  addParticipant(eventId: number, payload: AddParticipantRequest): Observable<Participant> {
    return this.http.post<Participant>(`${this.apiUrl}/${eventId}/participants`, payload);
  }

  getParticipants(eventId: number): Observable<Participant[]> {
    return this.http.get<Participant[]>(`${this.apiUrl}/${eventId}/participants`).pipe(
      catchError(() => this.http.get<Participant[]>(`${this.apiUrl}/${eventId}/participants/all`)),
      catchError(() => of([]))
    );
  }

  removeParticipant(eventId: number, participantId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}/participants/${participantId}`);
  }

  confirm(eventId: number, token: string): Observable<void> {
    return this.http.get<void>(`${this.apiUrl}/${eventId}/participants/confirm`, {
      params: { token },
    });
  }
}
