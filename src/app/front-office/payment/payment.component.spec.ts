/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PaymentComponent } from './payment.component';
import { CartService } from '../services/cart.service';
import { UserService } from '../services/user.service';

// ───────── MOCKS ─────────

const mockUser = {
  id: 'user-123',
  username: 'ines',
  email: 'ines@test.com',
  fullName: 'Ines Test',
  roles: ['LEARNER']
};
const mockCart = {
  learnerId: 'user-123',
  items: [{ id: 1 }]
};

const mockCartService = {
  getCart: jasmine.createSpy().and.returnValue(of(mockCart)),
  getCartTotal: jasmine.createSpy().and.returnValue(200),

  validateCoupon: jasmine.createSpy().and.returnValue(
    of({ valid: true, finalAmount: 160 })
  ),

  confirmPayment: jasmine.createSpy().and.returnValue(
    of({ invoiceNumber: 'INV-001' })
  ),

  checkWafaBalance: jasmine.createSpy().and.returnValue(
    of({ balance: 500, sufficient: true })
  ),

  payWithWafa: jasmine.createSpy().and.returnValue(
    of({ invoiceNumber: 'WAFA-001' })
  ),

  generateBakchichCode: jasmine.createSpy().and.returnValue(
    of({
      paymentCode: 'BC-123',
      qrCodeData: 'QR',
      expiresAt: '2026-01-01',
      amount: 200
    })
  ),

  initiateFlouciPayment: jasmine.createSpy().and.returnValue(
    of({ transactionRef: 'TR-123', otpSent: true })
  ),

  verifyFlouciOtp: jasmine.createSpy().and.returnValue(
    of({ invoiceNumber: 'FL-001', paymentStatus: 'SUCCESS' })
  ),

  resendFlouciOtp: jasmine.createSpy().and.returnValue(of({}))
};

const mockUserService = {
  getUser: jasmine.createSpy().and.returnValue(mockUser)
};

const mockRouter = {
  navigate: jasmine.createSpy('navigate')
};

const mockActivatedRoute = {};

describe('PaymentComponent', () => {
  let component: PaymentComponent;
  let fixture: ComponentFixture<PaymentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [PaymentComponent],
      providers: [
        { provide: CartService, useValue: mockCartService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentComponent);
    component = fixture.componentInstance;

    // ✅ IMPORTANT FIX GLOBAL STATE
    component.Currentuser = mockUser;
    component.totalAmount = 200;
  });

  // ───────── INIT ─────────

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger user et panier', () => {
    component.ngOnInit();
    expect(mockCartService.getCart).toHaveBeenCalledWith('user-123');
  });

  // ───────── COUPON ─────────

  it('devrait appliquer coupon', () => {
    component.couponCode = 'PROMO';

    component.applyCoupon();

    expect(mockCartService.validateCoupon).toHaveBeenCalledWith(
      'PROMO',
      'user-123',
      200
    );

    expect(component.couponResult.valid).toBeTrue();
  });

  it('devrait retirer coupon', () => {
    component.couponResult = { valid: true } as any;

    component.removeCoupon();

    expect(component.couponResult).toBeNull();
  });

  it('final amount avec coupon', () => {
    component.couponResult = { valid: true, finalAmount: 150 } as any;
    expect(component.finalAmount).toBe(150);
  });

  // ───────── FLOUCI ─────────

  it('devrait initier Flouci OTP', () => {
    component.selectedMethod = 'FLOUCI';
    component.tunisianPaymentForm.phoneNumber = '12345678';

    component.initiateFlouciPayment();

    expect(mockCartService.initiateFlouciPayment).toHaveBeenCalledWith(
      'user-123',
      '12345678',
      200
    );

    expect(component.showOtpScreen).toBeTrue();
  });

  it('devrait vérifier OTP', () => {
    component.transactionRef = 'TR-123';
    component.otpCode = '123456';

    component.confirmOtp();

    expect(mockCartService.verifyFlouciOtp).toHaveBeenCalledWith(
      'user-123',
      'TR-123',
      '123456',
      undefined
    );
  });

  it('devrait renvoyer OTP', () => {
    component.transactionRef = 'TR-123';

    component.resendOtp();

    expect(mockCartService.resendFlouciOtp).toHaveBeenCalledWith('TR-123');
  });

  // ───────── WAFA ─────────

  it('devrait vérifier solde Wafa', () => {
    component.selectedMethod = 'WAFA_CASH';
    component.tunisianPaymentForm.phoneNumber = '12345678';

    component.processWafaPayment();

    expect(mockCartService.checkWafaBalance).toHaveBeenCalledWith(
      '12345678',
      200
    );
  });

  it('devrait payer Wafa si solde suffisant', () => {
    component.selectedMethod = 'WAFA_CASH';
    component.tunisianPaymentForm.phoneNumber = '12345678';

    component.processWafaPayment();

    expect(mockCartService.payWithWafa).toHaveBeenCalledWith(
      'user-123',
      '12345678',
      200
    );
  });

  // ───────── BAKCHICH ─────────

  it('devrait générer code Bakchich', () => {
    component.selectedMethod = 'BAKCHICH';
    component.tunisianPaymentForm.phoneNumber = '12345678';

    component.processBakchichPayment();

    expect(mockCartService.generateBakchichCode).toHaveBeenCalledWith(
      'user-123',
      '12345678',
      200
    );

    expect(component.showBakchichScreen).toBeTrue();
  });

  // ───────── HELPERS ─────────

  it('valid phone', () => {
    component.tunisianPaymentForm.phoneNumber = '12345678';
    expect(component.isTunisianPaymentFormValid()).toBeTrue();
  });

  it('timer format', () => {
    component.otpTimer = 125;
    expect(component.timerDisplay).toBe('2:05');
  });

  // ───────── NAVIGATION ─────────

  it('go back', () => {
    component.goBack();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/cart']);
  });

  it('installment', () => {
    component.goToInstallment();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/cart/installment']);
  });
});