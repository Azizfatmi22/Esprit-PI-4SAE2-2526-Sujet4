import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PlanningService } from '../../services/planning.service';
import { LocationService } from '../../services/location.service';
import { Planning, PlanningMode, PlanningModeLabels, PlanningModeIcons } from '../../models/planning';
import { Location, LocationType, LocationTypeLabels, LocationTypeIcons, LocationTypeColors } from '../../models/location';

@Component({
  selector: 'app-planning-details',
  templateUrl: './planning-details.component.html',
  styleUrls: ['./planning-details.component.scss']
})
export class PlanningDetailsComponent implements OnInit {
  planningId: number | null = null;
  planning: Planning | null = null;
  loading: boolean = true;
  error: string | null = null;

  // Exposer les enums au template
  PlanningMode = PlanningMode;
  PlanningModeLabels = PlanningModeLabels;
  PlanningModeIcons = PlanningModeIcons;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private planningService: PlanningService,
    private locationService: LocationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.planningId = +id;
      this.loadPlanningDetails(this.planningId);
    } else {
      this.error = 'ID de planning non trouvé';
      this.loading = false;
    }
  }

  /**
   * Charger les détails du planning
   */
  loadPlanningDetails(id: number): void {
    this.loading = true;
    this.error = null;
    
    this.planningService.getPlanningById(id).subscribe({
      next: (data) => {
        this.planning = data;
        this.loading = false;
        console.log('Planning details loaded:', this.planning);
      },
      error: (err) => {
        console.error('Error loading planning details:', err);
        this.error = 'Erreur lors du chargement des détails du planning';
        this.loading = false;
      }
    });
  }

  /**
   * Retourner à la page précédente
   */
  goBack(): void {
    if (this.planning?.sessionId) {
      this.router.navigate(['/sessions', this.planning.sessionId]);
    } else {
      this.router.navigate(['/sessions']);
    }
  }

  /**
   * Éditer le planning
   */
  editPlanning(): void {
    if (this.planningId) {
      this.router.navigate(['/plannings/edit', this.planningId]);
    }
  }

  /**
   * Supprimer le planning
   */
  deletePlanning(): void {
    if (this.planningId && confirm('Êtes-vous sûr de vouloir supprimer ce planning ?')) {
      this.planningService.deletePlanning(this.planningId).subscribe({
        next: () => {
          this.router.navigate(['/sessions', this.planning?.sessionId]);
        },
        error: (err) => {
          console.error('Error deleting planning:', err);
          this.error = 'Erreur lors de la suppression';
        }
      });
    }
  }

  /**
   * Obtenir le libellé du mode
   */
  getModeLabel(mode: PlanningMode): string {
    return PlanningModeLabels[mode] || mode;
  }

  /**
   * Obtenir l'icône du mode
   */
  getModeIcon(mode: PlanningMode): string {
    return PlanningModeIcons[mode] || '📅';
  }

  /**
   * Obtenir la classe CSS du mode
   */
  getModeClass(mode: PlanningMode): string {
    const classes = {
      [PlanningMode.ONSITE]: 'mode-onsite',
      [PlanningMode.ONLINE]: 'mode-online',
      [PlanningMode.HYBRID]: 'mode-hybrid'
    };
    return classes[mode] || '';
  }

  /**
   * Formater une date
   */
  formatDate(date: Date | undefined): string {
    if (!date) return 'Non définie';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  /**
   * Obtenir le libellé du type de lieu
   */
  getLocationTypeLabel(type: LocationType): string {
    return LocationTypeLabels[type] || type;
  }

  /**
   * Obtenir l'icône du type de lieu
   */
  getLocationTypeIcon(type: LocationType): string {
    return LocationTypeIcons[type] || '📍';
  }

  /**
   * Obtenir la couleur du type de lieu
   */
  getLocationTypeColor(type: LocationType): string {
    return LocationTypeColors[type] || '#6366f1';
  }

  /**
   * Formater l'affichage complet du lieu
   */
  getLocationDisplay(location: Location | undefined): string {
    if (!location) return 'Aucun lieu assigné';
    return `${location.name} - ${location.address}, ${location.city} (Capacité: ${location.capacity} personnes)`;
  }

  /**
   * Vérifier si le planning est en mode ONSITE
   */
  isOnsiteMode(): boolean {
    return this.planning?.mode === PlanningMode.ONSITE;
  }

  /**
   * Vérifier si le planning est en mode ONLINE
   */
  isOnlineMode(): boolean {
    return this.planning?.mode === PlanningMode.ONLINE;
  }

  /**
   * Vérifier si le planning est en mode HYBRID
   */
  isHybridMode(): boolean {
    return this.planning?.mode === PlanningMode.HYBRID;
  }

  /**
   * Obtenir le nom de la session associée
   */
  getSessionName(): string {
    return this.planning?.sessionId ? `Session #${this.planning.sessionId}` : 'Non associée';
  }

  /**
   * Obtenir la durée formatée
   */
  getFormattedDuration(): string {
    if (!this.planning?.totalHours) return 'Non définie';
    const hours = this.planning.totalHours;
    const days = Math.floor(hours / 8);
    const remainingHours = hours % 8;
    
    if (days > 0) {
      return `${hours}h (${days} jour${days > 1 ? 's' : ''}${remainingHours > 0 ? ` ${remainingHours}h` : ''})`;
    }
    return `${hours} heure${hours > 1 ? 's' : ''}`;
  }

  /**
   * Obtenir l'URL de la plateforme (pour ONLINE et HYBRID)
   * Note: Cette propriété vient du backend mais n'est pas dans l'interface Planning
   */
  getPlatformUrl(): string | null {
    // Cast en any pour accéder à la propriété qui vient du backend
    return (this.planning as any)?.platformUrl || null;
  }

  /**
   * Vérifier si l'URL est valide
   */
  isValidUrl(url: string | null): boolean {
    if (!url) return false;
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Obtenir le nom de domaine de l'URL
   */
  getUrlDomain(url: string | null): string {
    if (!url) return '';
    try {
      const domain = new URL(url).hostname;
      return domain.replace('www.', '');
    } catch {
      return url;
    }
  }

  goToScheduleManagement(): void {
  if (this.planningId && this.planning) {
    // Option 1: Use query params only (recommended)
    this.router.navigate(['/schedule/new'], {
      queryParams: {
        planningId: this.planningId,
        startDate: this.planning.startDate,
        endDate: this.planning.endDate,
        mode: this.planning.mode
      }
    });
  }
}


/**
 * Alternative: Rediriger vers le formulaire d'ajout de schedule
 */
addSchedule(): void {
  if (this.planningId && this.planning) {
    this.router.navigate(['/schedules/new'], {
      queryParams: {
        planningId: this.planningId,
        startDate: this.planning.startDate,
        endDate: this.planning.endDate,
        mode: this.planning.mode
      }
    });
  }
}

/**
 * Voir tous les schedules du planning
 */
viewSchedules(): void {
  if (this.planningId) {
    this.router.navigate(['/schedules/new'], {
      queryParams: { planningId: this.planningId }
    });
  }
}
}