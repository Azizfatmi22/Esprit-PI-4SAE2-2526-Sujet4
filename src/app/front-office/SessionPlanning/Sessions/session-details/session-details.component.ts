import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../services/session.service';
import { PlanningService } from '../../services/planning.service';
import { Session, SessionStatus } from '../../models/session';
import { Planning, PlanningMode, PlanningModeLabels, PlanningModeIcons } from '../../models/planning';
import { LocationType, LocationTypeLabels, LocationTypeIcons, LocationTypeColors } from '../../models/location';

@Component({
  selector: 'app-session-details',
  templateUrl: './session-details.component.html',
  styleUrls: ['./session-details.component.scss']
})
export class SessionDetailsComponent implements OnInit {
  sessionId: number | null = null;
  session: Session | null = null;
  planning: Planning | null = null;
  
  loading: boolean = true;
  loadingPlanning: boolean = false;
  error: string | null = null;
  
  message: { show: boolean; type: 'success' | 'error'; text: string } = {
    show: false,
    type: 'success',
    text: ''
  };

  // Expose enums to template
  SessionStatus = SessionStatus;
  PlanningMode = PlanningMode;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private sessionService: SessionService,
    private planningService: PlanningService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.sessionId = +id;
      this.loadSessionDetails();
    } else {
      this.error = 'ID de session non trouvé';
      this.loading = false;
    }
  }

  loadSessionDetails(): void {
    this.loading = true;
    this.error = null;
    
    if (!this.sessionId) {
      this.error = 'ID de session non trouvé';
      this.loading = false;
      return;
    }

    this.sessionService.getSessionById(this.sessionId).subscribe({
      next: (data) => {
        this.session = data;
        this.loading = false;
        this.loadPlanningForSession();
      },
      error: (err) => {
        console.error('Error loading session:', err);
        this.error = 'Erreur lors du chargement de la session';
        this.loading = false;
      }
    });
  }

  loadPlanningForSession(): void {
    if (!this.sessionId) return;
    
    this.loadingPlanning = true;
    this.planningService.getPlanningsBySession(this.sessionId).subscribe({
      next: (plannings) => {
        if (plannings && plannings.length > 0) {
          this.planning = plannings[0];
          console.log('Planning loaded:', this.planning);
        }
        this.loadingPlanning = false;
      },
      error: (err) => {
        console.error('Error loading planning:', err);
        this.loadingPlanning = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/sessions']);
  }

  deleteSession(): void {
    if (!this.sessionId) return;
    
    if (confirm('Êtes-vous sûr de vouloir supprimer cette session ?')) {
      this.sessionService.deleteSession(this.sessionId).subscribe({
        next: () => {
          this.showMessage('Session supprimée avec succès', 'success');
          setTimeout(() => this.router.navigate(['/sessions']), 1500);
        },
        error: (err) => {
          console.error('Error deleting session:', err);
          this.showMessage('Erreur lors de la suppression', 'error');
        }
      });
    }
  }

  createPlanning(): void {
    if (this.sessionId) {
      this.router.navigate(['/plannings/new'], { 
        queryParams: { sessionId: this.sessionId }
      });
    }
  }

  // Status methods
  getStatusClass(status: SessionStatus): string {
    const classes = {
      [SessionStatus.PLANNED]: 'status-planned',
      [SessionStatus.ONGOING]: 'status-progress',
      [SessionStatus.COMPLETED]: 'status-completed',
      [SessionStatus.CANCELLED]: 'status-cancelled'
    };
    return classes[status] || '';
  }

  getStatusLabel(status: SessionStatus): string {
    const labels = {
      [SessionStatus.PLANNED]: 'Planifiée',
      [SessionStatus.ONGOING]: 'En cours',
      [SessionStatus.COMPLETED]: 'Terminée',
      [SessionStatus.CANCELLED]: 'Annulée'
    };
    return labels[status] || status;
  }

  getStatusIcon(status: SessionStatus): string {
    const icons = {
      [SessionStatus.PLANNED]: '📅',
      [SessionStatus.ONGOING]: '⚡',
      [SessionStatus.COMPLETED]: '✅',
      [SessionStatus.CANCELLED]: '❌'
    };
    return icons[status] || '📌';
  }

  // Planning methods
  getPlanningModeLabel(mode: PlanningMode): string {
    return PlanningModeLabels[mode] || mode;
  }

  getPlanningModeIcon(mode: PlanningMode): string {
    return PlanningModeIcons[mode] || '📅';
  }

  getPlanningModeClass(mode: PlanningMode): string {
    return mode;
  }

  getPlatformUrl(planning: any): string | null {
    return (planning as any).platformUrl || null;
  }

  // Location methods
  getLocationTypeLabel(type: LocationType): string {
    return LocationTypeLabels[type] || type;
  }

  getLocationTypeIcon(type: LocationType): string {
    return LocationTypeIcons[type] || '📍';
  }

  getLocationTypeColor(type: LocationType): string {
    return LocationTypeColors[type] || '#6366f1';
  }

  // Message
  showMessage(text: string, type: 'success' | 'error'): void {
    this.message = { show: true, type, text };
    
    if (type === 'success') {
      setTimeout(() => {
        this.message.show = false;
      }, 3000);
    }
  }
}