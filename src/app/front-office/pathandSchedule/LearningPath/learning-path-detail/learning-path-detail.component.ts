import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LearningPathService } from '../../services/learning-path.service';
import { SessionService } from '../../../SessionPlanning/services/session.service';
import { PlanningService } from '../../../SessionPlanning/services/planning.service';
import { LearningPath, LearningLevel, LearningPathStatus } from '../../models/learning-path.model';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-learning-path-detail',
  templateUrl: './learning-path-detail.component.html',
  styleUrls: ['./learning-path-detail.component.scss']
})
export class LearningPathDetailComponent implements OnInit {
  path: LearningPath | null = null;
  sessions: any[] = [];
  risks: string[] = [];
  loading: boolean = false;
  loadingSessions: boolean = false;
  showSuccess: boolean = false;
  showError: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  
  levelOptions = [
    { value: 'BEGINNER', label: 'Débutant', icon: '🌱', color: '#10b981' },
    { value: 'INTERMEDIATE', label: 'Intermédiaire', icon: '📚', color: '#f59e0b' },
    { value: 'ADVANCED', label: 'Avancé', icon: '🎓', color: '#ef4444' }
  ];
  
  statusOptions = [
    { value: 'DRAFT', label: 'Brouillon', icon: '📝', color: '#f59e0b' },
    { value: 'ACTIVE', label: 'Actif', icon: '✅', color: '#10b981' },
    { value: 'PUBLISHED', label: 'Publié', icon: '📚', color: '#6366f1' },
    { value: 'ARCHIVED', label: 'Archivé', icon: '📦', color: '#6b7280' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private learningPathService: LearningPathService,
    private sessionService: SessionService,
    private planningService: PlanningService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPath(+id);
      this.loadRisks(+id);
    }
  }

  loadPath(id: number): void {
    this.loading = true;
    this.learningPathService.getLearningPath(id).subscribe({
      next: (path) => {
        this.path = path;
        console.log('Path loaded:', path);
        console.log('Session IDs from path:', path.sessionIds);
        
        if (path.sessionIds && path.sessionIds.length > 0) {
          this.loadSessionsWithPlanning(path.sessionIds);
        } else {
          this.sessions = [];
          this.loading = false;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading path:', err);
        this.errorMessage = 'Erreur lors du chargement du parcours';
        this.showError = true;
        setTimeout(() => this.hideMessages(), 3000);
        this.loading = false;
      }
    });
  }

 loadSessionsWithPlanning(sessionIds: number[]): void {
  this.loadingSessions = true;
  console.log('Loading sessions with IDs:', sessionIds);
  
  if (!sessionIds || sessionIds.length === 0) {
    this.sessions = [];
    this.loadingSessions = false;
    return;
  }
  
  // Get each session by ID
  const sessionRequests = sessionIds.map(sessionId =>
    this.sessionService.getSessionById(sessionId).pipe(
      catchError(err => {
        console.error(`Error loading session ${sessionId}:`, err);
        return of(null);
      })
    )
  );
  
  forkJoin(sessionRequests).subscribe({
    next: (sessions) => {
      // Type guard to filter out null values
      const validSessions = sessions.filter((session): session is NonNullable<typeof session> => {
        return session !== null;
      });
      
      console.log('Loaded valid sessions:', validSessions.length);
      
      if (validSessions.length === 0) {
        this.sessions = [];
        this.loadingSessions = false;
        return;
      }
      
      // Ensure we only request planning for sessions that have a defined id
      const sessionsWithIds = validSessions.filter(
        (session): session is NonNullable<typeof session> & { id: number } => session.id != null
      );

      const planningRequests = sessionsWithIds.map(session =>
        this.planningService.getPlanningsBySession(session.id).pipe(
          catchError(err => {
            console.warn(`No planning for session ${session.id}`);
            return of([]);
          })
        )
      );
      
      forkJoin(planningRequests).subscribe({
        next: (planningsArray) => {
          this.sessions = validSessions.map((session, index) => {
            const plannings = planningsArray[index];
            const planning = plannings && plannings.length > 0 ? plannings[0] : null;
            return {
              ...session,
              planning: planning,
              totalHours: planning?.totalHours || 0,
              startDate: planning?.startDate,
              endDate: planning?.endDate,
              mode: planning?.mode,
              hasPlanning: planning !== null
            };
          });
          console.log('Final sessions with planning:', this.sessions);
          this.loadingSessions = false;
        },
        error: (err) => {
          console.error('Error loading plannings:', err);
          // Still show sessions without planning
          this.sessions = validSessions.map(session => ({
            ...session,
            planning: null,
            totalHours: 0,
            hasPlanning: false
          }));
          this.loadingSessions = false;
        }
      });
    },
    error: (err) => {
      console.error('Error loading sessions:', err);
      this.errorMessage = 'Erreur lors du chargement des sessions';
      this.showError = true;
      setTimeout(() => this.hideMessages(), 3000);
      this.loadingSessions = false;
    }
  });
}

  loadRisks(id: number): void {
    this.learningPathService.detectPathRisks(id).subscribe({
      next: (risks) => {
        this.risks = risks;
      },
      error: (err) => {
        console.error('Error loading risks:', err);
      }
    });
  }

  getLevelIcon(level: string): string {
    const option = this.levelOptions.find(o => o.value === level);
    return option?.icon || '📘';
  }

  getLevelLabel(level: string): string {
    const option = this.levelOptions.find(o => o.value === level);
    return option?.label || level;
  }

  getLevelColor(level: string): string {
    const option = this.levelOptions.find(o => o.value === level);
    return option?.color || '#6b7280';
  }

  getStatusIcon(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.icon || '📌';
  }

  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.label || status;
  }

