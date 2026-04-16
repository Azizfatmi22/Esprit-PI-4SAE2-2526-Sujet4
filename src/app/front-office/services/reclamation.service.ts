import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8085/msreclamation';

export interface Reclamation {
  id?: number;
  learnerId: string;
  adminId?: number;
  type: string;
  subject: string;
  description: string;
  status?: string;
  priority?: number;
  createdDate?: string;
  updatedDate?: string;
  resolvedDate?: string;
  // Satisfaction
  satisfactionScore?: number;    // 1–5
  satisfactionComment?: string;
  satisfactionDate?: string;
  // Communs optionnels
  contactPhone?: string;
  desiredResolutionDate?: string;
  attachmentUrl?: string;
  additionalInfo?: string;
  // TECHNICAL
  browserName?: string;
  osVersion?: string;
  errorCode?: string;
  errorMessage?: string;
  // PAYMENT
  transactionId?: string;
  paymentDate?: string;
  amount?: number;
  paymentMethod?: string;
  invoiceNumber?: string;
  // CONTENT
  courseId?: number;
  lessonId?: number;
  contentType?: string;
  pageUrl?: string;
  // ACCESS
  accessDate?: string;
  deviceType?: string;
  // CERTIFICATE
  completionDate?: string;
  certificateType?: string;
  responses?: ReclamationResponse[];

  externalTicketId?: string;
  externalTicketUrl?: string;
  externalTool?: string;

  isSuspect?: boolean;
  moderationReason?: string;
  moderationFlag?: boolean;
  flaggedWords?: string[];
  
  moderatedBy?: number;
  moderatedDate?: string;

  gravityScore?: number;
    gravityLevel?: string; 

  
}

export interface ReclamationResponse {
  id?: number;
  reclamationId: number;
  learnerId: string | number;
  adminId?: number;
  
  responseText: string;
  isInternal?: boolean;
  senderType?: string;   // 'ADMIN' | 'LEARNER'
  createdDate?: string;
  reaction?: string;
  quotedText?: string;    // ← ajouter
  quotedAuthor?: string; 
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

export interface TimelineEvent {
  status: string;
  label: string;
  date?: string;
  actor?: string;
  icon: string;
  done: boolean;
  current: boolean;
}

export interface SatisfactionPayload {
  score: number;        // 1–5
  comment?: string;
  learnerId: string;
}

export interface CsatStats {
  averageScore: number;
  totalRatings: number;
  distribution: { score: number; count: number }[];
}

export interface SlaAlert {
  id: number;
  reclamationId: number;
  priority: number;
  slaMinutes: number;
  elapsedMinutes: number;
  alertDate: string;
  emailSent: boolean;
  resolved: boolean;
}

@Injectable({ providedIn: 'root' })
export class ReclamationService {

  constructor(private http: HttpClient) {}

  // ── Reclamations ─────────────────────────────────────────────────────────────
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

  updateReclamationStatus(id: number, status: string, adminResponse?: string, learnerId?: string | number): Observable<Reclamation> {
    const payload: any = { status };
    if (adminResponse) payload.adminResponse = adminResponse;
    if (learnerId) payload.learnerId = learnerId;
    return this.http.put<Reclamation>(`${API_URL}/reclamations/${id}/status`, payload);
  }

  deleteReclamation(id: number): Observable<any> {
    return this.http.delete(`${API_URL}/reclamations/${id}`);
  }

  getStatistics(): Observable<ReclamationStats> {
    return this.http.get<ReclamationStats>(`${API_URL}/reclamations/stats`);
  }

