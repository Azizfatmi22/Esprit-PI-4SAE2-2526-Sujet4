import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ReportingService {
  private readonly reportingUrl = 'http://localhost:8085/reporting';

  constructor(private http: HttpClient) {}

  getEvaluationHistory(evaluationId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.reportingUrl}/evaluation/${evaluationId}`,
    );
  }

  updateVigilanceStatus(historyId: number, status: string): Observable<any> {
    return this.http.patch(
      `${this.reportingUrl}/history/${historyId}/status?status=${status}`,
      {},
    );
  }

  saveTemplate(template: any): Observable<any> {
    return this.http.post(
      `${this.reportingUrl}/certificates/template`,
      template,
    );
  }

  getTemplateByEvaluationId(evaluationId: number): Observable<any> {
    return this.http.get(
      `${this.reportingUrl}/certificates/template/${evaluationId}`,
    );
  }

  getHistoryByLearnerAndEvaluation(learnerId: String, evaluationId: number) {
    return this.http.get<any>(
      `${this.reportingUrl}/history/find?learnerId=${learnerId}&evaluationId=${evaluationId}`,
    );
  }

  downloadCertificate(historyId: number) {
    return this.http.get(
      `${this.reportingUrl}/certificates/download/${historyId}`,
      {
        responseType: 'blob',
      },
    );
  }

  getUserEvaluationHistory(learnerId: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.reportingUrl}/history/all?learnerId=${learnerId}`,
    );
  }

  downloadPdf(historyId: number): Observable<Blob> {
    return this.http.get(
      `${this.reportingUrl}/certificates/download/${historyId}`,
      {
        responseType: 'blob',
      },
    );
  }
}
