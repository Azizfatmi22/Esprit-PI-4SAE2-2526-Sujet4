import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlanningService } from '../../services/planning.service';
import { SessionService } from '../../services/session.service';
import { Planning, PlanningMode, PlanningModeLabels, PlanningModeIcons } from '../../models/planning';
import { Session } from '../../models/session';

@Component({
  selector: 'app-planning-list',
  templateUrl: './planning-list.component.html',
  styleUrls: ['./planning-list.component.scss']
})
export class PlanningListComponent implements OnInit {
  // Données
  plannings: Planning[] = [];
  sessions: Session[] = [];
  filteredPlannings: Planning[] = [];
  
  // États d'affichage
  viewMode: 'grid' | 'list' = 'grid';
  selectedFilter: string = 'all';
  searchText: string = '';
  
  // Statistiques
  totalSessions: number = 0;
  sessionsWithoutPlanning: number = 0;
  totalHours: number = 0;
  
  // Message
  message: { show: boolean; type: 'success' | 'error'; text: string } = {
    show: false,
    type: 'success',
    text: ''
  };

  // Constantes pour le template
  PlanningMode = PlanningMode;

  constructor(
    private planningService: PlanningService,
    private sessionService: SessionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  // Charger toutes les données
  loadData(): void {
    this.loadSessions();
    this.loadAllPlannings();
  }

  // Charger les sessions
  loadSessions(): void {
    this.sessionService.getAllSessions().subscribe({
      next: (data) => {
        this.sessions = data;
        this.totalSessions = data.length;
        this.calculateStats();
      },
      error: (error) => {
        this.showMessage('Erreur lors du chargement des sessions', 'error');
        console.error('Error loading sessions:', error);
      }
    });
  }

  // Charger tous les plannings
  loadAllPlannings(): void {
    this.sessionService.getAllSessions().subscribe({
      next: (sessions) => {
        const planningRequests = sessions.map(session => 
          this.planningService.getPlanningsBySession(session.id!).toPromise()
        );
        
        Promise.all(planningRequests).then(results => {
          this.plannings = results
            .flat()
            .filter(planning => planning !== null) as Planning[];
          this.calculateStats();
          this.applyFilters();
        });
      },
      error: (error) => {
        console.error('Error loading plannings:', error);
        this.showMessage('Erreur lors du chargement des plannings', 'error');
      }
    });
  }

  // Calculer les statistiques
  calculateStats(): void {
    // Sessions sans planning
    this.sessionsWithoutPlanning = this.sessions.filter(
      session => !this.plannings.some(p => p.sessionId === session.id)
    ).length;

    // Total des heures
    this.totalHours = this.plannings.reduce((acc, p) => acc + (p.totalHours || 0), 0);
  }

  // Appliquer les filtres
  applyFilters(): void {
    let filtered = [...this.plannings];

    // Filtre par recherche textuelle
    if (this.searchText) {
      const searchLower = this.searchText.toLowerCase();
      filtered = filtered.filter(planning => {
        const session = this.getSessionById(planning.sessionId);
        return session?.id?.toString().includes(searchLower) ||
               planning.mode.toLowerCase().includes(searchLower) ||
               this.getModeLabel(planning.mode).toLowerCase().includes(searchLower);
      });
    }

    // Filtre par mode
    if (this.selectedFilter !== 'all') {
      const modeMap: Record<string, PlanningMode> = {
        'onsite': PlanningMode.ONSITE,
        'online': PlanningMode.ONLINE,
        'hybrid': PlanningMode.HYBRID
      };
      const mode = modeMap[this.selectedFilter];
      if (mode) {
        filtered = filtered.filter(p => p.mode === mode);
      }
    }

    // Tri par date (du plus récent au plus ancien)
    filtered.sort((a, b) => 
      new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
    );

    this.filteredPlannings = filtered;
  }

  // Réinitialiser les filtres
  resetFilters(): void {
    this.searchText = '';
    this.selectedFilter = 'all';
    this.applyFilters();
  }

  // Effacer la recherche
  clearSearch(): void {
    this.searchText = '';
    this.applyFilters();
  }

  // Obtenir une session par son ID
  getSessionById(sessionId?: number): Session | undefined {
    return this.sessions.find(s => s.id === sessionId);
  }

  // Compter les plannings par mode
  getCountByMode(mode: string): number {
    const modeEnum = mode as PlanningMode;
    return this.plannings.filter(p => p.mode === modeEnum).length;
  }

  // Obtenir l'icône du mode
  getModeIcon(mode: PlanningMode): string {
    return PlanningModeIcons[mode] || '';
  }

  // Obtenir le libellé du mode
  getModeLabel(mode: PlanningMode): string {
    return PlanningModeLabels[mode] || mode;
  }

  // Vérifier si une session a un planning
  hasPlanning(sessionId: number): boolean {
    return this.plannings.some(p => p.sessionId === sessionId);
  }

  // Obtenir le planning d'une session
  getPlanningBySession(sessionId: number): Planning | undefined {
    return this.plannings.find(p => p.sessionId === sessionId);
  }

  // Charger un planning spécifique
  loadPlanningById(id: number): void {
    this.planningService.getPlanningById(id).subscribe({
      next: (planning) => {
        console.log('Planning loaded:', planning);
      },
      error: (error) => {
        console.error('Error loading planning:', error);
      }
    });
  }

  // Supprimer un planning
  deletePlanning(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce planning ?')) {
      this.planningService.deletePlanning(id).subscribe({
        next: () => {
          this.showMessage('Planning supprimé avec succès', 'success');
          this.plannings = this.plannings.filter(p => p.id !== id);
          this.calculateStats();
          this.applyFilters();
        },
        error: (error) => {
          this.showMessage('Erreur lors de la suppression', 'error');
          console.error('Error deleting planning:', error);
        }
      });
    }
  }

