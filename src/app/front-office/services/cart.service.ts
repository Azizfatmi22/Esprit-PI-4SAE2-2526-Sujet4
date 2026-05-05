import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { tap } from 'rxjs/operators';
import { map } from 'rxjs/operators';

const API_URL = 'http://localhost:8085/msenrollment';

export interface CartItem {
  id?: number;
  courseId: number;
  courseTitle: string;
  coursePrice: number;
}

export interface Cart {
  id?: number;
  learnerId: string;
  items: CartItem[];
}

export interface PaymentRequest {
  amount: number;
  method: string;
  phoneNumber: string;
  couponCode?: string;  // ✅ optionnel avec le "?"
}

export interface PaymentResponse {
  message: string;
  invoiceNumber: string;
  paymentStatus: string;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  learnerId: string;
  paymentId: number;
  issueDate: string;
  totalAmount: number;
  currency: string;
  purchasedCourses: string[];
  billingAddress?: string;
  status: string;
  installmentPlanId?: number | null;

  couponCode?: string;
  discountAmount?: number;
  originalAmount?: number;
}

export interface InstallmentPlanRequest {
  numberOfInstallments: 3 | 6;
  paymentMethod: string;
  phoneNumber?: string;
  totalAmount: number;
  courseTitles?: string[];
}

export interface InstallmentDTO {
  id: number;
  installmentNumber: number;
  amount: number;
  dueDate: string;
  paidDate?: string;
  status: 'PENDING' | 'PAID' | 'OVERDUE' | 'FAILED';
  invoiceNumber?: string;
}

export interface InstallmentPlanResponse {
  planId: number;
  totalAmount: number;
  amountWithFees: number;
  feePercentage: number;
  numberOfInstallments: number;
  installmentAmount: number;
  status: 'ACTIVE' | 'COMPLETED' | 'DEFAULTED';
  installments: InstallmentDTO[];
}

export interface InvoiceEmailRequest {
  invoiceNumber: string;
  email: string;
  invoiceId?: number;
  learnerId?: string;
}
export interface RefundRequest {
  id: number;
  learnerId: string;
  invoiceId: number;
  reason: string;
  refundAmount: number;
  status: 'PENDING' | 'PROCESSED' | 'REJECTED';
  requestDate: string;
  processedDate?: string;
  processedBy?: string;
  rejectionReason?: string;
  creditNoteNumber?: string;
  planId: number|null;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartSubject = new BehaviorSubject<Cart | null>(null);
  public cart$ = this.cartSubject.asObservable();

  constructor(private http: HttpClient) {}

  getCart(learnerId: string): Observable<Cart> {
    return this.http.get<Cart>(`${API_URL}/cart/${learnerId}`).pipe(
      tap(cart => {
        if (!cart || !cart.items) {
          cart = { learnerId, items: [] };
        }
        this.cartSubject.next(cart);
      })
    );
  }

  addCourseToCart(learnerId: string, courseId: number, courseTitle: string, coursePrice: number): Observable<Cart> {
    const requestBody = { courseId, courseTitle, coursePrice };
    return this.http.post<Cart>(`${API_URL}/cart/${learnerId}/add`, requestBody).pipe(
      tap(cart => this.cartSubject.next(cart))
    );
  }

  removeItemFromCart(learnerId: string, itemId: number): Observable<string> {
    return this.http.delete<string>(`${API_URL}/cart/${learnerId}/remove/${itemId}`, { responseType: 'text' as 'json' });
  }

