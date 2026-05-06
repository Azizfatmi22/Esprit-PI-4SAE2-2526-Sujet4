import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { AdminReclamationsComponent } from './admin-reclamations.component';
import { ReclamationService } from '../../front-office/services/reclamation.service';
import { AdminTypingService } from '../../front-office/services/admin-typing.service';
import { UserService } from '../../front-office/services/user.service';

// ── MOCK DATA ─────────────────────────────────────────────

const mockUser = { id: 'admin-99', username: 'admin' };

const mockReclamation = {
  id: 1,
  learnerId: 'user-123',
  type: 'TECHNICAL',
  subject: 'Test',
  description: 'Desc',
  status: 'PENDING',
  priority: 2,
  createdDate: new Date().toISOString(),
  gravityLevel: 'HIGH'
};

const mockStats = {
  total: 1,
  pending: 1,
  inProgress: 0,
  resolved: 0,
  closed: 0,
  rejected: 0,
  unresolved: 1
};

const mockReclamationService = {
  getAllReclamations: jasmine.createSpy().and.returnValue(of([mockReclamation])),
  getStatistics: jasmine.createSpy().and.returnValue(of(mockStats)),
  getSlaAlerts: jasmine.createSpy().and.returnValue(of([])),
  getReclamationResponses: jasmine.createSpy().and.returnValue(of([])),
  createResponse: jasmine.createSpy().and.returnValue(of({})),
  updateReclamationStatus: jasmine.createSpy().and.returnValue(of({ ...mockReclamation })),
  deleteReclamation: jasmine.createSpy().and.returnValue(of({})),
  approveReclamation: jasmine.createSpy().and.returnValue(of({ ...mockReclamation })),
  rejectReclamation: jasmine.createSpy().and.returnValue(of({ ...mockReclamation })),
  getAiSuggestions: jasmine.createSpy().and.returnValue(of({ suggestions: [] })),
  detectLanguage: jasmine.createSpy().and.returnValue(of({ detectedLanguage: 'FR' })),
  translateText: jasmine.createSpy().and.returnValue(of({ translatedText: 'ok' })),
  createExternalTicket: jasmine.createSpy().and.returnValue(of({
    ticketId: 'JIRA-1',
    ticketUrl: 'url',
    toolName: 'Jira'
  })),
  getTicketInfo: jasmine.createSpy().and.returnValue(of({ exists: false })),
  updateReclamation: jasmine.createSpy().and.returnValue(of(mockReclamation))
};

const mockTypingService = {
  sendTyping: jasmine.createSpy()
};

const mockUserService = {
  getUser: jasmine.createSpy().and.returnValue(mockUser)
};

// ── TEST SUITE ─────────────────────────────────────────────

describe('AdminReclamationsComponent', () => {
  let component: AdminReclamationsComponent;
  let fixture: ComponentFixture<AdminReclamationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [AdminReclamationsComponent],
      providers: [
        { provide: ReclamationService, useValue: mockReclamationService },
        { provide: AdminTypingService, useValue: mockTypingService },
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReclamationsComponent);
    component = fixture.componentInstance;

    spyOn(window, 'setInterval').and.returnValue(123 as any);
  });

  afterEach(() => {
    if (component) {
      try {
        component.ngOnDestroy();
      } catch {}
    }
  });

  // ─────────────────────────────────────────────
  // INIT
  // ─────────────────────────────────────────────

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load data on init', () => {
    fixture.detectChanges(); // triggers ngOnInit
    expect(mockReclamationService.getAllReclamations).toHaveBeenCalled();
    expect(component.reclamations.length).toBe(1);
  });

  // ─────────────────────────────────────────────
  // FILTERS
  // ─────────────────────────────────────────────

  it('should filter by status', () => {
    component.reclamations = [mockReclamation];
    component.filterStatus = 'PENDING';
    expect(component.filteredReclamations.length).toBe(1);
  });

  it('should reset filters', () => {
    component.searchTerm = 'x';
    component.resetFilters();
    expect(component.searchTerm).toBe('');
  });

  // ─────────────────────────────────────────────
  // DETAIL
  // ─────────────────────────────────────────────

  it('should open detail', () => {
    mockReclamationService.getReclamationResponses.and.returnValue(of([]));
    mockReclamationService.getAiSuggestions.and.returnValue(of({ suggestions: [] }));

    component.openDetail(mockReclamation);

    expect(component.viewMode).toBe('detail');
    expect(component.selectedReclamation?.id).toBe(1);
  });

  // ─────────────────────────────────────────────
  // STATUS
  // ─────────────────────────────────────────────

  it('should update status', () => {
    component.reclamations = [mockReclamation];
    component.updateStatus(mockReclamation, 'IN_PROGRESS');

    expect(mockReclamationService.updateReclamationStatus)
      .toHaveBeenCalledWith(1, 'IN_PROGRESS', undefined, 99);
  });

  // ─────────────────────────────────────────────
  // RESPONSE
  // ─────────────────────────────────────────────

  it('should send response', () => {
    component.selectedReclamation = mockReclamation;
    component.adminResponse = 'hello';

    component.submitResponse();

    expect(mockReclamationService.createResponse).toHaveBeenCalled();
  });

  // ─────────────────────────────────────────────
  // DELETE
  // ─────────────────────────────────────────────

  it('should delete reclamation', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.reclamations = [mockReclamation];
    component.deleteReclamation(1);

    expect(mockReclamationService.deleteReclamation).toHaveBeenCalledWith(1);
  });

  // ─────────────────────────────────────────────
  // GRAVITY SORT
  // ─────────────────────────────────────────────

  it('should toggle gravity sort', () => {
    const initial = component.sortByGravity;
    component.toggleGravitySort();
    expect(component.sortByGravity).toBe(!initial);
  });

  // ─────────────────────────────────────────────
  // NOTIFY
  // ─────────────────────────────────────────────

  it('should show notification', fakeAsync(() => {
    component.notify('ok', 'success');
    expect(component.notification?.msg).toBe('ok');

    tick(4000);
    expect(component.notification).toBeNull();
  }));
});