  // Navigation vers l'édition d'un planning avec sessionId
  editPlanning(id: number, sessionId?: number): void {
    if (sessionId) {
      // Si on a l'ID de session, on le passe en queryParams
      this.router.navigate(['/plannings/edit', id], {
        queryParams: { sessionId: sessionId }
      });
    } else {
      // Sinon on navigue sans paramètre
      this.router.navigate(['/plannings/edit', id]);
    }
  }

  // Navigation vers les détails d'un planning
  viewPlanningDetails(id: number): void {
    this.router.navigate(['/plannings', id]);
  }

  // Mettre à jour un planning
  updatePlanning(id: number, planningData: Partial<Planning>): void {
    const planning = this.plannings.find(p => p.id === id);
    if (planning) {
      const updatedPlanning = { ...planning, ...planningData };
      this.planningService.updatePlanning(id, updatedPlanning).subscribe({
        next: (updated) => {
          const index = this.plannings.findIndex(p => p.id === id);
          if (index !== -1) {
            this.plannings[index] = updated;
            this.applyFilters();
          }
          this.showMessage('Planning mis à jour avec succès', 'success');
        },
        error: (error) => {
          this.showMessage('Erreur lors de la mise à jour', 'error');
          console.error('Error updating planning:', error);
        }
      });
    }
  }

  // Navigation vers la création d'un planning pour une session
  createPlanningForSession(sessionId: number): void {
    this.router.navigate(['/plannings/new'], { 
      queryParams: { sessionId: sessionId }
    });
  }

  // Afficher un message
  showMessage(text: string, type: 'success' | 'error'): void {
    this.message = { show: true, type, text };
    
    if (type === 'success') {
      setTimeout(() => {
        this.message.show = false;
      }, 3000);
    }
  }

  // Formater une date
  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  // Obtenir la couleur du mode (pour les badges)
  getModeColor(mode: PlanningMode): string {
    const colors: Record<PlanningMode, string> = {
      [PlanningMode.ONSITE]: '#6366f1',
      [PlanningMode.ONLINE]: '#3b82f6',
      [PlanningMode.HYBRID]: '#f59e0b'
    };
    return colors[mode] || '#6b7280';
  }

  // Vérifier si des filtres sont actifs
  hasActiveFilters(): boolean {
    return !!(this.searchText || this.selectedFilter !== 'all');
  }

  // Obtenir le libellé du filtre actif
  getActiveFilterLabel(): string {
    switch (this.selectedFilter) {
      case 'onsite': return 'Présentiel';
      case 'online': return 'En ligne';
      case 'hybrid': return 'Hybride';
      default: return 'Tous';
    }
  }

  // Rafraîchir les données
  refreshData(): void {
    this.plannings = [];
    this.loadData();
  }

  // Obtenir le nom de la session
  getSessionName(sessionId?: number): string {
    const session = this.getSessionById(sessionId);
    return session ? `Session #${session.id}` : 'Session inconnue';
  }
}