  confirmPayment(learnerId: string, paymentRequest: PaymentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${API_URL}/payment/confirm/${learnerId}`, paymentRequest).pipe(
      tap(() => this.cartSubject.next(null))
    );
  }

  getInvoicesByLearner(learnerId: string): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${API_URL}/invoices/learner/${learnerId}`);
  }

  getInvoiceByNumber(invoiceNumber: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${API_URL}/invoices/number/${invoiceNumber}`);
  }

  sendInvoiceToEmail(request: InvoiceEmailRequest): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${API_URL}/invoices/send-email`, request, { observe: 'response' }).pipe(
      catchError((firstError) =>
        this.http.post<any>(`${API_URL}/invoices/${request.invoiceNumber}/send-email`, request, { observe: 'response' }).pipe(
          catchError(() => throwError(() => firstError))
        )
      )
    );
  }

  getCartTotal(cart: Cart | null): number {
    if (!cart || !cart.items) return 0;
    return cart.items.reduce((total, item) => total + item.coursePrice, 0);
  }

  // ===== INSTALLMENT =====

  createInstallmentPlan(learnerId: string, request: InstallmentPlanRequest): Observable<InstallmentPlanResponse> {
    return this.http.post<InstallmentPlanResponse>(`${API_URL}/installments/create/${learnerId}`, request);
  }

  payInstallment(installmentId: number, paymentMethod: string, phoneNumber: string): Observable<InstallmentDTO> {
    return this.http.post<InstallmentDTO>(`${API_URL}/installments/pay/${installmentId}`, { paymentMethod, phoneNumber });
  }

  getInstallmentPlans(learnerId: string): Observable<InstallmentPlanResponse[]> {
    return this.http.get<InstallmentPlanResponse[]>(`${API_URL}/installments/learner/${learnerId}`);
  }

  checkCourseAccess(learnerId: string, courseId: number): Observable<boolean> {
    return this.http.get<boolean>(`${API_URL}/installments/access/${learnerId}/${courseId}`);
  }

  // ===== FLOUCI =====

  initiateFlouciPayment(learnerId: string, phoneNumber: string, amount: number): Observable<any> {
    return this.http.post<any>(`${API_URL}/flouci/initiate/${learnerId}`, { phoneNumber, amount });
  }

  verifyFlouciOtp(learnerId: string, transactionRef: string, otp: string, couponCode?: string): Observable<PaymentResponse> {
    const payload = {
      learnerId,
      transactionRef,
      transactionId: transactionRef,
      reference: transactionRef,
      otp,
      otpCode: otp,
      code: otp,
      couponCode
    };

    return this.http.post(`${API_URL}/flouci/verify-otp`, payload, { responseType: 'text' }).pipe(
      map((rawResponse: string) => {
        if (!rawResponse) {
          return {
            message: 'Réponse vide du serveur.',
            invoiceNumber: '',
            paymentStatus: 'UNKNOWN'
          } as PaymentResponse;
        }

        try {
          return JSON.parse(rawResponse) as PaymentResponse;
        } catch {
          return {
            message: rawResponse,
            invoiceNumber: '',
            paymentStatus: 'UNKNOWN'
          } as PaymentResponse;
        }
      })
    );
  }

  resendFlouciOtp(transactionRef: string): Observable<any> {
    return this.http.post<any>(`${API_URL}/flouci/resend-otp`, { transactionRef });
  }

  // ===== WAFA CASH =====

  checkWafaBalance(phoneNumber: string, amount: number): Observable<any> {
    return this.http.get<any>(`${API_URL}/wafa/check-balance/${phoneNumber}?amount=${amount}`);
  }

  payWithWafa(learnerId: string, phoneNumber: string, amount: number): Observable<PaymentResponse> {
    return this.http.post(`${API_URL}/wafa/pay/${learnerId}`, { phoneNumber, amount }, { responseType: 'text' }).pipe(
      map((rawResponse: string) => {
        if (!rawResponse) {
          return {
            message: 'Réponse vide du serveur.',
            invoiceNumber: '',
            paymentStatus: 'UNKNOWN'
          } as PaymentResponse;
        }

        try {
          return JSON.parse(rawResponse) as PaymentResponse;
        } catch {
          return {
            message: rawResponse,
            invoiceNumber: '',
            paymentStatus: 'UNKNOWN'
          } as PaymentResponse;
        }
      })
    );
  }

  requestWafaRefund(learnerId: string, paymentId: number, phoneNumber: string, courseTitle: string, amount: number): Observable<any> {
    return this.http.post<any>(`${API_URL}/wafa/refund/${learnerId}`, { paymentId, phoneNumber, courseTitle, amount });
  }

  getWafaBalance(phoneNumber: string): Observable<any> {
    return this.http.get<any>(`${API_URL}/wafa/balance/${phoneNumber}`);
  }

  // ===== BAKCHICH DIRECT =====

  generateBakchichCode(learnerId: string, phoneNumber: string, amount: number): Observable<any> {
    return this.http.post<any>(`${API_URL}/bakchich/generate/${learnerId}`, { phoneNumber, amount });
  }

  getBakchichPending(): Observable<any[]> {
    return this.http.get<any[]>(`${API_URL}/bakchich/admin/pending`);
  }

  confirmBakchichPayment(bakchichId: number, confirmedBy: string): Observable<any> {
    return this.http.post<any>(`${API_URL}/bakchich/admin/confirm/${bakchichId}`, { confirmedBy });
  }

  cancelBakchichPayment(bakchichId: number): Observable<any> {
    return this.http.post<any>(`${API_URL}/bakchich/admin/cancel/${bakchichId}`, {});
  }

  // ===== BAKCHICH INSTALLMENT =====

  generateBakchichInstallmentCode(learnerId: string, phoneNumber: string, amount: number, planId: number): Observable<any> {
    return this.http.post<any>(`${API_URL}/bakchich/generate-installment/${learnerId}`, { phoneNumber, amount, planId });
  }

  getBakchichPendingInstallment(): Observable<any[]> {
    return this.http.get<any[]>(`${API_URL}/bakchich/admin/pending/installment`);
  }
  validateCoupon(code: string, learnerId: string, amount: number): Observable<any> {
  return this.http.post(`${API_URL}/coupons/validate`, {
    code, learnerId, amount
  });
}
// Toutes les factures
getAllInvoices(learnerId: string): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/learner/${learnerId}/all`);
}

// Résumé plans échelonnés
getInstallmentSummary(learnerId: string): Observable<any[]> {
  return this.http.get<any[]>(`${API_URL}/installments/learner/${learnerId}/summary`);
}
deleteInvoice(id: number): Observable<string> {
  return this.http.delete(
    `${API_URL}/invoices/${id}`,
    { responseType: 'text' }
  ) as Observable<string>;
}
deletePlan(planId: number): Observable<string> {
  return this.http.delete(
    `${API_URL}/installments/plan/${planId}`,
    { responseType: 'text' }
  ) as Observable<string>;
}
getDirectInvoices(learnerId: string): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/learner/${learnerId}/direct`);
}

