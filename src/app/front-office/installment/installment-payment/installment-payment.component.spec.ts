import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { InstallmentPaymentComponent } from './installment-payment.component';
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

// ───────── MOCK SERVICES ─────────
const mockCartService = {
  getCart: jasmine.createSpy().and.returnValue(of({
    items: [{ courseTitle: 'Angular' }]
  })),

  getCartTotal: jasmine.createSpy().and.returnValue(200),

  // FLOUCI
  initiateFlouciPayment: jasmine.createSpy().and.returnValue(
    of({ transactionRef: 'TR-123' })
  ),
  verifyFlouciOtp: jasmine.createSpy().and.returnValue(of({})),
  resendFlouciOtp: jasmine.createSpy().and.returnValue(of({})),

  // WAFA
  checkWafaBalance: jasmine.createSpy().and.returnValue(
    of({ balance: 500, sufficient: true })
  ),
  payWithWafa: jasmine.createSpy().and.returnValue(of({})),
  getWafaBalance: jasmine.createSpy().and.returnValue(
    of({ balance: 500 })
  ),

  // BAKCHICH
  createInstallmentPlan: jasmine.createSpy().and.returnValue(
    of({ planId: 1 })
  ),

  generateBakchichInstallmentCode: jasmine.createSpy().and.returnValue(
    of({
      paymentCode: 'BC-123',
      qrCodeData: 'QR',
      expiresAt: '2026-01-01',
      amount: 200
    })
  )
};

const mockUserService = {
  getUser: jasmine.createSpy().and.returnValue(mockUser),
  loadUser: jasmine.createSpy().and.returnValue(Promise.resolve(mockUser))
};

const mockRouter = {
  navigate: jasmine.createSpy('navigate')
};

describe('InstallmentPaymentComponent', () => {
  let component: InstallmentPaymentComponent;
  let fixture: ComponentFixture<InstallmentPaymentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [InstallmentPaymentComponent],
      providers: [
        { provide: CartService, useValue: mockCartService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InstallmentPaymentComponent);
    component = fixture.componentInstance;

    // 🔥 IMPORTANT FIX GLOBAL (évite tous tes erreurs)
    component.Currentuser = mockUser as any;
    component.planPreview = { totalWithFee: 100, perInstallment: 50 } as any;
    component.phoneNumber = '12345678';
    component.totalAmount = 200;
  });

  // ───────── INIT ─────────
  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger le panier', fakeAsync(async () => {
    await component.ngOnInit();
    tick();

    expect(mockCartService.getCart).toHaveBeenCalled();
  }));

  // ───────── FLOUCI ─────────
  it('devrait initier Flouci', () => {
    component.selectedMethod = 'FLOUCI';

    component.processInstallmentFlouci();

    expect(mockCartService.initiateFlouciPayment).toHaveBeenCalled();
  });

  it('devrait vérifier OTP puis créer plan', fakeAsync(() => {
    component.transactionRef = 'TR-123';
    component.otpCode = '123456';

    component.confirmOtp();

    tick();

    expect(mockCartService.verifyFlouciOtp).toHaveBeenCalled();
    expect(mockCartService.createInstallmentPlan).toHaveBeenCalled();
  }));

  it('devrait renvoyer OTP', () => {
    component.transactionRef = 'TR-123';

    component.resendOtp();

    expect(mockCartService.resendFlouciOtp).toHaveBeenCalled();
  });

  // ───────── WAFA ─────────
  it('devrait vérifier solde Wafa', () => {
    component.processInstallmentWafa();

    expect(mockCartService.checkWafaBalance).toHaveBeenCalled();
  });

  it('devrait confirmer paiement Wafa', fakeAsync(() => {
    component.confirmWafaInstallment();

    tick();

    expect(mockCartService.createInstallmentPlan).toHaveBeenCalled();
    expect(mockCartService.payWithWafa).toHaveBeenCalled();
  }));

  // ───────── BAKCHICH ─────────
  it('devrait générer code Bakchich', fakeAsync(() => {
    component.processInstallmentBakchich();

    tick();

    expect(mockCartService.createInstallmentPlan).toHaveBeenCalled();
    expect(mockCartService.generateBakchichInstallmentCode).toHaveBeenCalled();
  }));

  // ───────── VALIDATION ─────────
  it('devrait bloquer si téléphone vide', () => {
    spyOn(window, 'alert');

    component.phoneNumber = '';

    component.confirmInstallmentPayment();

    expect(window.alert).toHaveBeenCalled();
  });

  // ───────── NAVIGATION ─────────
  it('devrait revenir page payment', () => {
    component.goBack();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/cart/payment']);
  });

  // ───────── TIMER ─────────
  it('format timer correct', () => {
    component.otpTimer = 125;

    expect(component.timerDisplay).toBe('2:05');
  });
});