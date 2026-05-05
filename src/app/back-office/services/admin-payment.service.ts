import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


const API_URL = 'http://localhost:8085/msenrollment';

export interface Payment {
  id: number;
  learnerId: number;
  amount: number;
  method: string;
  status: string;
  paymentDate: string;
  transactionId: string;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  learnerId: number;
  paymentId: number;
  issueDate: string;
  totalAmount: number;
  currency: string;
  purchasedCourses: string[];
  billingAddress?: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminPaymentService {

  constructor(private http: HttpClient) { }

  // Récupérer tous les paiements
  getAllPayments(): Observable<Payment[]> {
    // Note: Le backend n'a pas encore d'endpoint pour tous les paiements
    // Pour l'instant, on peut récupérer par learnerId
    // TODO: Ajouter un endpoint GET /payments/all dans le backend
    return this.http.get<Payment[]>(`${API_URL}/payments/all`);
  }

  // Récupérer les paiements d'un apprenant spécifique
  getPaymentsByLearner(learnerId: number): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${API_URL}/payments/learner/${learnerId}`);
  }

  // Récupérer toutes les factures
  getAllInvoices(): Observable<Invoice[]> {
    // Note: Le backend n'a pas encore d'endpoint pour toutes les factures
    // Pour l'instant, on peut récupérer par learnerId
    // TODO: Ajouter un endpoint GET /invoices/all dans le backend
    return this.http.get<Invoice[]>(`${API_URL}/invoices/all`);
  }

  // Récupérer les factures d'un apprenant spécifique
  getInvoicesByLearner(learnerId: number): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${API_URL}/invoices/learner/${learnerId}`);
  }

  // Récupérer une facture par son numéro
  getInvoiceByNumber(invoiceNumber: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${API_URL}/invoices/number/${invoiceNumber}`);
  }
  getDirectInvoices(): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/direct`);
}

getInstallmentInvoices(): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/installment`);
}
deleteInvoice(id: number): Observable<string> {
  return this.http.delete(`${API_URL}/invoices/${id}`, { responseType: 'text' });
}
deletePayment(id: number): Observable<string> {
  return this.http.delete(`${API_URL}/payments/${id}`, { responseType: 'text' });
}
}
