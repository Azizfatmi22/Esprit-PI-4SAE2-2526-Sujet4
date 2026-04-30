import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AdminPaymentsInvoicesComponent } from './admin-payments-invoices.component';
import { AdminPaymentService } from '../services/admin-payment.service';
import { CartService, RefundRequest } from '../../front-office/services/cart.service';

// =======================
// MOCK REFUND COMPLET
// =======================
const mockRefund: RefundRequest = {
  id: 1,
  learnerId: 10,
  invoiceId: 20,
  reason: 'test reason',
  refundAmount: 100,
  status: 'PENDING',
  requestDate: new Date().toISOString(),
  planId: null,
  creditNoteNumber: null
} as any;

describe('AdminPaymentsInvoicesComponent', () => {
  let component: AdminPaymentsInvoicesComponent;
  let fixture: ComponentFixture<AdminPaymentsInvoicesComponent>;

  let adminPaymentServiceSpy: jasmine.SpyObj<AdminPaymentService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;

  beforeEach(async () => {
    adminPaymentServiceSpy = jasmine.createSpyObj('AdminPaymentService', [
      'getAllPayments',
      'getPaymentsByLearner',
      'getDirectInvoices',
      'getInstallmentInvoices',
      'getAllInvoices',
      'getInvoicesByLearner',
      'deleteInvoice',
      'deletePayment'
    ]);

    cartServiceSpy = jasmine.createSpyObj('CartService', [
      'getBakchichPending',
      'confirmBakchichPayment',
      'cancelBakchichPayment',
      'getAllRefunds',
      'approveRefund',
      'rejectRefund'
    ]);

    await TestBed.configureTestingModule({
      declarations: [AdminPaymentsInvoicesComponent],
      providers: [
        { provide: AdminPaymentService, useValue: adminPaymentServiceSpy },
        { provide: CartService, useValue: cartServiceSpy }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminPaymentsInvoicesComponent);
    component = fixture.componentInstance;
  });

  // =======================
  // INIT
  // =======================
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should init load', fakeAsync(() => {
    adminPaymentServiceSpy.getAllPayments.and.returnValue(of([]));
    adminPaymentServiceSpy.getDirectInvoices.and.returnValue(of([]));
    adminPaymentServiceSpy.getInstallmentInvoices.and.returnValue(of([]));
    adminPaymentServiceSpy.getAllInvoices.and.returnValue(of([]));
    cartServiceSpy.getAllRefunds.and.returnValue(of([]));
    cartServiceSpy.getBakchichPending.and.returnValue(of([]));

    component.ngOnInit();
    tick();

    expect(adminPaymentServiceSpy.getAllPayments).toHaveBeenCalled();
    expect(cartServiceSpy.getAllRefunds).toHaveBeenCalled();
  }));

  // =======================
  // PAYMENTS
  // =======================
  it('should load payments', () => {
    adminPaymentServiceSpy.getAllPayments.and.returnValue(
      of([{ id: 1, amount: 100 } as any])
    );

    component.loadPayments();

    expect(component.payments.length).toBe(1);
  });

  it('should fallback when payment fails', () => {
    adminPaymentServiceSpy.getAllPayments.and.returnValue(
      throwError(() => new Error('error'))
    );

    adminPaymentServiceSpy.getPaymentsByLearner.and.returnValue(of([]));

    component.loadPayments();

    expect(adminPaymentServiceSpy.getPaymentsByLearner).toHaveBeenCalledWith(1);
  });

  // =======================
  // INVOICES
  // =======================
  it('should load invoices', fakeAsync(() => {
    adminPaymentServiceSpy.getDirectInvoices.and.returnValue(of([]));
    adminPaymentServiceSpy.getInstallmentInvoices.and.returnValue(of([]));
    adminPaymentServiceSpy.getAllInvoices.and.returnValue(of([]));

    component.loadInvoices();
    tick();

    expect(adminPaymentServiceSpy.getDirectInvoices).toHaveBeenCalled();
  }));

  // =======================
  // BAKCHICH
  // =======================
  it('should load bakchich', () => {
    cartServiceSpy.getBakchichPending.and.returnValue(of([]));

    component.loadBakchichPending();

    expect(component.loadingBakchich).toBeFalse();
  });

  it('should confirm bakchich', () => {
    // ✅ FIX IMPORTANT : retourner un string OU objet compatible
    cartServiceSpy.confirmBakchichPayment.and.returnValue(of('ok'));

    component.confirmBakchich(1);

    expect(cartServiceSpy.confirmBakchichPayment).toHaveBeenCalled();
  });

  // =======================
  // REFUNDS
  // =======================
  it('should load refunds', () => {
    cartServiceSpy.getAllRefunds.and.returnValue(of([mockRefund]));

    component.loadAllRefunds();

    expect(component.allRefunds.length).toBe(1);
  });

  it('should approve refund', () => {
    component.selectedRefund = mockRefund;

    cartServiceSpy.approveRefund.and.returnValue(
      of({ ...mockRefund, status: 'PROCESSED' })
    );

    component.confirmApprove();

    expect(cartServiceSpy.approveRefund).toHaveBeenCalled();
  });

  it('should reject refund', () => {
    component.selectedRefund = mockRefund;
    component.rejectionReason = 'bad request';

    cartServiceSpy.rejectRefund.and.returnValue(
      of({ ...mockRefund, status: 'REJECTED' })
    );

    component.confirmReject();

    expect(cartServiceSpy.rejectRefund).toHaveBeenCalled();
  });

  // =======================
  // DELETE
  // =======================
  it('should delete payment', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    adminPaymentServiceSpy.deletePayment.and.returnValue(of('ok' as any));

    component.deletePayment({ id: 1, amount: 100 } as any);

    expect(adminPaymentServiceSpy.deletePayment).toHaveBeenCalledWith(1);
  });

  it('should delete invoice', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    adminPaymentServiceSpy.deleteInvoice.and.returnValue(of('ok' as any));

    component.deleteInvoice({ id: 1, invoiceNumber: 'INV' } as any);

    expect(adminPaymentServiceSpy.deleteInvoice).toHaveBeenCalledWith(1);
  });

  // =======================
  // FILTER
  // =======================
  it('should filter refunds', () => {
    component.allRefunds = [
      { ...mockRefund, status: 'PENDING' },
      { ...mockRefund, id: 2, status: 'PROCESSED' }
    ];

    component.setRefundFilter('PROCESSED');

    expect(component.refundFilterStatus).toBe('PROCESSED');
  });
});