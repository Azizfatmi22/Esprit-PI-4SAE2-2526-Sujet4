import { ComponentFixture, TestBed } from '@angular/core/testing';
<<<<<<< HEAD

import { InstallmentSuccessComponent } from './installment-success.component';
=======
import { Router } from '@angular/router';
import { InstallmentSuccessComponent } from './installment-success.component';
import { InstallmentPlanResponse } from '../../services/cart.service';

// ───────── ROUTER MOCK ─────────
const mockRouter = {
  navigate: jasmine.createSpy('navigate')
};

// ───────── MOCK PLAN (FIX TYPE ERROR) ─────────
const mockPlan = {
  planId: 1,
  numberOfInstallments: 3,
  totalAmount: 300,
  feePercentage: 5,
  amountWithFees: 315,
  installmentAmount: 105,
  installments: [
    {
      installmentNumber: 1,
      dueDate: new Date().toISOString(),
      amount: 105,
      status: 'PAID'
    },
    {
      installmentNumber: 2,
      dueDate: new Date().toISOString(),
      amount: 105,
      status: 'PENDING'
    }
  ]
} as unknown as InstallmentPlanResponse;
>>>>>>> a2c577a (test unitaire reclamation et enrollment)

describe('InstallmentSuccessComponent', () => {
  let component: InstallmentSuccessComponent;
  let fixture: ComponentFixture<InstallmentSuccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
<<<<<<< HEAD
      declarations: [InstallmentSuccessComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstallmentSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
=======
      declarations: [InstallmentSuccessComponent],
      providers: [
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InstallmentSuccessComponent);
    component = fixture.componentInstance;
  });

  // ───────── INIT ─────────
  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger plan depuis history.state', () => {
  spyOnProperty(window.history, 'state', 'get').and.returnValue({
    plan: mockPlan
  });

  component.ngOnInit();

  expect(component.plan).toEqual(mockPlan);
});

  // ───────── FORMAT ─────────
  it('devrait formater montant correctement', () => {
  expect((component as any).formatAmount(100)).toBe('100.00 TND');
});

  // ───────── NAVIGATION ─────────
  it('devrait naviguer vers cours', () => {
    component.goToCourses();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/trainer_course/list']);
  });

  // ───────── PDF GENERATION ─────────
  it('devrait générer PDF sans erreur', () => {
    component.plan = mockPlan;

    spyOn(window as any, 'open');
    spyOn(URL, 'createObjectURL').and.returnValue('blob:url');

    expect(() => component.generateAndOpenPDF()).not.toThrow();
  });

  it('ne doit rien faire si plan null', () => {
    component.plan = null;

    expect(() => component.generateAndOpenPDF()).not.toThrow();
  });

  // ───────── DOWNLOAD PDF ─────────
  it('devrait appeler generateAndOpenPDF', () => {
    const spy = spyOn(component, 'generateAndOpenPDF');

    component.downloadPDF();

    expect(spy).toHaveBeenCalled();
  });
});
>>>>>>> a2c577a (test unitaire reclamation et enrollment)
