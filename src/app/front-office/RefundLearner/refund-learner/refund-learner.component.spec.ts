import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { RefundLearnerComponent } from './refund-learner.component';
import { CartService } from '../../services/cart.service';
import { UserService } from '../../services/user.service';

// ───────── MOCK USER ─────────
const mockUser = {
  id: 'user-123',
  username: 'ines',
  email: 'ines@test.com',
  fullName: 'Ines Test',
  roles: ['LEARNER']
};

// ───────── MOCK INVOICE ─────────
const mockInvoice = {
  id: 1,
  invoiceNumber: 'INV-001',
  learnerId: 'user-123',
  paymentId: 'PAY-1',
  currency: 'TND',
  status: 'PAID',
  totalAmount: 200,
  issueDate: new Date().toISOString(),
  purchasedCourses: ['Angular']
};

// ───────── MOCK REFUND ─────────
const mockRefund = {
  id: 10,
  invoiceId: 1,
  status: 'PENDING',
  reason: 'test refund'
};

// ───────── CART SERVICE MOCK ─────────
const mockCartService = {
  getAllInvoices: jasmine.createSpy().and.returnValue(of([mockInvoice])),
  getInvoicesByLearner: jasmine.createSpy().and.returnValue(of([])),
  getDirectInvoices: jasmine.createSpy().and.returnValue(of([])),
  getInstallmentInvoices: jasmine.createSpy().and.returnValue(of([])),

  getMyRefunds: jasmine.createSpy().and.returnValue(of([mockRefund])),

  requestRefund: jasmine.createSpy().and.returnValue(of(mockRefund))
};

// ───────── USER SERVICE MOCK ─────────
const mockUserService = {
  getUser: jasmine.createSpy().and.returnValue(mockUser),
  loadUser: jasmine.createSpy().and.returnValue(Promise.resolve(mockUser))
};

// ───────── ROUTER MOCK ─────────
const mockRouter = {
  navigate: jasmine.createSpy('navigate')
};

describe('RefundLearnerComponent', () => {
  let component: RefundLearnerComponent;
  let fixture: ComponentFixture<RefundLearnerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [RefundLearnerComponent],
      providers: [
        { provide: CartService, useValue: mockCartService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RefundLearnerComponent);
    component = fixture.componentInstance;
  });

  // ───────── INIT ─────────
  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger user + invoices + refunds', fakeAsync(async () => {
    await component.ngOnInit();
    tick();

    expect(component.currentUser?.id).toBe('user-123');
    expect(mockCartService.getAllInvoices).toHaveBeenCalled();
    expect(mockCartService.getMyRefunds).toHaveBeenCalled();
  }));

  // ───────── INVOICES ─────────
  it('devrait charger factures sans erreur', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(component.invoices.length).toBeGreaterThanOrEqual(0);
    expect(component.loadingInvoices).toBeFalse();
  }));

  // ───────── REFUNDS ─────────
  it('devrait charger refunds', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(component.myRefunds.length).toBe(1);
  }));

  // ───────── MODAL ─────────
  it('devrait ouvrir modal refund', () => {
    component.openRefundModal(mockInvoice as any);

    expect(component.showRefundModal).toBeTrue();
    expect(component.selectedInvoice).toBeTruthy();
  });

  it('devrait fermer modal', () => {
    component.closeModal();

    expect(component.showRefundModal).toBeFalse();
    expect(component.selectedInvoice).toBeNull();
  });

  // ───────── REFUND SUBMIT ─────────
  it('devrait envoyer demande refund', () => {
    component.currentUser = mockUser as any;
    component.selectedInvoice = mockInvoice as any;
    component.refundReason = 'test reason';

    component.submitRefundRequest();

    expect(mockCartService.requestRefund).toHaveBeenCalled();
    expect(component.showRefundModal).toBeFalse();
  });

  it('ne doit rien envoyer si raison vide', () => {
  const spy = mockCartService.requestRefund;

  spy.calls.reset(); // 🔥 IMPORTANT

  component.currentUser = mockUser as any;
  component.selectedInvoice = mockInvoice as any;
  component.refundReason = '   ';

  component.submitRefundRequest();

  expect(spy).not.toHaveBeenCalled();
});

  // ───────── HELPERS ─────────
  it('devrait détecter deadline expirée', () => {
    const past = new Date();
    past.setDate(past.getDate() - 20);

    expect(component.isDeadlineExpired(past.toISOString())).toBeTrue();
  });

  it('devrait retourner deadline', () => {
    const d = component.getDeadline(new Date().toISOString());
    expect(d).toBeTruthy();
  });

  it('devrait détecter refund existant', () => {
    component.myRefunds = [mockRefund as any];

    expect(component.hasExistingRefund(1)).toBeTrue();
  });

  // ───────── NAVIGATION ─────────
  it('devrait revenir success page', () => {
    component.goBack();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/cart/success']);
  });

  // ───────── PENDING COUNT ─────────
  it('devrait compter refunds pending', () => {
    component.myRefunds = [
      { ...mockRefund, status: 'PENDING' } as any,
      { ...mockRefund, id: 2, status: 'APPROVED' } as any
    ];

    expect(component.pendingCount).toBe(1);
  });
});