getInstallmentInvoices(learnerId: string): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/learner/${learnerId}/installment`);
}

getDirectInvoicesAll(): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/direct`);
}

getInstallmentInvoicesAll(): Observable<Invoice[]> {
  return this.http.get<Invoice[]>(`${API_URL}/invoices/installment`);
}

// Dans CartService — ajouter ces méthodes
requestRefund(learnerId: string, invoiceId: number, reason: string): Observable<RefundRequest> {
  return this.http.post<RefundRequest>(`${API_URL}/refunds/request`, {
    learnerId, invoiceId, reason
  });
}

getMyRefunds(learnerId: string): Observable<RefundRequest[]> {
  return this.http.get<RefundRequest[]>(`${API_URL}/refunds/learner/${learnerId}`);
}

getAllRefunds(): Observable<RefundRequest[]> {
  return this.http.get<RefundRequest[]>(`${API_URL}/refunds/all`);
}

approveRefund(refundId: number, adminName: string): Observable<RefundRequest> {
  return this.http.post<RefundRequest>(
    `${API_URL}/refunds/approve/${refundId}`,
    { adminName }
  );
}

rejectRefund(refundId: number, adminName: string, reason: string): Observable<RefundRequest> {
  return this.http.post<RefundRequest>(
    `${API_URL}/refunds/reject/${refundId}`,
    { adminName, reason }
  );
}
}