  getStatusClass(status: string): string {
    switch(status) {
      case 'DRAFT': return 'draft';
      case 'ACTIVE': return 'active';
      case 'PUBLISHED': return 'published';
      case 'ARCHIVED': return 'archived';
      default: return '';
    }
  }

  getSessionStatusClass(status: string): string {
    switch(status?.toLowerCase()) {
      case 'planned': return 'planned';
      case 'ongoing': return 'ongoing';
      case 'completed': return 'completed';
      case 'cancelled': return 'cancelled';
      default: return 'planned';
    }
  }

  getDifficultyLevel(hours: number): string {
    if (hours < 20) return 'Facile';
    if (hours < 50) return 'Moyen';
    return 'Intensif';
  }

  getDifficultyColor(hours: number): string {
    if (hours < 20) return '#10b981';
    if (hours < 50) return '#f59e0b';
    return '#ef4444';
  }

  calculateEstimatedWeeks(): number {
    const hours = this.path?.totalHours || 0;
    if (hours === 0) return 0;
    return Math.ceil(hours / 5);
  }

  getRiskSeverity(risk: string): string {
    if (risk.includes('dropout') || risk.includes('too long')) return 'high';
    if (risk.includes('No sessions')) return 'medium';
    return 'low';
  }

  editPath(): void {
    if (this.path?.id) {
      this.router.navigate(['/learning-paths/edit', this.path.id]);
    }
  }

  deletePath(): void {
    if (this.path?.id && confirm('Êtes-vous sûr de vouloir supprimer ce parcours ? Cette action est irréversible.')) {
      this.learningPathService.deleteLearningPath(this.path.id).subscribe({
        next: () => {
          this.successMessage = 'Parcours supprimé avec succès';
          this.showSuccess = true;
          setTimeout(() => {
            this.router.navigate(['/learning-paths']);
          }, 1500);
        },
        error: (err) => {
          console.error('Error deleting path:', err);
          this.errorMessage = 'Erreur lors de la suppression du parcours';
          this.showError = true;
          setTimeout(() => this.hideMessages(), 3000);
        }
      });
    }
  }

  viewSession(sessionId: number): void {
    this.router.navigate(['/sessions', sessionId]);
  }

  goBack(): void {
    this.router.navigate(['/learning-paths']);
  }

  hideMessages(): void {
    setTimeout(() => {
      this.showSuccess = false;
      this.showError = false;
      this.successMessage = '';
      this.errorMessage = '';
    }, 3000);
  }
}