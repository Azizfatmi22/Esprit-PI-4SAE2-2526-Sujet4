import { Injectable } from '@angular/core';
//import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';

const API_URL = 'http://localhost:8085/msreclamation';

export interface Reclamation {
  
  id?: number;
  learnerId: string;
  type: string;
  subject: string;
  description: string;
  status?: string;
  priority?: number;
  createdDate?: string;
  updatedDate?: string;
  resolvedDate?: string;
  

  // ── Champs communs optionnels ─────────────────────────────
  contactPhone?: string;
  desiredResolutionDate?: string;   // format "YYYY-MM-DD"
  attachmentUrl?: string;
  additionalInfo?: string;

  // ── TECHNICAL ─────────────────────────────────────────────
  browserName?: string;
  osVersion?: string;
  errorCode?: string;
  errorMessage?: string;

  // ── PAYMENT ───────────────────────────────────────────────
  transactionId?: string;
  paymentDate?: string;             // format "YYYY-MM-DD"
  amount?: number;
  paymentMethod?: string;
  invoiceNumber?: string;

  // ── CONTENT ───────────────────────────────────────────────
  courseId?: number;
  lessonId?: number;
  contentType?: string;
  pageUrl?: string;

  // ── ACCESS ────────────────────────────────────────────────
  accessDate?: string;              // format "YYYY-MM-DD"
  deviceType?: string;

  // ── CERTIFICATE ───────────────────────────────────────────
  completionDate?: string;          // format "YYYY-MM-DD"
  certificateType?: string;

  responses?: ReclamationResponse[];
}

export interface ReclamationResponse {
  id?: number;
  reclamationId: number;
  adminId: number;
  responseText: string;
  isInternal?: boolean;
  createdDate?: string;
}

export interface ReclamationStats {
  total: number;
  pending: number;
  inProgress: number;
  resolved: number;
  closed: number;
  rejected: number;
  unresolved: number;
}

@Injectable({ providedIn: 'root' })
export class ReclamationService {
  getWeeklyVolume() {
    throw new Error('Method not implemented.');
  }

  constructor(private http: HttpClient) {}
  detectType(data: any, token: string): Observable<any> {
  const headers = new HttpHeaders({
    Authorization: `Bearer ${token}`
  });

  return this.http.post(
    `${API_URL}/reclamations/detect-type`,
    data,
    { headers }
  );
}

  // ── Reclamations ────────────────────────────────────────────────────────────
  createReclamation(reclamation: any): Observable<Reclamation> {
    return this.http.post<Reclamation>(`${API_URL}/reclamations`, reclamation);
  }

  getAllReclamations(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${API_URL}/reclamations`);
  }

  getReclamationById(id: number): Observable<Reclamation> {
    return this.http.get<Reclamation>(`${API_URL}/reclamations/${id}`);
  }

  getReclamationsByLearner(learnerId: string): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${API_URL}/reclamations/learner/${learnerId}`);
  }

  getReclamationsByStatus(status: string): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${API_URL}/reclamations/status/${status}`);
  }

  getReclamationsByType(type: string): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${API_URL}/reclamations/type/${type}`);
  }

  getUnresolvedReclamations(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${API_URL}/reclamations/unresolved`);
  }

  updateReclamation(id: number, reclamation: Reclamation): Observable<Reclamation> {
    return this.http.put<Reclamation>(`${API_URL}/reclamations/${id}`, reclamation);
  }

  updateReclamationStatus(id: number, status: string, adminResponse?: string, adminId?: number): Observable<Reclamation> {
    const payload: any = { status };
    if (adminResponse) payload.adminResponse = adminResponse;
    if (adminId)       payload.adminId = adminId;
    return this.http.put<Reclamation>(`${API_URL}/reclamations/${id}/status`, payload);
  }

  deleteReclamation(id: number): Observable<any> {
    return this.http.delete(`${API_URL}/reclamations/${id}`);
  }

  getStatistics(): Observable<ReclamationStats> {
    return this.http.get<ReclamationStats>(`${API_URL}/reclamations/stats`);
  }

  // ── Responses ───────────────────────────────────────────────────────────────
  createResponse(reclamationId: number, adminId: number, responseText: string, isInternal = false): Observable<ReclamationResponse> {
    return this.http.post<ReclamationResponse>(
      `${API_URL}/reclamations/${reclamationId}/responses`,
      { responseText, adminId, isInternal }
    );
  }

  getReclamationResponses(reclamationId: number): Observable<ReclamationResponse[]> {
    return this.http.get<ReclamationResponse[]>(`${API_URL}/reclamations/${reclamationId}/responses`);
  }

  getPublicResponses(reclamationId: number): Observable<ReclamationResponse[]> {
    return this.http.get<ReclamationResponse[]>(`${API_URL}/reclamations/${reclamationId}/responses/public`);
  }

  getInternalComments(reclamationId: number): Observable<ReclamationResponse[]> {
    return this.http.get<ReclamationResponse[]>(`${API_URL}/reclamations/${reclamationId}/responses/internal`);
  }

  updateResponse(responseId: number, responseText: string): Observable<ReclamationResponse> {
    return this.http.put<ReclamationResponse>(`${API_URL}/responses/${responseId}`, { responseText });
  }

  deleteResponse(responseId: number): Observable<any> {
    return this.http.delete(`${API_URL}/responses/${responseId}`);
  }

  addAdminResponse(id: number, response: string, adminId?: number): Observable<Reclamation> {
    const payload: any = { response };
    if (adminId) payload.adminId = adminId;
    return this.http.post<Reclamation>(`${API_URL}/reclamations/${id}/response`, payload);
  }
}