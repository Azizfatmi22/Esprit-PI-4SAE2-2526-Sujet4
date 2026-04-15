import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../services/session.service';
import { PlanningService } from '../../services/planning.service';
import { Session, SessionStatus } from '../../models/session';
import { Planning } from '../../models/planning';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-session-list',
  templateUrl: './session-list.component.html',
  styleUrls: ['./session-list.component.scss']
})
export class SessionListComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;

  sessions: Session[] = [];
  filteredSessions: Session[] = [];

  // Lazy-loaded plannings
  planningsCache: Map<number, Planning | null> = new Map();

  loadingPlannings: boolean = false;

  // Search & filter
  searchText: string = '';
  statusFilter: string = '';
  participantsMin: number | null = null;
  participantsMax: number | null = null;
  dateRange: string = 'all';
  dateFrom: string | null = null;
  dateTo: string | null = null;
  sortBy: string = 'date';
  sortOrder: 'asc' | 'desc' = 'desc';

  // UI state
  showAdvancedFilters: boolean = false;
  searchTimeout: any;

  // Expose enum to template
  SessionStatus = SessionStatus;

  statusOptions = [
    { value: SessionStatus.PLANNED, label: 'Planifiée', class: 'planned' },
    { value: SessionStatus.ONGOING, label: 'En cours', class: 'progress' },
    { value: SessionStatus.COMPLETED, label: 'Terminée', class: 'completed' },
    { value: SessionStatus.CANCELLED, label: 'Annulée', class: 'cancelled' }
  ];

  message: { show: boolean; type: 'success' | 'error'; text: string } = {
    show: false,
    type: 'success',
    text: ''
  };

  userId?: string;
  courseId?: number;
  
  constructor(
    private sessionService: SessionService,
    private planningService: PlanningService,
    private authService: UserService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const user = this.authService.getUser();
    
    if (!user || !user.id) {
      console.error('User not found or ID missing');
      this.showMessage('Utilisateur non connecté', 'error');
      return;
    }
    
    this.userId = user.id;
    
    // Update session statuses first
    this.sessionService.updateAllSessionsStatus().subscribe({
      next: () => {
        console.log('Session statuses updated successfully');
      },
      error: (err) => {
        console.error('Error updating session statuses:', err);
      }
    });
    
    // Get courseId from query params and load sessions
    this.route.queryParams.subscribe(params => {
      this.courseId = params['courseId'] ? Number(params['courseId']) : undefined;

      if (!this.courseId) {
        console.warn('courseId is missing in URL');
      }

      // ✅ Load sessions AFTER getting both userId and courseId
      this.loadSessions();
    });
  }

  // Load sessions safely
  loadSessions(): void {
    if (!this.userId || !this.courseId) {
      console.warn('User ID or Course ID is missing, cannot load sessions.', {
        userId: this.userId,
        courseId: this.courseId
      });
      this.sessions = [];
      this.filteredSessions = [];
      return;
    }

    this.sessionService.getSessionsByUserAndCourse(this.userId, this.courseId).subscribe({
      next: (data) => {
        this.sessions = data || [];
        this.applyFilters();
      },
      error: (error) => {
        this.showMessage('Erreur lors du chargement des sessions', 'error');
        console.error('Error loading sessions:', error);
        this.sessions = [];
        this.filteredSessions = [];
      }
    });
  }

  // Lazy check if session has a planning
  hasPlanning(sessionId: number | null | undefined): boolean {
    return sessionId != null && this.planningsCache.has(sessionId) && !!this.planningsCache.get(sessionId);
  }

  // Lazy load planning for a session
  loadPlanning(sessionId: number | null | undefined): void {
    if (!sessionId || this.planningsCache.has(sessionId)) return;

    this.planningService.getPlanningsBySession(sessionId).subscribe({
      next: (plannings) => {
        this.planningsCache.set(sessionId, plannings[0] || null);
      },
      error: (err) => {
        console.error(`Error loading planning for session ${sessionId}:`, err);
        this.planningsCache.set(sessionId, null);
      }
    });
  }

  // Get planning object for a session
  getPlanningBySession(sessionId: number): Planning | undefined {
    return this.planningsCache.get(sessionId) || undefined;
  }

  handlePlanningClick(sessionId: number): void {
    const planning = this.getPlanningBySession(sessionId);
    if (planning && planning.id) {
      this.router.navigate(['/plannings', planning.id]);
    } else {
      this.router.navigate(['/planning/add'], { queryParams: { sessionId } });
    }
  }

  onSearchInput(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.applyFilters(), 300);
  }

  applyFilters(): void {
    let filtered = [...this.sessions];

    if (this.searchText) {
      const searchLower = this.searchText.toLowerCase();
      filtered = filtered.filter(session =>
        session.id?.toString().includes(searchLower) ||
        session.status.toLowerCase().includes(searchLower) ||
        this.getStatusLabel(session.status).toLowerCase().includes(searchLower) ||
        (session.createdAt && new Date(session.createdAt).toLocaleDateString().includes(searchLower))
      );
    }

    if (this.statusFilter) {
      filtered = filtered.filter(session => session.status === this.statusFilter as SessionStatus);
    }

    if (this.participantsMin !== null) {
      filtered = filtered.filter(session => session.maxParticipants >= (this.participantsMin || 0));
    }

    if (this.participantsMax !== null) {
      filtered = filtered.filter(session => session.maxParticipants <= (this.participantsMax || Infinity));
    }

    filtered = filtered.filter(session => this.isDateInRange(session.createdAt));

    filtered.sort((a, b) => {
      let comparison = 0;
      switch (this.sortBy) {
        case 'date':
          comparison = new Date(b.createdAt || '').getTime() - new Date(a.createdAt || '').getTime();
          break;
        case 'participants':
          comparison = (b.maxParticipants || 0) - (a.maxParticipants || 0);
          break;
        case 'status':
          comparison = a.status.localeCompare(b.status);
          break;
        case 'id':
          comparison = (b.id || 0) - (a.id || 0);
          break;
      }
      return this.sortOrder === 'desc' ? comparison : -comparison;
    });

    this.filteredSessions = filtered;
  }

  isDateInRange(date: Date | undefined): boolean {
    if (!date) return true;
    const sessionDate = new Date(date).setHours(0, 0, 0, 0);
    const today = new Date().setHours(0, 0, 0, 0);

    switch (this.dateRange) {
      case 'today':
        return sessionDate === today;
      case 'week': {
        const weekAgo = new Date();
        weekAgo.setDate(weekAgo.getDate() - 7);
        return sessionDate >= weekAgo.setHours(0, 0, 0, 0);
      }
      case 'month': {
        const monthAgo = new Date();
        monthAgo.setMonth(monthAgo.getMonth() - 1);
        return sessionDate >= monthAgo.setHours(0, 0, 0, 0);
      }
      case 'custom':
        if (this.dateFrom && this.dateTo) {
          const from = new Date(this.dateFrom).setHours(0, 0, 0, 0);
          const to = new Date(this.dateTo).setHours(23, 59, 59, 999);
          return sessionDate >= from && sessionDate <= to;
        }
        return true;
      default:
        return true;
    }
  }

  getDateRangeLabel(): string {
    switch (this.dateRange) {
      case 'today': return "Aujourd'hui";
      case 'week': return "Cette semaine";
      case 'month': return "Ce mois";
      case 'custom': return `${this.dateFrom || '...'} → ${this.dateTo || '...'}`;
      default: return '';
    }
  }

  hasActiveFilters(): boolean {
    return !!(this.searchText || this.statusFilter || this.participantsMin !== null || this.participantsMax !== null || this.dateRange !== 'all');
  }

  resetFilters(): void {
    this.searchText = '';
    this.statusFilter = '';
    this.participantsMin = null;
    this.participantsMax = null;
    this.dateRange = 'all';
    this.dateFrom = null;
    this.dateTo = null;
    this.sortBy = 'date';
    this.sortOrder = 'desc';
    this.applyFilters();
    setTimeout(() => this.searchInput?.nativeElement.focus(), 100);
  }

  clearSearch(): void {
    this.searchText = '';
    this.applyFilters();
  }

  toggleSortOrder(): void {
    this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    this.applyFilters();
  }

  getCountByStatus(status: SessionStatus): number {
    return this.sessions.filter(s => s.status === status).length;
  }

  getStatusClass(status: SessionStatus): string {
    const classes: Record<SessionStatus, string> = {
      [SessionStatus.PLANNED]: 'status-planned',
      [SessionStatus.ONGOING]: 'status-progress',
      [SessionStatus.COMPLETED]: 'status-completed',
      [SessionStatus.CANCELLED]: 'status-cancelled'
    };
    return classes[status] || '';
  }

  getStatusLabel(status: SessionStatus): string {
    const labels: Record<SessionStatus, string> = {
      [SessionStatus.PLANNED]: 'Planifiée',
      [SessionStatus.ONGOING]: 'En cours',
      [SessionStatus.COMPLETED]: 'Terminée',
      [SessionStatus.CANCELLED]: 'Annulée'
    };
    return labels[status] || status;
  }

  deleteSession(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette session ?')) {
      this.sessionService.deleteSession(id).subscribe({
        next: () => {
          this.showMessage('Session supprimée avec succès', 'success');
          this.loadSessions();
        },
        error: (error) => {
          this.showMessage('Erreur lors de la suppression', 'error');
          console.error('Error deleting session:', error);
        }
      });
    }
  }

  showMessage(text: string, type: 'success' | 'error'): void {
    this.message = { show: true, type, text };
    if (type === 'success') {
      setTimeout(() => {
        this.message.show = false;
      }, 3000);
    }
  }
}