import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdminInstallmentsComponent } from './admin-installments.component';
import { AdminInstallmentService } from '../../services/admin-installment.service';
import { CartService } from '../../../front-office/services/cart.service';
import { AdminPaymentService } from '../../services/admin-payment.service';

// ================= MOCK DATA =================
const mockPlans: any[] = [
  {
    planId: 1,
    learnerId: 1001,
    totalAmount: 100,
    amountWithFees: 120,
    feePercentage: 20,
    numberOfInstallments: 2,
    installmentAmount: 60,
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
    installments: [
      { installmentNumber: 1, amount: 60, dueDate: new Date(), status: 'PAID' },
      { installmentNumber: 2, amount: 60, dueDate: new Date(), status: 'PENDING' }
    ]
  },
  {
    planId: 2,
    learnerId: 1002,
    totalAmount: 200,
    amountWithFees: 240,
    feePercentage: 20,
    numberOfInstallments: 2,
    installmentAmount: 120,
    status: 'COMPLETED',
    createdAt: new Date().toISOString(),
    installments: []
  }
];

const mockRefunds: any[] = [
  {
    id: 1,
    planId: 1,
    status: 'PENDING',
    requestDate: new Date().toISOString()
  }
];

const mockInvoices: any[] = [
  {
    id: 1,
    invoiceNumber: 'INV-1',
    learnerId: 1001,
    installmentPlanId: 1,
    totalAmount: 100,
    currency: 'TND',
    status: 'PAID',
    issueDate: new Date().toISOString(),
    purchasedCourses: ['Angular']
  }
];

// ================= MOCK SERVICES =================
const mockInstallmentService = {
  getAllPlans: jasmine.createSpy().and.returnValue(of(mockPlans)),
  getAllPlansByLearners: jasmine.createSpy().and.returnValue(of(mockPlans)),
  checkOverdue: jasmine.createSpy().and.returnValue(of({})),
  deletePlan: jasmine.createSpy().and.returnValue(of({})),

  getAllInstallmentRefunds: jasmine.createSpy().and.returnValue(of(mockRefunds)),
  approveInstallmentRefund: jasmine.createSpy().and.returnValue(of(mockRefunds[0])),
  rejectInstallmentRefund: jasmine.createSpy().and.returnValue(of(mockRefunds[0]))
};

const mockCartService = {
  getBakchichPendingInstallment: jasmine.createSpy().and.returnValue(of([])),
  confirmBakchichPayment: jasmine.createSpy().and.returnValue(of({})),
  cancelBakchichPayment: jasmine.createSpy().and.returnValue(of({}))
};

const mockPaymentService = {
  getInstallmentInvoices: jasmine.createSpy().and.returnValue(of(mockInvoices)),
  deleteInvoice: jasmine.createSpy().and.returnValue(of({}))
};

// ================= UTILS =================
function flushTimers() {
  tick(5000); // couvre setTimeout(4000)
}

// ================= TEST =================
describe('AdminInstallmentsComponent', () => {
  let component: AdminInstallmentsComponent;
  let fixture: ComponentFixture<AdminInstallmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AdminInstallmentsComponent],
      providers: [
        { provide: AdminInstallmentService, useValue: mockInstallmentService },
        { provide: CartService, useValue: mockCartService },
        { provide: AdminPaymentService, useValue: mockPaymentService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminInstallmentsComponent);
    component = fixture.componentInstance;
  });

  // ================= INIT =================
  it('devrait charger les plans', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(component.plans.length).toBe(2);
    expect(component.invoicesInstallment.length).toBe(1);
  }));

  // ================= DELETE PLAN =================
  it('devrait supprimer plan', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.plans = [...mockPlans];

    component.deletePlan(mockPlans[0]);

    tick();        // API
    flushTimers(); // 🔥 supprime timer

    expect(mockInstallmentService.deletePlan).toHaveBeenCalled();
  }));

  // ================= APPROVE REFUND =================
  it('devrait approuver refund', fakeAsync(() => {
    component.allInstallmentRefunds = [...mockRefunds];
    component.selectedRefund = mockRefunds[0];

    component.confirmInstallmentApprove();

    tick();        // API observable
    flushTimers(); // 🔥 supprime setTimeout

    expect(mockInstallmentService.approveInstallmentRefund).toHaveBeenCalled();
  }));

  // ================= STATS =================
  it('devrait calculer stats', () => {
    component.plans = mockPlans;

    expect(component.getTotalRevenue()).toBe(360);
    expect(component.getActivePlans()).toBe(1);
    expect(component.getCompletedPlans()).toBe(1);
  });

  // ================= FILTER =================
  it('devrait filtrer plans', () => {
    component.plans = mockPlans;
    component.filterStatus = 'ACTIVE';

    expect(component.filteredPlans.length).toBe(1);
  });

  // ================= REFUNDS LOAD =================
  it('devrait charger refunds', fakeAsync(() => {
    component.loadAllInstallmentRefunds();
    tick();

    expect(mockInstallmentService.getAllInstallmentRefunds).toHaveBeenCalled();
    expect(component.allInstallmentRefunds.length).toBe(1);
  }));

  // ================= FORMAT =================
  it('devrait formater date', () => {
    const result = component.formatDate(new Date().toISOString());
    expect(typeof result).toBe('string');
  });

  // ================= CLEANUP =================
  afterEach(fakeAsync(() => {
    flushTimers(); // 🔥 sécurité anti "timer still in queue"
  }));
});