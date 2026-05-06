import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PaymentSuccessComponent } from './payment-success.component';
import { CartService } from '../services/cart.service';
import { UserService } from '../services/user.service';

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
  purchasedCourses: ['Angular', 'Spring']
};

// ───────── CART SERVICE MOCK ─────────
const mockCartService = {
  getAllInvoices: jasmine.createSpy().and.returnValue(of([mockInvoice])),
  getInvoiceByNumber: jasmine.createSpy().and.returnValue(of(mockInvoice)),
  sendInvoiceToEmail: jasmine.createSpy().and.returnValue(
    of({ status: 200, body: {} })
  )
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

// ───────── ROUTE MOCK ─────────
const mockActivatedRoute = {
  snapshot: {
    queryParams: {
      invoiceNumber: 'INV-001'
    }
  }
};

describe('PaymentSuccessComponent', () => {
  let component: PaymentSuccessComponent;
  let fixture: ComponentFixture<PaymentSuccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [PaymentSuccessComponent],
      providers: [
        { provide: CartService, useValue: mockCartService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentSuccessComponent);
    component = fixture.componentInstance;
  });

  // ───────── INIT ─────────
  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger facture par invoiceNumber', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(mockCartService.getInvoiceByNumber).toHaveBeenCalled();
    expect(component.invoice?.invoiceNumber).toBe('INV-001');
  }));

  it('devrait charger user', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(component.currentUser?.id).toBe('user-123');
  }));

  // ───────── NAVIGATION ─────────
  it('devrait naviguer vers courses', () => {
    component.goToCourses();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/learner/courses']);
  });

  // ───────── EMAIL ─────────
  it('devrait envoyer email facture', fakeAsync(() => {
    component.invoice = mockInvoice as any;
    component.currentUser = mockUser as any;

    component.sendInvoiceToCurrentUser();
    tick();

    expect(mockCartService.sendInvoiceToEmail).toHaveBeenCalled();
    expect(component.emailStatus).toBe('sent');
  }));

  it('devrait rejouer email', fakeAsync(() => {
    component.invoice = mockInvoice as any;
    component.currentUser = mockUser as any;

    component.resendInvoiceEmail();
    tick();

    expect(mockCartService.sendInvoiceToEmail).toHaveBeenCalled();
  }));

  // ───────── BACKUP FACTURE ─────────
  it('devrait charger facture via learner', fakeAsync(() => {
    component.currentUser = mockUser as any;

    component.loadLatestInvoiceForLearner('user-123');
    tick();

    expect(mockCartService.getAllInvoices).toHaveBeenCalled();
    expect(component.invoice?.invoiceNumber).toBe('INV-001');
  }));

  // ───────── STATUS ─────────
  it('devrait gérer absence user/email', () => {
    component.currentUser = null;
    component.invoice = null;

    component.sendInvoiceToCurrentUser();

    expect(component.emailStatus).toBe('failed');
  });
});