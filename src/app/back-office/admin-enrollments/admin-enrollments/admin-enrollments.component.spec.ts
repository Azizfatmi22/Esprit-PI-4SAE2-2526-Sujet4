import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { AdminEnrollmentsComponent } from './admin-enrollments.component';
import { AdminEnrollmentService, Enrollment } from '../../services/admin-enrollment.service';

// ───────── MOCK DATA ─────────
const baseEnrollments = (): Enrollment[] => [
  {
    id: 1,
    learnerId: 1001,
    courseId: 101,
    status: 'ACTIVE',
    progress: 50,
    enrolledDate: new Date().toISOString(),
    completedDate: undefined
  },
  {
    id: 2,
    learnerId: 1002,
    courseId: 102,
    status: 'COMPLETED',
    progress: 100,
    enrolledDate: new Date().toISOString(),
    completedDate: new Date().toISOString()
  }
];

// ───────── MOCK SERVICE ─────────
const mockService = {
  getAllEnrollments: jasmine.createSpy().and.returnValue(of(baseEnrollments())),

  updateStatus: jasmine.createSpy().and.callFake((id: number, status: string) =>
    of({
      id,
      learnerId: 1001,
      courseId: 101,
      status,
      progress: status === 'COMPLETED' ? 100 : 50,
      enrolledDate: new Date().toISOString(),
      completedDate: status === 'COMPLETED' ? new Date().toISOString() : undefined
    })
  ),

  cancelEnrollment: jasmine.createSpy().and.returnValue(of({}))
};

describe('AdminEnrollmentsComponent', () => {
  let component: AdminEnrollmentsComponent;
  let fixture: ComponentFixture<AdminEnrollmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AdminEnrollmentsComponent],
      providers: [
        { provide: AdminEnrollmentService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminEnrollmentsComponent);
    component = fixture.componentInstance;
  });

  // ───────── INIT ─────────
  it('devrait charger les enrollments', fakeAsync(() => {
    component.ngOnInit();
    tick();

    expect(component.enrollments.length).toBe(2);
    expect(component.loading).toBeFalse();
  }));

  // ───────── STATS ─────────
  it('devrait calculer active', () => {
    component.enrollments = baseEnrollments();
    expect(component.getActive()).toBe(1);
  });

  it('devrait calculer completed', () => {
    component.enrollments = baseEnrollments();
    expect(component.getCompleted()).toBe(1);
  });

  it('devrait compter par progress', () => {
    component.enrollments = baseEnrollments();
    expect(component.countByProgress(0, 60)).toBe(1);
  });

  // ───────── CANCEL (FIX TIMER ISSUE) ─────────
  it('devrait cancel enrollment', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.enrollments = baseEnrollments();

    component.cancelEnrollment(component.enrollments[0]);
    tick();

    expect(mockService.cancelEnrollment).toHaveBeenCalled();

    // 🔥 IMPORTANT: flush timeout
    tick(3000);
  }));

  // ───────── FILTER (FIX ISOLATION) ─────────
  it('devrait filtrer par status', () => {
    component.enrollments = baseEnrollments(); // NEW INSTANCE

    component.filterStatus = 'ACTIVE';

    expect(component.filteredEnrollments.length).toBe(1);
  });

  // ───────── UPDATE STATUS ─────────
  it('devrait update status', fakeAsync(() => {
    component.enrollments = baseEnrollments();

    component.updateStatus(component.enrollments[0], 'COMPLETED');
    tick();
    tick(3000);

    expect(mockService.updateStatus).toHaveBeenCalled();
  }));

  // ───────── BASIC TESTS ─────────
  it('devrait créer composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait calculer total', () => {
    component.enrollments = baseEnrollments();
    expect(component.getTotal()).toBe(2);
  });

  it('devrait calculer moyenne progress', () => {
    component.enrollments = baseEnrollments();
    expect(component.getAvgProgress()).toBe(75);
  });

  it('devrait retourner couleur status', () => {
    expect(component.getStatusColor('ACTIVE')).toBe('#10b981');
  });
});