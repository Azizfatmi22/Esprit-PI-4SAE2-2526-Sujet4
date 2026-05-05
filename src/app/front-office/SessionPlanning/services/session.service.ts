import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Session } from '../models/session';

@Injectable({
  providedIn: 'root'
})
export class SessionService {

   private apiUrl = 'http://localhost:8085/api/sessions';

  constructor(private http: HttpClient) { }

  // POST /api/sessions
  createSession(session: Session): Observable<Session> {
    return this.http.post<Session>(this.apiUrl, session);
  }
  getSessionsByUserAndCourse(userId: string, courseId: number): Observable<Session[]> {
  const params = new HttpParams()
    .set('userId', userId.toString())
    .set('courseId', courseId.toString());

  return this.http.get<Session[]>(`${this.apiUrl}/by-user-course`, { params });
}
  // GET /api/sessions - Get all sessions
  getAllSessions(): Observable<Session[]> {
    return this.http.get<Session[]>(this.apiUrl);
  }

  // GET /api/sessions/{id} - Get session by ID
  getSessionById(id: number): Observable<Session> {
    return this.http.get<Session>(`${this.apiUrl}/${id}`);
  }

  // PUT /api/sessions/{id} - Update session
  updateSession(id: number, session: Session): Observable<Session> {
    return this.http.put<Session>(`${this.apiUrl}/${id}`, session);
  }

  // DELETE /api/sessions/{id} - Delete session
  deleteSession(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // POST /api/sessions/{id}/cancel - Cancel a session
  cancelSession(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/cancel`, {});
  }
  // POST /api/sessions/update-statuses - Update all session statuses
  updateAllStatuses(): Observable<string> {
 return this.http.post(
  `${this.apiUrl
  }/update-statuses`,
  {},
  { responseType: 'text' }
);
}

  // GET /api/sessions/check-trainer-availability - Check if trainer is available
  checkTrainerAvailability(trainerId: string, date: string): Observable<boolean> {
    const params = new HttpParams()
      .set('trainerId', trainerId)
      .set('date', date);
    
    return this.http.get<boolean>(`${this.apiUrl}/check-trainer-availability`, { params });
  }

  // GET /api/sessions/check-scheduling-conflict - Check for scheduling conflict
  checkSchedulingConflict(trainerId: string, locationId: number, date: string): Observable<boolean> {
    const params = new HttpParams()
      .set('trainerId', trainerId)
      .set('locationId', locationId.toString())
      .set('date', date);
    
    return this.http.get<boolean>(`${this.apiUrl}/check-scheduling-conflict`, { params });
  }

  // GET /api/sessions/check-trainer-overload - Check trainer overload
  checkTrainerOverload(trainerId: string, date: string): Observable<boolean> {
    const params = new HttpParams()
      .set('trainerId', trainerId)
      .set('date', date);
    
    return this.http.get<boolean>(`${this.apiUrl}/check-trainer-overload`, { params });
  }

  getSessionByTrainerId(): Observable<Session[]> {
  // No params needed, JWT will be sent automatically via your interceptor
  return this.http.get<Session[]>(`${this.apiUrl}/trainer`);
}
 updateAllSessionsStatus(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/status/update-all`, {});
  }
    
}
