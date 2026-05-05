import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8085/msenrollment';

export interface Enrollment {
  id: number;
  learnerId: number;
  courseId: number;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  progress: number;
  enrolledDate: string;
  completedDate?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminEnrollmentService {

  constructor(private http: HttpClient) {}

  getAllEnrollments(): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${API_URL}/enrollments/all`);
  }

  getByLearner(learnerId: number): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${API_URL}/enrollments/learner/${learnerId}`);
  }

  updateStatus(enrollmentId: number, status: string): Observable<Enrollment> {
    return this.http.put<Enrollment>(
      `${API_URL}/enrollments/${enrollmentId}/status`,
      { status }
    );
  }

  cancelEnrollment(enrollmentId: number): Observable<string> {
    return this.http.delete<string>(
      `${API_URL}/enrollments/${enrollmentId}`,
      { responseType: 'text' as 'json' }
    );
  }
}