  // ── Responses ────────────────────────────────────────────────────────────────
  createResponse(reclamationId: number, adminId: string | number, responseText: string, isInternal = false, senderType = 'ADMIN'): Observable<ReclamationResponse> {
  return this.http.post<ReclamationResponse>(
    `${API_URL}/reclamations/${reclamationId}/responses`,
    { responseText, adminId, isInternal, senderType }  // ← adminId au lieu de learnerId
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

  addAdminResponse(id: number, response: string, learnerId?: number): Observable<Reclamation> {
    const payload: any = { response };
    if (learnerId) payload.learnerId = learnerId;
    return this.http.post<Reclamation>(`${API_URL}/reclamations/${id}/response`, payload);
  }

  // ── Satisfaction ─────────────────────────────────────────────────────────────
  submitSatisfaction(reclamationId: number, payload: SatisfactionPayload): Observable<Reclamation> {
    return this.http.post<Reclamation>(
      `${API_URL}/reclamations/${reclamationId}/satisfaction`,
      payload
    );
  }

  getCsatStats(): Observable<CsatStats> {
    return this.http.get<CsatStats>(`${API_URL}/reclamations/csat`);
  }

  // ── IA Suggestions ────────────────────────────────────────────────────────────
  getAiSuggestions(reclamationId: number): Observable<{ suggestions: string[] }> {
    return this.http.post<{ suggestions: string[] }>(
      `${API_URL}/ai/suggest/${reclamationId}`,
      {}
    );
  }

  // ── Timeline ──────────────────────────────────────────────────────────────────
  // Construit la timeline localement à partir des données de la réclamation
  buildTimeline(rec: Reclamation, responses: ReclamationResponse[]): TimelineEvent[] {
    const steps: { status: string; label: string; icon: string }[] = [
      { status: 'PENDING',     label: 'Réclamation soumise',  icon: '📝' },
      { status: 'IN_PROGRESS', label: 'Prise en charge',      icon: '🔄' },
      { status: 'RESOLVED',    label: 'Résolue',              icon: '✅' },
      { status: 'CLOSED',      label: 'Dossier fermé',        icon: '🔒' },
    ];

    const statusOrder = ['PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
    const currentIdx = statusOrder.indexOf(rec.status || 'PENDING');

    // Trouver la première réponse admin pour IN_PROGRESS
    const firstAdminResp = responses.find(r => !r.isInternal);

    return steps.map((step, idx) => {
      let date: string | undefined;
      let actor: string | undefined;

      if (step.status === 'PENDING') {
        date = rec.createdDate;
        actor = `Apprenant #${rec.learnerId}`;
      } else if (step.status === 'IN_PROGRESS') {
        date = firstAdminResp?.createdDate || rec.updatedDate;
        actor = rec.learnerId ? `Apprenant #${rec.learnerId}` : 'Support';
      } else if (step.status === 'RESOLVED') {
        date = rec.resolvedDate;
        actor = rec.learnerId ? `Apprenant #${rec.learnerId}` : 'Support';
      } else if (step.status === 'CLOSED') {
        date = rec.status === 'CLOSED' ? rec.updatedDate : undefined;
        actor = 'Automatique';
      }

      return {
        ...step,
        date,
        actor,
        done:    idx < currentIdx || (rec.status === 'REJECTED' ? false : idx <= currentIdx),
        current: rec.status === step.status,
      };
    });
  }
  getSlaAlerts(): Observable<SlaAlert[]> {
  return this.http.get<SlaAlert[]>(`${API_URL}/sla/alerts`);
}

resolveSlaAlert(reclamationId: number): Observable<void> {
  return this.http.put<void>(`${API_URL}/sla/alerts/${reclamationId}/resolve`, {});
}

// ── Analytics ─────────────────────────────────────────────────────────────────
getWeeklyVolume(): Observable<{label: string, count: number}[]> {
  return this.http.get<any[]>(`${API_URL}/reclamations/analytics/weekly`);
}

getResolutionTimeByType(): Observable<{type: string, avgHours: number}[]> {
  return this.http.get<any[]>(`${API_URL}/reclamations/analytics/resolution-time`);
}

getByStatus(): Observable<{[key: string]: number}> {
  return this.http.get<any>(`${API_URL}/reclamations/analytics/by-status`);
}

getByType(): Observable<{type: string, count: number}[]> {
  return this.http.get<any[]>(`${API_URL}/reclamations/analytics/by-type`);
}

getByPriority(): Observable<{priority: number, count: number}[]> {
  return this.http.get<any[]>(`${API_URL}/reclamations/analytics/by-priority`);
}
// ── Traduction DeepL ──────────────────────────────────────────────
translateText(text: string, targetLang: string = 'français'): Observable<{
  translatedText: string;
  detectedLanguage: string;
  languageName: string;
  languageFlag: string;
  originalText: string;
}> {
  return this.http.post<any>(
    `${API_URL}/translation/translate`,
    { text, targetLang }
  );
}

detectLanguage(text: string): Observable<{
  detectedLanguage: string;
  isFrench: boolean;
}> {
  return this.http.post<any>(
    `${API_URL}/translation/detect`,
    { text }
  );
}
addReaction(responseId: number, emoji: string): Observable<ReclamationResponse> {
  return this.http.post<ReclamationResponse>(
    `${API_URL}/responses/${responseId}/react`,
    { emoji }
  );
}

createResponseWithQuote(
  reclamationId: number,
  senderId: string | number,
  responseText: string,
  isInternal = false,
  senderType = 'ADMIN',
  quotedText?: string,
  quotedAuthor?: string
): Observable<ReclamationResponse> {
  return this.http.post<ReclamationResponse>(
    `${API_URL}/reclamations/${reclamationId}/responses`,
    { 
      responseText, 
      learnerId: senderId, 
      isInternal, 
      senderType,
      quotedText,        // ✅ ajouté
      quotedAuthor       // ✅ ajouté
    }
  );
}

// Dans reclamation.service.ts
createExternalTicket(reclamationId: number, tool: string = 'jira'): Observable<{
  ticketId: string;
  ticketUrl: string;
  toolName: string;
  alreadyExists: boolean;
}> {
  return this.http.post<any>(`${API_URL}/admin/reclamations/${reclamationId}/create-ticket`, { tool });
}

getTicketInfo(reclamationId: number): Observable<{
  exists: boolean;
  ticketId?: string;
  ticketUrl?: string;
  toolName?: string;
}> {
  return this.http.get<any>(`${API_URL}/admin/reclamations/${reclamationId}/ticket`);
}

// Approuver une réclamation suspecte
approveReclamation(reclamationId: number): Observable<Reclamation> {
  return this.http.post<Reclamation>(`${API_URL}/reclamations/${reclamationId}/approve`, {});
}

// Rejeter une réclamation suspecte
rejectReclamation(reclamationId: number): Observable<Reclamation> {
  return this.http.post<Reclamation>(`${API_URL}/reclamations/${reclamationId}/reject`, {});
}

// Obtenir toutes les réclamations suspectes
getSuspectReclamations(): Observable<Reclamation[]> {
  return this.http.get<Reclamation[]>(`${API_URL}/reclamations/suspect`);
}

detectReclamationType(subject: string, description: string): Observable<{
  hasSuggestion: boolean;
  suggestedType?: string;
  suggestedLabel?: string;
  confidence?: number;
  matchedKeywords?: string[];
}> {
  return this.http.post<any>(`${API_URL}/reclamations/detect-type`, {
    subject, description
  });
}

extractInformation(text: string): Observable<{
  hasData: boolean;
  amount?: number;
  transactionId?: string;
  errorCode?: string;
  email?: string;
  extractedDate?: string;
  invoiceNumber?: string;
}> {
  return this.http.post<any>(`${API_URL}/reclamations/extract`, { text });
}
}