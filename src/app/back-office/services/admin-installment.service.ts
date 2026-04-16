import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { forkJoin } from 'rxjs';                // ✅ import forkJoin
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { catchError } from 'rxjs/operators';

const API_URL = 'http://localhost:8085/msenrollment';

export interface Installment {
  id: number;
  installmentNumber: number;
  amount: number;
  dueDate: string;
  paidDate?: string;
  status: 'PENDING' | 'PAID' | 'OVERDUE' | 'FAILED';
  invoiceNumber?: string;
  transactionId?: string;
}

export interface InstallmentPlan {
  planId: number;
  learnerId: number;
  totalAmount: number;
  amountWithFees: number;
  feePercentage: number;
  numberOfInstallments: number;
  installmentAmount: number;
  status: 'ACTIVE' | 'COMPLETED' | 'DEFAULTED';
  createdAt: string;
  installments: Installment[];
}
// Ajouter ces interfaces en haut du fichier, après InstallmentPlan

export interface InstallmentRefundRequest {
  id: number;
  learnerId: number;
  planId: number;           // lié au plan échelonné
  invoiceId: number;
  refundAmount: number;
  reason: string;
  status: 'PENDING' | 'PROCESSED' | 'REJECTED';
  requestDate: string;
  processedBy?: string;
  creditNoteNumber?: string;
  rejectionReason?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminInstallmentService {

  constructor(private http: HttpClient) {}

  getAllPlans(): Observable<InstallmentPlan[]> {
    return this.http.get<InstallmentPlan[]>(`${API_URL}/installments/all`);
  }

  getPlansByLearner(learnerId: number): Observable<InstallmentPlan[]> {
    return this.http.get<InstallmentPlan[]>(
      `${API_URL}/installments/learner/${learnerId}`
    );
  }

  getAllPlansByLearners(learnerIds: number[]): Observable<InstallmentPlan[]> {
    const requests = learnerIds.map(id =>
      this.getPlansByLearner(id).pipe(catchError(() => of([] as InstallmentPlan[])))
    );
    return forkJoin(requests).pipe(
      map((results: InstallmentPlan[][]) => results.flat())
    );
  }
  getPlanById(planId: number): Observable<InstallmentPlan> {
    return this.http.get<InstallmentPlan>(
      `${API_URL}/installments/plan/${planId}`
    );
  }

  checkOverdue(): Observable<string> {
    return this.http.post<string>(
      `${API_URL}/installments/admin/check-overdue`, {},
      { responseType: 'text' as 'json' }
    );
  }

  checkAccess(learnerId: number, courseId: number): Observable<boolean> {
    return this.http.get<boolean>(
      `${API_URL}/installments/access/${learnerId}/${courseId}`
    );
  }
  deletePlan(planId: number): Observable<string> {
  return this.http.delete(
    `${API_URL}/installments/plan/${planId}`,
    { responseType: 'text' }
  ) as Observable<string>;
}

// Ajouter ces méthodes dans AdminInstallmentService

getAllInstallmentRefunds(): Observable<InstallmentRefundRequest[]> {
  return this.http.get<InstallmentRefundRequest[]>(
    `${API_URL}/refunds/all`   // même endpoint que les refunds normaux
  );
}

approveInstallmentRefund(refundId: number, adminName: string): Observable<InstallmentRefundRequest> {
  return this.http.put<InstallmentRefundRequest>(
    `${API_URL}/refunds/${refundId}/approve?adminName=${adminName}`, {}
  );
}

rejectInstallmentRefund(refundId: number, adminName: string, reason: string): Observable<InstallmentRefundRequest> {
  return this.http.put<InstallmentRefundRequest>(
    `${API_URL}/refunds/${refundId}/reject?adminName=${adminName}&reason=${encodeURIComponent(reason)}`, {}
  );
}
}