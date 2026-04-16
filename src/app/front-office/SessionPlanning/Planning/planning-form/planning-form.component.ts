import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PlanningService } from '../../services/planning.service';
import { LocationService } from '../../services/location.service';
import { SessionService } from '../../services/session.service';
import { Planning, PlanningMode, PlanningModeLabels, PlanningModeIcons, PlanningModeDescriptions } from '../../models/planning';
import { Location, LocationType } from '../../models/location';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface SessionDay {
  date: Date;
  dayNumber: number;
  dayName: string;
  hours: number;
  startTime?: string;
  endTime?: string;
  isWeekend: boolean;
  sessions: {
    morning?: { start: string; end: string; hours: number };
    afternoon?: { start: string; end: string; hours: number };
    evening?: { start: string; end: string; hours: number };
  };
  breaks: {
    lunch: { start: string; end: string; duration: number };
    coffee: { start: string; end: string; duration: number }[];
  };
}

@Component({
  selector: 'app-planning-form',
  templateUrl: './planning-form.component.html',
  styleUrls: ['./planning-form.component.scss']
})
export class PlanningFormComponent implements OnInit {
  @Input() sessionId?: number;
  
  // Form
  planningForm!: FormGroup;
  isEditMode: boolean = false;
  planningId: number | null = null;
  
  // UI States
  isProcessing: boolean = false;
  isSubmitting: boolean = false;
  loading: boolean = false;
  locationsLoading: boolean = false;
  
  // Messages
  statusMessage: string = '';
  statusType: 'success' | 'error' | 'warning' | 'info' = 'success';
  showSuccess: boolean = false;
  showError: boolean = false;
  showInfo: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  infoMessage: string = '';
  
  // Timeout pour les messages
  private messageTimeout: any = null;
  
  // Preview
  showPlanningPreview: boolean = false;
  previewPlannings: Planning[] = [];
  previewSummary: any = {
    totalDays: 0,
    totalHours: 0,
    startDate: '',
    endDate: '',
    locations: [] as string[]
  };
  
  // Locations
  locations: Location[] = [];
  filteredLocations: Location[] = [];
  alternativeLocations: Location[] = [];
  selectedLocation: Location | null = null;
  
  // Session data
  sessionParticipants: number = 0;
  
  // Capacity check
  capacityStatus: 'ok' | 'warning' | 'error' = 'ok';
  capacityMessage: string = '';
  capacityPercentage: number = 0;
  
  // URL for online/hybrid
  platformUrl: string = '';
  
  // Propriétés pour l'affichage conditionnel
  showLocation: boolean = false;
  
  // Advanced features
  checkingConflict: boolean = false;
  hasConflict: boolean = false;
  conflictDetails: any = null;
  hasDateError: boolean = false;
  suggestedDate: string | null = null;
  distributeDays: number = 5;
  locationUsageCount: number | null = null;
  
  // Schedule preview
  schedulePreview: SessionDay[] = [];
  selectedView: 'calendar' | 'list' | 'timeline' = 'calendar';
  timeSlots: string[] = ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'];
  
  // Hours and capacity recommendations
  hoursRecommendation: string = '';
  capacityRecommendation: string = '';
  optimalDistribution: string = '';
  sessionType: string = '';
  
  // Creative UX features
  showMagicWand: boolean = false;
  activeQuickAction: string | null = null;
  showConfetti: boolean = false;
  generatedPlannings: Planning[] = [];
  showDistributionPreview: boolean = false;
  distributionPreview: { day: number; date: string; hours: number; }[] = [];
  
  // Analytics
  busyDays: any[] = [];
  bestLocation: any = null;
  riskAssessment: any = null;
  showRiskAlert: boolean = false;
  rollingDaysAhead: number = 30;
  showAnalytics: boolean = false;
  analyticsView: 'busy' | 'risk' | 'best' = 'busy';
  
  // Loading states
  isGenerating: boolean = false;
  isDistributing: boolean = false;
  isGeneratingFill: boolean = false;
  isGeneratingRolling: boolean = false;
  isAnalyzingRisk: boolean = false;
  isFindingBestLocation: boolean = false;
  isAnalyzingBusyDays: boolean = false;
  
  planningStats: any = {
    totalDays: 0,
    averageHoursPerDay: 0,
    totalHours: 0,
    recommendedHoursPerDay: 0,
    isOverloaded: false,
    isUnderloaded: false,
    weekendsIncluded: 0,
    weekdaysOnly: 0
  };

  // Conflict types
  conflictTypes = {
    LOCATION_BOOKED: 'location_booked',
    TRAINER_UNAVAILABLE: 'trainer_unavailable',
    CAPACITY_EXCEEDED: 'capacity_exceeded',
    DATE_CONSTRAINT: 'date_constraint'
  };

  // Quick Actions
  quickActions = [
    { 
      id: 'magic',
      icon: '✨', 
      label: 'Auto', 
      description: 'Génération complète',
      color: '#6366f1' 
    },
    { 
      id: 'optimize',
      icon: '⚡', 
      label: 'Optimiser', 
      description: 'Optimise dates + lieu',
      color: '#10b981' 
    },
    { 
      id: 'analyze',
      icon: '📊', 
      label: 'Analyser', 
      description: 'Risques + disponibilités',
      color: '#f59e0b' 
    },
    { 
      id: 'fix',
      icon: '🔧', 
      label: 'Corriger', 
      description: 'Corrige les problèmes',
      color: '#ef4444' 
    }
  ];

  // Planning modes
  planningModes = [
    { 
      value: PlanningMode.ONSITE, 
      label: 'Présentiel', 
      icon: '📍',
      description: 'Formation en salle',
      color: '#6366f1',
      gradient: 'linear-gradient(135deg, #6366f1, #818cf8)'
    },
    { 
      value: PlanningMode.ONLINE, 
      label: 'En ligne', 
      icon: '🌐',
      description: 'Formation à distance',
      color: '#10b981',
      gradient: 'linear-gradient(135deg, #10b981, #34d399)'
    },
    { 
      value: PlanningMode.HYBRID, 
      label: 'Hybride', 
      icon: '🔄',
      description: 'Mixte présentiel/distanciel',
      color: '#f59e0b',
      gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)'
    }
  ];

  constructor(
    private fb: FormBuilder,
    private planningService: PlanningService,
    private locationService: LocationService,
    private sessionService: SessionService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadLocations();
    this.checkRouteParams();
  }

  initForm(): void {
    this.planningForm = this.fb.group({
      mode: ['', Validators.required],
      startDate: [''],
      endDate: [''],
      totalHours: [35, [Validators.min(1)]],
      sessionId: [this.sessionId || null],
      excludeWeekends: [true],
      preferredStartTime: ['09:00'],
      preferredEndTime: ['17:00'],
      lunchBreak: [true]
    });

    // Watch for changes
    this.planningForm.get('startDate')?.valueChanges.subscribe(() => {
      this.validateDates();
      if (this.selectedLocation) {
        this.checkForConflict();
      }
      this.updatePlanningStats();
      this.generateHoursRecommendations();
      this.generateSchedulePreview();
    });
    
    this.planningForm.get('endDate')?.valueChanges.subscribe(() => {
      this.validateDates();
      if (this.selectedLocation) {
        this.checkForConflict();
      }
      this.updatePlanningStats();
      this.generateHoursRecommendations();
      this.generateSchedulePreview();
    });

    this.planningForm.get('totalHours')?.valueChanges.subscribe(() => {
      this.updatePlanningStats();
      this.generateHoursRecommendations();
      this.generateSchedulePreview();
    });

    this.planningForm.get('mode')?.valueChanges.subscribe(mode => {
      this.showLocation = true;
      this.updateFormControls(mode);
      this.updateSessionType(mode);
      if (mode === PlanningMode.ONSITE || mode === PlanningMode.HYBRID) {
        this.filterLocationsByType();
        this.showMagicWand = true;
      } else {
        this.clearSelectedLocation();
        this.showMagicWand = false;
      }
    });

    this.planningForm.get('excludeWeekends')?.valueChanges.subscribe(() => {
      this.generateSchedulePreview();
    });
  }

  checkRouteParams(): void {
    this.route.queryParams.subscribe(params => {
      if (params['sessionId']) {
        this.sessionId = +params['sessionId'];
        this.planningForm.patchValue({ sessionId: this.sessionId });
        this.loadSessionParticipants(this.sessionId);
      }
    });

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.planningId = +params['id'];
        this.loadPlanningData(this.planningId);
      }
    });
  }

  updateFormControls(mode: string): void {
    if (this.planningForm.contains('locationId')) {
      this.planningForm.removeControl('locationId');
    }
    if (this.planningForm.contains('platformUrl')) {
      this.planningForm.removeControl('platformUrl');
    }

    switch (mode) {
      case PlanningMode.ONSITE:
        this.planningForm.addControl('locationId', new FormControl(null, Validators.required));
        break;
        
      case PlanningMode.ONLINE:
        this.planningForm.addControl('locationId', new FormControl(null, Validators.required));
        this.selectedLocation = null;
        break;
        
      case PlanningMode.HYBRID:
        this.planningForm.addControl('locationId', new FormControl(null, Validators.required));
        break;
    }
  }
  sessionCreatedDate!: Date;
  // ==================== SESSION FUNCTIONS ====================

  /**
   * Récupère le nombre de participants depuis la session
   */
  loadSessionParticipants(sessionId?: number): void {
    const id = sessionId || this.sessionId;
    if (!id) {
      this.sessionParticipants = 20;
      return;
    }
    
    this.sessionService.getSessionById(id).subscribe({
      next: (session) => {
        this.sessionParticipants = session.maxParticipants || 20;
        this.sessionCreatedDate = new Date(session.createdAt || new Date());
        if (this.selectedLocation) {
          this.checkCapacityWithBackend(this.selectedLocation);
        }
      },
      error: (err) => {
        console.error('Erreur chargement session:', err);
        this.sessionParticipants = 20;
      }
    });
  }

  // ==================== LOCATION FUNCTIONS ====================

  loadLocations(): void {
    this.locationsLoading = true;
    this.locationService.getAllLocations().pipe(
      catchError(err => {
        console.error('Error loading locations:', err);
        this.showSimpleMessage('Erreur chargement lieux', 'error');
        return of([]);
      })
    ).subscribe({
      next: (data) => {
        this.locations = data;
        this.filterLocationsByType();
        this.locationsLoading = false;
      }
    });
  }

  filterLocationsByType(searchTerm: string = ''): void {
    const currentMode = this.planningForm.get('mode')?.value;
    
    if (!currentMode) {
      this.filteredLocations = [];
      return;
    }

    this.locationsLoading = true;
    
    let typesToLoad: LocationType[] = [];
    
    switch(currentMode) {
      case PlanningMode.ONSITE:
        typesToLoad = [LocationType.ROOM];
        break;
      case PlanningMode.ONLINE:
        typesToLoad = [LocationType.ONLINE_PLATFORM];
        break;
      case PlanningMode.HYBRID:
        typesToLoad = [LocationType.ROOM, LocationType.ONLINE_PLATFORM, LocationType.HYBRID];
        break;
      default:
        this.filteredLocations = [];
        this.locationsLoading = false;
        return;
    }
    
    const observables = typesToLoad.map(type => 
      this.locationService.getLocationsByType(type).pipe(
        catchError(err => {
          console.error(`Error loading locations of type ${type}:`, err);
          return of([]);
        })
      )
    );
    
    forkJoin(observables).subscribe({
      next: (results) => {
        let allLocations: Location[] = [];
        results.forEach(locations => {
          allLocations = [...allLocations, ...locations];
        });
        
        if (searchTerm) {
          const term = searchTerm.toLowerCase();
          allLocations = allLocations.filter(loc => 
            loc.name.toLowerCase().includes(term) ||
            loc.city?.toLowerCase().includes(term) ||
            loc.address?.toLowerCase().includes(term)
          );
        }
        
        this.filteredLocations = allLocations.sort((a, b) => 
          a.name.localeCompare(b.name)
        );
        
        this.locationsLoading = false;
      },
      error: (err) => {
        console.error('Error loading locations:', err);
        this.filteredLocations = [];
        this.locationsLoading = false;
        this.showSimpleMessage('Erreur chargement des lieux', 'error');
      }
    });
  }

  selectLocation(location: Location): void {
    this.selectedLocation = location;
    this.planningForm.patchValue({ locationId: location.id });
    
    this.checkCapacityWithBackend(location);
    
    if (location.id) {
      this.planningService.countPlanningsByLocation(location.id).subscribe({
        next: (count) => {
          this.locationUsageCount = count;
        }
      });
    }
    
    if (this.planningForm.get('startDate')?.value) {
      this.checkForConflict();
    }
    
    this.generateHoursRecommendations();
    this.showMagicWand = true;
    
    this.showSimpleMessage(`${location.name} sélectionné`, 'success');
  }

  clearSelectedLocation(): void {
    this.selectedLocation = null;
    this.planningForm.patchValue({ locationId: null });
    this.hasConflict = false;
    this.locationUsageCount = null;
    this.conflictDetails = null;
    this.alternativeLocations = [];
  }

  createNewLocation(): void {
    this.router.navigate(['/locations/new'], { 
      queryParams: { returnUrl: this.router.url }
    });
  }

  // ==================== CAPACITY CHECK ====================

  checkCapacityWithBackend(location: Location): void {
    if (!location) return;
    
    if (this.sessionParticipants === 0) {
      this.loadSessionParticipants();
      return;
    }

    this.locationService.getAvailableLocations(this.sessionParticipants).subscribe({
      next: (availableLocations) => {
        const isAvailable = availableLocations.some(l => l.id === location.id);
        
        if (!isAvailable) {
          this.capacityStatus = 'error';
          this.capacityPercentage = 100;
          this.capacityMessage = `❌ Capacité insuffisante ! La session nécessite ${this.sessionParticipants} places.`;
          this.findAlternativeLocations();
          this.showSimpleMessage(`⚠️ Capacité insuffisante pour ${location.name}`, 'warning');
        } else {
          this.capacityPercentage = (this.sessionParticipants / location.capacity) * 100;
          
          if (this.capacityPercentage > 90) {
            this.capacityStatus = 'warning';
            this.capacityMessage = `⚠️ Capacité limite : ${Math.round(this.capacityPercentage)}% d'occupation (${this.sessionParticipants}/${location.capacity})`;
          } else if (this.capacityPercentage > 75) {
            this.capacityStatus = 'warning';
            this.capacityMessage = `⚠️ Capacité élevée : ${Math.round(this.capacityPercentage)}%`;
          } else {
            this.capacityStatus = 'ok';
            this.capacityMessage = `✅ Capacité adaptée : ${Math.round(this.capacityPercentage)}% d'occupation`;
          }
        }
      },
      error: (err) => {
        console.error('Erreur vérification capacité:', err);
        this.checkCapacityLocally(location);
      }
    });

    this.locationService.findOverloadedLocations(80).subscribe({
      next: (overloaded) => {
        const isOverloaded = overloaded.some(l => l.id === location.id);
        if (isOverloaded && this.capacityStatus !== 'error') {
          this.capacityStatus = 'warning';
          this.capacityMessage = '⚠️ Ce lieu est généralement très demandé';
        }
      },
      error: () => {}
    });
  }

  checkCapacityLocally(location: Location): void {
    if (!this.sessionParticipants) {
      this.capacityStatus = 'warning';
      this.capacityMessage = '⚠️ Nombre de participants non défini';
      return;
    }

    if (this.sessionParticipants > location.capacity) {
      this.capacityStatus = 'error';
      this.capacityPercentage = 100;
      this.capacityMessage = `❌ Capacité insuffisante ! Besoin de ${this.sessionParticipants} places, ce lieu n'en propose que ${location.capacity}.`;
      this.findAlternativeLocations();
    } else {
      this.capacityPercentage = (this.sessionParticipants / location.capacity) * 100;
      
      if (this.capacityPercentage > 90) {
        this.capacityStatus = 'warning';
        this.capacityMessage = `⚠️ Capacité limite : ${Math.round(this.capacityPercentage)}% d'occupation`;
      } else {
        this.capacityStatus = 'ok';
        this.capacityMessage = `✅ Capacité adaptée : ${Math.round(this.capacityPercentage)}% d'occupation`;
      }
    }
  }

  findAlternativeLocations(): void {
    if (!this.sessionParticipants) return;
    
    const currentMode = this.planningForm.get('mode')?.value;
    let types: LocationType[] = [];
    
    switch(currentMode) {
      case PlanningMode.ONSITE:
        types = [LocationType.ROOM];
        break;
      case PlanningMode.ONLINE:
        types = [LocationType.ONLINE_PLATFORM];
        break;
      case PlanningMode.HYBRID:
        types = [LocationType.ROOM, LocationType.HYBRID];
        break;
      default:
        return;
    }
    
    this.locationService.getAvailableLocations(this.sessionParticipants).subscribe({
      next: (available) => {
        this.alternativeLocations = available
          .filter(loc => 
            loc.id !== this.selectedLocation?.id && 
            types.includes(loc.type)
          )
          .sort((a, b) => a.capacity - b.capacity)
          .slice(0, 3);
      },
      error: () => {
        this.alternativeLocations = this.locations
          .filter(loc => 
            loc.id !== this.selectedLocation?.id && 
            types.includes(loc.type) &&
            loc.capacity >= this.sessionParticipants
          )
          .sort((a, b) => a.capacity - b.capacity)
          .slice(0, 3);
      }
    });
  }

  // ==================== PLANNING DATA LOADING ====================

  loadPlanningData(id: number): void {
    this.loading = true;
    this.showSimpleMessage('Chargement du planning...', 'info');
    
    this.planningService.getPlanningById(id).subscribe({
      next: (planning) => {
        console.log('Planning chargé:', planning);
        
        this.planningForm.patchValue({
          mode: planning.mode,
          startDate: planning.startDate,
          endDate: planning.endDate,
          totalHours: planning.totalHours,
          sessionId: planning.sessionId
        });

        this.updateFormControls(planning.mode);
        this.updateSessionType(planning.mode);

        if (planning.location) {
          this.selectedLocation = planning.location;
          this.planningForm.patchValue({ locationId: planning.location.id });
          
          this.loadSessionParticipants(planning.sessionId);
          
          if (planning.location.id) {
            this.planningService.countPlanningsByLocation(planning.location.id).subscribe({
              next: (count) => {
                this.locationUsageCount = count;
              }
            });
          }
          
          this.showMagicWand = true;
        }
        
        if (planning.mode !== PlanningMode.ONSITE && (planning as any).platformUrl) {
          this.planningForm.patchValue({ platformUrl: (planning as any).platformUrl });
        }
        
        this.generateHoursRecommendations();
        this.generateSchedulePreview();
        
        if (planning.startDate && planning.endDate && this.selectedLocation) {
          this.checkForConflict();
        }
        
        this.loading = false;
        this.showSimpleMessage('Planning chargé avec succès', 'success');
      },
      error: (error) => {
        console.error('Erreur chargement planning:', error);
        this.showSimpleMessage('Erreur lors du chargement du planning', 'error');
        this.loading = false;
      }
    });
  }

  // ==================== FONCTIONS DU SCÉNARIO ====================

  /**
   * 📅 ÉTAPE 2: Génération automatique du planning
   */
  generatePlanningDemo(): void {
  if (!this.sessionId) {
    this.showSimpleMessage('❌ Session ID manquant', 'error');
    return;
  }

  const mode = this.planningForm.get('mode')?.value;
  
  if (!mode) {
    this.showSimpleMessage('❌ Veuillez sélectionner un mode de formation', 'error');
    return;
  }

  this.isGenerating = true;
  this.activeQuickAction = 'magic';
  this.showSimpleMessage(`✨ Génération du planning ${this.getModeLabel(mode)}...`, 'info');

  // Pass the selected mode to the service
  this.planningService.generatePlanning(this.sessionId, mode).subscribe({
    next: (planning) => {
      console.log('✅ Planning généré:', planning);
      
      if (!planning) {
        this.showSimpleMessage('⚠️ Aucun planning généré', 'warning');
        this.resetGenerationState();
        return;
      }

      // ✅ Store preview
      this.previewPlannings = [planning];
      
      // Update the form with generated data
      this.planningForm.patchValue({
        startDate: planning.startDate,
        endDate: planning.endDate,
        totalHours: planning.totalHours
      });

      // ✅ If location is provided, select it
      if (planning.location) {
        this.selectLocation(planning.location);
      }

      // ✅ Safe date parsing
      const start = new Date(planning.startDate);
      const end = new Date(planning.endDate);

      const totalDays =
        Math.floor((end.getTime() - start.getTime()) / (1000 * 3600 * 24)) + 1;

      // ✅ Safe location
      const locationName =
        planning.location?.name || this.selectedLocation?.name || 'Non défini';

      this.previewSummary = {
        totalDays,
        totalHours: planning.totalHours ?? 0,
        startDate: planning.startDate,
        endDate: planning.endDate,
        locations: [locationName]
      };

      // Generate schedule preview
      this.generateSchedulePreview();

      // ✅ Show preview popup
      this.showPlanningPreview = true;

      this.resetGenerationState();
    },

    error: (err) => {
      console.error('❌ Erreur génération:', err);
      this.showSimpleMessage('Erreur lors de la génération', 'error');
      this.resetGenerationState();
    }
  });
}

/**
 * Generate planning with a specific mode (can be called from UI)
 */
generateWithMode(mode: string): void {
  this.planningForm.patchValue({ mode });
  this.generatePlanningDemo();
}

  /**
   * ⚡ ÉTAPE 3: Distribution intelligente
   */
  distributePlanningDemo(): void {
    if (!this.sessionId || !this.selectedLocation?.id) {
      this.showSimpleMessage('❌ Veuillez sélectionner un lieu d\'abord', 'error');
      return;
    }

    this.isDistributing = true;
    this.activeQuickAction = 'optimize';
    this.showSimpleMessage(`📊 Distribution sur ${this.distributeDays} jours...`, 'info');

    this.planningService.distributePlanning(this.sessionId, this.selectedLocation.id, this.distributeDays).subscribe({
      next: (planning) => {
        console.log('✅ Planning distribué:', planning);
        
        if (planning) {
          // Mettre à jour le formulaire
          this.planningForm.patchValue({
            startDate: planning.startDate,
            endDate: planning.endDate
          });
          
          // Stocker pour la prévisualisation
          this.generatedPlannings = [planning];
          
          // Calculer le nombre de jours
          const start = new Date(planning.startDate);
          const end = new Date(planning.endDate);
          const totalDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 3600 * 24)) + 1;
          
          this.showConfetti = true;
          setTimeout(() => this.showConfetti = false, 3000);
          
          this.showSimpleMessage(
            `✅ Planning distribué sur ${totalDays} jours (${planning.totalHours || 0}h totales)`,
            'success'
          );
        }
        
        this.isDistributing = false;
        this.activeQuickAction = null;
      },
      error: (err) => {
        console.error('❌ Erreur distribution:', err);
        this.showSimpleMessage('Erreur lors de la distribution', 'error');
        this.isDistributing = false;
        this.activeQuickAction = null;
      }
    });
  }

  /**
   * 🚨 ÉTAPE 4: Détection de conflit
   */
  checkConflictDemo(): void {
    // Vérifier que toutes les données nécessaires sont présentes
    if (!this.selectedLocation?.id) {
      this.showSimpleMessage('❌ Veuillez sélectionner un lieu d\'abord', 'error');
      return;
    }

    const startDate = this.planningForm.get('startDate')?.value;
    const endDate = this.planningForm.get('endDate')?.value;

    if (!startDate || !endDate) {
      this.showSimpleMessage('❌ Veuillez sélectionner des dates', 'error');
      return;
    }

    // Validation que startDate n'est pas après endDate
    if (new Date(startDate) > new Date(endDate)) {
      this.showSimpleMessage('❌ La date de début doit être avant la date de fin', 'error');
      return;
    }

    this.checkingConflict = true;
    this.showSimpleMessage('🔍 Vérification des conflits...', 'info');

    console.log('📤 Vérification conflit:', {
      locationId: this.selectedLocation.id,
      startDate: startDate,
      endDate: endDate
    });

    this.planningService.checkConflict(
      this.selectedLocation.id,
      startDate,
      endDate
    ).subscribe({
      next: (conflict) => {
        console.log('✅ Résultat conflit:', conflict);
        
        this.hasConflict = conflict;
        this.checkingConflict = false;
        
        if (conflict) {
          this.showSimpleMessage('⚠️ Conflit détecté! Cette salle est déjà réservée sur cette période', 'warning');
          
          // Optionnel: Chercher une date alternative
          this.suggestDateDemo();
        } else {
          this.showSimpleMessage('✅ Aucun conflit, salle disponible pour toute la période', 'success');
        }
      },
      error: (err) => {
        console.error('❌ Erreur vérification conflit:', err);
        this.checkingConflict = false;
        
        // Message d'erreur plus détaillé
        if (err.status === 404) {
          this.showSimpleMessage('❌ Service non disponible', 'error');
        } else if (err.status === 400) {
          this.showSimpleMessage('❌ Requête invalide', 'error');
        } else {
          this.showSimpleMessage('Erreur lors de la vérification', 'error');
        }
      }
    });
  }

  /**
   * 🧠 ÉTAPE 5: Suggestion de date intelligente
   */
  suggestDateDemo(): void {
    if (!this.selectedLocation?.id) {
      this.showSimpleMessage('❌ Veuillez sélectionner un lieu d\'abord', 'error');
      return;
    }

    const startDate = this.planningForm.get('startDate')?.value || new Date().toISOString().split('T')[0];
    
    this.showSimpleMessage('🔮 Recherche de la prochaine date disponible...', 'info');

    this.planningService.smartSuggestDate(this.selectedLocation.id, startDate).subscribe({
      next: (date) => {
        this.suggestedDate = date;
        this.planningForm.patchValue({ startDate: date });
        this.showSimpleMessage(`📅 Date suggérée: ${new Date(date).toLocaleDateString()}`, 'success');
        
        // Re-vérifier les conflits avec la nouvelle date
        setTimeout(() => this.checkConflictDemo(), 500);
      },
      error: (err) => {
        console.error('❌ Erreur suggestion date:', err);
        this.showSimpleMessage('Aucune date disponible trouvée', 'warning');
      }
    });
  }

  /**
   * 🔄 ÉTAPE 6: Optimisation automatique
   */
  optimizePlanningDemo(): void {
    if (!this.sessionId) {
      this.showSimpleMessage('❌ Session ID manquant', 'error');
      return;
    }

    this.isProcessing = true;
    this.activeQuickAction = 'optimize';
    this.showSimpleMessage('⚡ Optimisation du planning en cours...', 'info');

    this.planningService.optimizePlanning(this.sessionId).subscribe({
      next: (planning) => {
        console.log('✅ Planning optimisé:', planning);
        
        if (planning) {
          // Mettre à jour le formulaire
          this.planningForm.patchValue({
            startDate: planning.startDate,
            endDate: planning.endDate
          });
          
          // Stocker pour la prévisualisation
          this.generatedPlannings = [planning];
          
          this.showConfetti = true;
          setTimeout(() => this.showConfetti = false, 3000);
          
          // Calculer le nombre de jours
          const start = new Date(planning.startDate);
          const end = new Date(planning.endDate);
          const totalDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 3600 * 24)) + 1;
          
          this.showSimpleMessage(
            `✅ Optimisation terminée: ${totalDays} jours, ${planning.totalHours || 0}h`,
            'success'
          );
        }
        
        this.isProcessing = false;
        this.activeQuickAction = null;
      },
      error: (err) => {
        console.error('❌ Erreur optimisation:', err);
        this.showSimpleMessage('Erreur lors de l\'optimisation', 'error');
        this.isProcessing = false;
        this.activeQuickAction = null;
      }
    });
  }

  /**
   * 📊 ÉTAPE 7: Analyse des risques et jours d'affluence
   */
  analyzeRiskDemo(): void {
    if (!this.sessionId) {
      this.showSimpleMessage('❌ Session ID manquant', 'error');
      return;
    }

    this.isAnalyzingRisk = true;
    this.activeQuickAction = 'analyze';
    this.showSimpleMessage('📊 Analyse complète en cours...', 'info');

    forkJoin({
      risk: this.planningService.isHighRisk(this.sessionId),
      busyDays: this.selectedLocation?.id ? 
        this.planningService.getBusyDays(this.selectedLocation.id) : 
        of(null)
    }).subscribe({
      next: (results) => {
        console.log('✅ Analyse risques:', results.risk);
        
        this.riskAssessment = results.risk;
        this.showAnalytics = true;
        this.analyticsView = 'risk';
        
        if (results.risk?.isHighRisk) {
          this.showRiskAlert = true;
          this.showSimpleMessage(`⚠️ Risque élevé détecté (score: ${results.risk.riskScore})`, 'warning');
        }
        
        if (results.busyDays) {
          this.busyDays = results.busyDays.days || [];
          this.analyticsView = 'busy';
          
          const avgOccupancy = results.busyDays.averageOccupancy || 0;
          const overbooked = results.busyDays.overbookedDays || 0;
          
          this.showSimpleMessage(
            `📊 Taux d'occupation moyen: ${avgOccupancy}%, ${overbooked} jours surchargés`,
            'info'
          );
        }
        
        this.isAnalyzingRisk = false;
        this.activeQuickAction = null;
      },
      error: (err) => {
        console.error('❌ Erreur analyse:', err);
        this.showSimpleMessage('Erreur lors de l\'analyse', 'error');
        this.isAnalyzingRisk = false;
        this.activeQuickAction = null;
      }
    });
  }

  /**
   * 🔧 ÉTAPE 8: Correction automatique (Fix)
   */
  fixAction(): void {
    this.isProcessing = true;
    this.activeQuickAction = 'fix';
    this.showSimpleMessage('🛠️ Correction automatique...', 'info');
    
    let fixed = false;
    let fixes: string[] = [];
    
    // 1. Correction de l'ordre des dates
    const start = this.planningForm.get('startDate')?.value;
    const end = this.planningForm.get('endDate')?.value;
    
    if (start && end && new Date(start) > new Date(end)) {
      this.planningForm.patchValue({
        startDate: end,
        endDate: start
      });
      fixed = true;
      fixes.push('Dates inversées corrigées');
    }
    
    // 2. Définition de dates par défaut si manquantes
    if (!start && !end) {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const nextWeek = new Date(tomorrow);
      nextWeek.setDate(nextWeek.getDate() + 4);
      
      this.planningForm.patchValue({
        startDate: tomorrow.toISOString().split('T')[0],
        endDate: nextWeek.toISOString().split('T')[0]
      });
      fixed = true;
      fixes.push('Dates par défaut ajoutées');
    }
    
    // 3. Heures par défaut si manquantes
    if (!this.planningForm.get('totalHours')?.value) {
      this.planningForm.patchValue({ totalHours: 35 });
      fixed = true;
      fixes.push('Volume horaire par défaut (35h)');
    }
    
    // 4. Vérification des conflits
    if (this.selectedLocation?.id && this.planningForm.get('startDate')?.value) {
      this.checkConflictDemo();
    }
    
    this.generateSchedulePreview();
    
    setTimeout(() => {
      this.isProcessing = false;
      this.activeQuickAction = null;
      
      if (fixed) {
        this.showSimpleMessage(`✅ Problèmes corrigés: ${fixes.join(', ')}`, 'success');
      } else {
        this.showSimpleMessage('✓ Rien à corriger', 'info');
      }
    }, 1000);
  }

  // ==================== QUICK ACTIONS ====================

  handleQuickAction(actionId: string): void {
    if (this.isProcessing) {
      this.showSimpleMessage('Action en cours...', 'info');
      return;
    }

    this.activeQuickAction = actionId;
    
    switch(actionId) {
      case 'magic':
        this.generatePlanningDemo();
        break;
      case 'optimize':
        this.optimizePlanningDemo();
        break;
      case 'analyze':
        this.analyzeRiskDemo();
        break;
      case 'fix':
        this.fixAction();
        break;
    }
  }

  magicAction(): void {
    this.generatePlanningDemo();
  }

  optimizeAction(): void {
    this.distributePlanningDemo();
  }

  analyzeAction(): void {
    this.analyzeRiskDemo();
  }

  private applyBestPlanning(plannings: any[], message?: string): void {
    if (!plannings?.length) {
      this.isProcessing = false;
      return;
    }
    
    const sorted = [...plannings].sort((a, b) => 
      new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
    );
    
    const best = sorted[0];
    this.planningForm.patchValue({
      startDate: best.startDate,
      endDate: best.endDate,
      totalHours: best.totalHours
    });
    
    this.generateSchedulePreview();
    this.showConfetti = true;
    setTimeout(() => this.showConfetti = false, 2000);
    
    if (message) {
      this.showSimpleMessage(message, 'success');
    }
    
    this.isProcessing = false;
    this.activeQuickAction = null;
  }

  // ==================== MESSAGE FUNCTIONS ====================

  showSimpleMessage(msg: string, type: 'success' | 'error' | 'warning' | 'info'): void {
    // Annuler le timeout précédent
    if (this.messageTimeout) {
      clearTimeout(this.messageTimeout);
    }
    
    this.statusMessage = msg;
    this.statusType = type;
    
    // Réinitialiser tous les états
    this.showSuccess = false;
    this.showError = false;
    this.showInfo = false;
    
    // Activer le bon état
    switch(type) {
      case 'success':
        this.successMessage = `✅ ${msg}`;
        this.showSuccess = true;
        break;
      case 'error':
        this.errorMessage = `❌ ${msg}`;
        this.showError = true;
        break;
      case 'warning':
        this.infoMessage = `⚠️ ${msg}`;
        this.showInfo = true;
        break;
      case 'info':
        this.infoMessage = `ℹ️ ${msg}`;
        this.showInfo = true;
        break;
    }
    
    // Auto-disparition après délai
    const delay = type === 'info' ? 2000 : 3000;
    this.messageTimeout = setTimeout(() => {
      this.showSuccess = false;
      this.showError = false;
      this.showInfo = false;
      this.statusMessage = '';
      this.messageTimeout = null;
    }, delay);
    
    // Log pour débogage
    console.log(`[${type.toUpperCase()}] ${msg}`);
  }

  // ==================== SCHEDULE FUNCTIONS ====================

  generateSchedulePreview(): void {
    const start = this.planningForm.get('startDate')?.value;
    const end = this.planningForm.get('endDate')?.value;
    const totalHours = this.planningForm.get('totalHours')?.value;
    const excludeWeekends = this.planningForm.get('excludeWeekends')?.value;

    if (!start || !end || !totalHours) {
      this.schedulePreview = [];
      return;
    }

    const startDate = new Date(start);
    const endDate = new Date(end);
    const days = this.getWorkingDays(startDate, endDate, excludeWeekends);
    
    if (days.length === 0) return;

    const hoursPerDay = totalHours / days.length;
    const baseHoursPerDay = Math.floor(hoursPerDay);
    const remainder = totalHours - (baseHoursPerDay * days.length);

    this.schedulePreview = days.map((date, index) => {
      const dayHours = baseHoursPerDay + (index < remainder ? 1 : 0);
      
      return {
        date: date,
        dayNumber: index + 1,
        dayName: this.getDayName(date),
        hours: dayHours,
        isWeekend: date.getDay() === 0 || date.getDay() === 6,
        sessions: this.generateDailySchedule(dayHours),
        breaks: this.generateBreaks(dayHours)
      };
    });

    this.planningStats.weekendsIncluded = this.schedulePreview.filter(d => d.isWeekend).length;
    this.planningStats.weekdaysOnly = this.schedulePreview.filter(d => !d.isWeekend).length;
    this.planningStats.totalDays = days.length;
    this.planningStats.averageHoursPerDay = Math.round((totalHours / days.length) * 10) / 10;
  }

  getWorkingDays(startDate: Date, endDate: Date, excludeWeekends: boolean): Date[] {
    const days: Date[] = [];
    const currentDate = new Date(startDate);
    
    while (currentDate <= endDate) {
      if (!excludeWeekends || (currentDate.getDay() !== 0 && currentDate.getDay() !== 6)) {
        days.push(new Date(currentDate));
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    return days;
  }

  generateDailySchedule(hours: number): any {
    const schedule: any = {};
    
    if (hours <= 4) {
      schedule.morning = { start: '09:00', end: '13:00', hours: 4 };
    } else if (hours <= 6) {
      schedule.morning = { start: '09:00', end: '12:00', hours: 3 };
      schedule.afternoon = { start: '13:00', end: '16:00', hours: 3 };
    } else {
      schedule.morning = { start: '09:00', end: '12:00', hours: 3 };
      schedule.afternoon = { start: '13:00', end: '17:00', hours: 4 };
      
      if (hours > 7) {
        schedule.evening = { start: '18:00', end: '20:00', hours: 2 };
      }
    }
    
    return schedule;
  }

  generateBreaks(hours: number): any {
    const breaks: any = {
      lunch: { start: '12:00', end: '13:00', duration: 1 },
      coffee: []
    };

    if (hours >= 6) {
      breaks.coffee.push(
        { start: '10:30', end: '10:45', duration: 0.25 },
        { start: '15:00', end: '15:15', duration: 0.25 }
      );
    } else if (hours >= 4) {
      breaks.coffee.push(
        { start: '10:30', end: '10:45', duration: 0.25 }
      );
    }

    return breaks;
  }

  getDayName(date: Date): string {
    const days = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
    return days[date.getDay()];
  }

  getScheduleSummary(): string {
    if (this.schedulePreview.length === 0) return '';
    
    const totalDays = this.schedulePreview.length;
    const avgHours = this.planningStats.averageHoursPerDay;
    
    return `${totalDays} jours • ${avgHours}h/jour`;
  }

  getDailyScheduleDescription(day: SessionDay): string {
    const parts = [];
    
    if (day.sessions.morning) {
      parts.push(`${day.sessions.morning.start}-${day.sessions.morning.end}`);
    }
    if (day.sessions.afternoon) {
      parts.push(`${day.sessions.afternoon.start}-${day.sessions.afternoon.end}`);
    }
    
    return parts.join(' / ');
  }

  isTimeSlotOccupied(day: SessionDay, timeSlot: string): boolean {
    const time = parseInt(timeSlot.split(':')[0]);
    
    if (day.sessions.morning && time < 13) return true;
    if (day.sessions.afternoon && time >= 13 && time < 17) return true;
    if (day.sessions.evening && time >= 18) return true;
    
    return false;
  }

  getTimeSlotType(day: SessionDay, timeSlot: string): string {
    const time = parseInt(timeSlot.split(':')[0]);
    
    if (time >= 12 && time < 13) return 'break';
    if (time < 13) return 'morning';
    if (time < 17) return 'afternoon';
    if (time >= 18) return 'evening';
    return 'free';
  }

  // ==================== UX FUNCTIONS ====================

  updateSessionType(mode: string): void {
    switch(mode) {
      case PlanningMode.ONSITE:
        this.sessionType = 'présentiel';
        break;
      case PlanningMode.ONLINE:
        this.sessionType = 'distanciel';
        break;
      case PlanningMode.HYBRID:
        this.sessionType = 'hybride';
        break;
      default:
        this.sessionType = 'formation';
    }
  }

  generateHoursRecommendations(): void {
    const hours = this.planningForm.get('totalHours')?.value;
    const start = this.planningForm.get('startDate')?.value;
    const end = this.planningForm.get('endDate')?.value;
    
    if (!hours) {
      this.hoursRecommendation = 'Entrez le nombre d\'heures';
      return;
    }

    if (start && end) {
      const days = this.planningStats.totalDays;
      
      if (days > 0) {
        const hoursPerDay = hours / days;
        this.planningStats.averageHoursPerDay = Math.round(hoursPerDay * 10) / 10;
        
        if (hoursPerDay > 8) {
          this.planningStats.isOverloaded = true;
          this.hoursRecommendation = `${Math.round(hoursPerDay)}h/jour : charge élevée`;
          const optimalDays = Math.ceil(hours / 8);
          this.optimalDistribution = `→ ${optimalDays} jours recommandés`;
        } else if (hoursPerDay < 4) {
          this.planningStats.isUnderloaded = true;
          this.hoursRecommendation = `${Math.round(hoursPerDay)}h/jour : charge légère`;
        } else {
          this.planningStats.isOverloaded = false;
          this.planningStats.isUnderloaded = false;
          this.hoursRecommendation = `${Math.round(hoursPerDay)}h/jour : rythme optimal`;
        }
      }
    } else {
      if (hours <= 8) {
        this.hoursRecommendation = '1 jour suffit';
      } else {
        const days = Math.ceil(hours / 7);
        this.hoursRecommendation = `${days} jours recommandés`;
      }
    }
  }

  // ==================== CONFLICT FUNCTIONS ====================

  checkForConflict(): void {
    const locationId = this.selectedLocation?.id;
    const startDate = this.planningForm.get('startDate')?.value;
    const endDate = this.planningForm.get('endDate')?.value;

    if (!locationId || !startDate || !endDate || this.isEditMode) {
      this.hasConflict = false;
      this.conflictDetails = null;
      return;
    }

    this.checkingConflict = true;

    this.planningService.checkConflict(locationId, startDate, endDate).subscribe({
      next: (conflict) => {
        this.hasConflict = conflict;
        this.checkingConflict = false;
        
        if (conflict) {
          this.showSimpleMessage('⚠️ Conflit détecté', 'warning');
        }
      },
      error: () => {
        this.checkingConflict = false;
      }
    });
  }

  // ==================== UTILITY FUNCTIONS ====================

  /**
   * Vérifie si un jour est valide (pas weekend)
   */
  isValidDay(date: Date): boolean {
    const day = date.getDay();
    return day !== 0 && day !== 6; // 0 = dimanche, 6 = samedi
  }

  validateDates(): void {
    const start = this.planningForm.get('startDate')?.value;
    const end = this.planningForm.get('endDate')?.value;
    
    if (start && end) {
      const startDate = new Date(start);
      const endDate = new Date(end);
      
      if (startDate > endDate) {
        this.planningForm.setErrors({ dateRange: true });
        this.hasDateError = true;
      } else {
        this.planningForm.setErrors(null);
        this.hasDateError = false;
      }
    } else {
      this.planningForm.setErrors(null);
      this.hasDateError = false;
    }
  }

  get dateError(): string | null {
    if (this.planningForm.hasError('dateRange')) {
      return 'La date de début doit être avant la date de fin';
    }
    return null;
  }

  updatePlanningStats(): void {
    const start = this.planningForm.get('startDate')?.value;
    const end = this.planningForm.get('endDate')?.value;
    const hours = this.planningForm.get('totalHours')?.value;

    this.planningStats.totalHours = hours || 0;

    if (start && end) {
      const days = Math.ceil((new Date(end).getTime() - new Date(start).getTime()) / (1000 * 3600 * 24)) + 1;
      this.planningStats.totalDays = days;
      
      if (hours) {
        const hoursPerDay = hours / days;
        this.planningStats.averageHoursPerDay = Math.round(hoursPerDay * 10) / 10;
        this.planningStats.isOverloaded = hoursPerDay > 8;
        this.planningStats.isUnderloaded = hoursPerDay < 4;
      }
    }
  }

  getNextMonday(): Date {
    const date = new Date();
    const day = date.getDay();
    const daysUntilMonday = day === 0 ? 1 : day === 1 ? 0 : 8 - day;
    date.setDate(date.getDate() + daysUntilMonday);
    return date;
  }

  onSubmit(): void {
  if (this.planningForm.invalid) {
    this.showSimpleMessage('Formulaire incomplet', 'error');
    return;
  }

  if (this.hasConflict) {
    this.showSimpleMessage('Conflit détecté', 'error');
    return;
  }

  if (this.capacityStatus === 'error') {
    this.showSimpleMessage('Capacité insuffisante', 'error');
    return;
  }

  const mode = this.planningForm.get('mode')?.value;

  if ((mode === PlanningMode.ONSITE || mode === PlanningMode.HYBRID) && !this.selectedLocation) {
    this.showSimpleMessage('Sélectionnez un lieu', 'error');
    return;
  }

  const formValue = this.planningForm.value;

  // ✅ FIX: ensure sessionId is defined
  const sessionId = this.sessionId || formValue.sessionId;

  if (!sessionId) {
    this.showSimpleMessage('Session manquante', 'error');
    return;
  }

  // ✅ FIX: validate startDate >= sessionCreatedDate
  const startDate = new Date(formValue.startDate);
  const sessionCreated = new Date(this.sessionCreatedDate);

  if (startDate < sessionCreated) {
    this.showSimpleMessage(
      '❌ La date de début est avant la création de la session',
      'error'
    );
    return;
  }

  this.isSubmitting = true;

  // ✅ Build planning object (compatible with your service)
  const planningData: any = {
    mode: formValue.mode,
    startDate: formValue.startDate,
    endDate: formValue.endDate,
    totalHours: formValue.totalHours
  };

  if (mode === PlanningMode.ONLINE || mode === PlanningMode.HYBRID) {
    planningData.platformUrl = formValue.platformUrl;
  }

  const locationId = this.selectedLocation?.id;

  // 🔁 UPDATE MODE
  if (this.isEditMode && this.planningId) {
    this.planningService.updatePlanning(this.planningId, planningData).subscribe({
      next: () => {
        this.showConfetti = true;
        setTimeout(() => (this.showConfetti = false), 3000);
        this.showSimpleMessage('Planning mis à jour!', 'success');
        setTimeout(() => this.goBack(), 1500);
      },
      error: () => {
        this.showSimpleMessage('Erreur mise à jour', 'error');
        this.isSubmitting = false;
      }
    });

  // ➕ CREATE MODE
  } else {
    this.planningService.createPlanning(planningData, sessionId, locationId).subscribe({
      next: () => {
        this.showConfetti = true;
        setTimeout(() => (this.showConfetti = false), 3000);
        this.showSimpleMessage('Planning créé!', 'success');
        setTimeout(() => this.goBack(), 1500);
      },
      error: () => {
        this.showSimpleMessage('Erreur création', 'error');
        this.isSubmitting = false;
      }
    });
  }
}

  goBack(): void {
    if (this.sessionId) {
      this.router.navigate(['/sessions', this.sessionId]);
    } else {
      this.router.navigate(['/sessions']);
    }
  }

  // ==================== DISTRIBUTION FUNCTIONS ====================

  cancelDistribute(): void {
    this.showDistributionPreview = false;
    this.activeQuickAction = null;
  }

  confirmDistribute(): void {
    this.distributePlanningDemo();
  }

  applySelectedPlanning(planning: Planning): void {
    if (!planning) return;
    
    this.planningForm.patchValue({
      startDate: planning.startDate,
      endDate: planning.endDate,
      totalHours: planning.totalHours
    });
    
    this.generatedPlannings = [];
    
    setTimeout(() => {
      this.checkForConflict();
      this.generateSchedulePreview();
      this.showSimpleMessage('Planning appliqué', 'success');
    }, 100);
  }

  selectBestLocation(): void {
    if (this.bestLocation && this.bestLocation.id) {
      const location = this.locations.find(l => l.id === this.bestLocation.id);
      if (location) {
        this.selectLocation(location);
        this.showAnalytics = false;
        this.showSimpleMessage(`📍 Meilleur lieu sélectionné: ${location.name}`, 'success');
      }
    }
  }

  // ==================== ANALYTICS FUNCTIONS ====================

  calculateAverageOccupancy(): number {
    if (!this.busyDays || this.busyDays.length === 0) {
      return 0;
    }
    
    const occupancies = this.busyDays.map(day => {
      return day.occupancyRate || day.rate || day.occupancy || 0;
    }).filter(value => typeof value === 'number');
    
    if (occupancies.length === 0) {
      return 0;
    }
    
    const sum = occupancies.reduce((acc, val) => acc + val, 0);
    const average = sum / occupancies.length;
    
    return Math.round(average * 10) / 10;
  }

  calculateAverageOccupancyDetails(): { average: number; min: number; max: number; totalDays: number } {
    if (!this.busyDays || this.busyDays.length === 0) {
      return { average: 0, min: 0, max: 0, totalDays: 0 };
    }
    
    const occupancies = this.busyDays.map(day => {
      return day.occupancyRate || day.rate || day.occupancy || 0;
    }).filter(value => typeof value === 'number');
    
    if (occupancies.length === 0) {
      return { average: 0, min: 0, max: 0, totalDays: this.busyDays.length };
    }
    
    const sum = occupancies.reduce((acc, val) => acc + val, 0);
    const average = sum / occupancies.length;
    const min = Math.min(...occupancies);
    const max = Math.max(...occupancies);
    
    return {
      average: Math.round(average * 10) / 10,
      min: Math.round(min * 10) / 10,
      max: Math.round(max * 10) / 10,
      totalDays: this.busyDays.length
    };
  }

  // ==================== PLATFORM FUNCTIONS ====================

  /**
   * Méthode appelée quand l'utilisateur sélectionne une plateforme dans la liste
   */
  onPlatformSelect(event: any): void {
    const locationId = event.target.value;
    if (locationId) {
      const location = this.locations.find(l => l.id === Number(locationId));
      if (location) {
        this.selectedLocation = location;
        this.planningForm.patchValue({ 
          locationId: location.id,
          platformUrl: location.platformUrl 
        });
        this.showSimpleMessage(`Plateforme sélectionnée: ${location.name}`, 'success');
      }
    }
  }

  /**
   * Méthode appelée quand l'utilisateur saisit une URL manuellement
   */
  onPlatformUrlChange(): void {
    const url = this.platformUrlControl?.value;
    if (url && this.isValidUrl(url)) {
      this.createOrSelectOnlineLocation(url);
    }
  }

  /**
   * Valider l'URL
   */
  isValidUrl(url: string): boolean {
    const pattern = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$/;
    return pattern.test(url);
  }

  /**
   * Créer ou sélectionner une plateforme en ligne
   */
  createOrSelectOnlineLocation(url: string): void {
    const existingLocation = this.locations.find(l => 
      l.type === LocationType.ONLINE_PLATFORM && l.platformUrl === url
    );
    
    if (existingLocation) {
      this.selectedLocation = existingLocation;
      this.planningForm.patchValue({ locationId: existingLocation.id });
      this.showSimpleMessage('Plateforme existante sélectionnée', 'success');
    } else {
      const newLocation: Location = {
        name: 'Plateforme en ligne',
        type: LocationType.ONLINE_PLATFORM,
        platformUrl: url,
        capacity: 0,
        address: 'Online',
        city: 'Virtuel'
      };
      
      this.locationService.createLocation(newLocation).subscribe({
        next: (location) => {
          this.locations.push(location);
          this.selectedLocation = location;
          this.planningForm.patchValue({ locationId: location.id });
          this.showSimpleMessage('Nouvelle plateforme créée', 'success');
        },
        error: (err) => {
          console.error('Erreur création plateforme:', err);
          this.showSimpleMessage('Erreur lors de la création', 'error');
        }
      });
    }
  }

  // ==================== PREVIEW FUNCTIONS ====================

  /**
   * 📅 Génération avec prévisualisation avant confirmation
   */
  generatePlanningWithPreview(): void {
  if (!this.sessionId) {
    this.showSimpleMessage('❌ Session ID manquant', 'error');
    return;
  }

  this.isGenerating = true;
  this.activeQuickAction = 'magic';

  this.planningService.generatePlanning(this.sessionId).subscribe({
    next: (planning) => {
      console.log('✅ Planning généré:', planning);

      if (!planning) {
        this.showSimpleMessage('⚠️ Aucun planning généré', 'warning');
        this.resetGenerationState();
        return;
      }

      // ✅ Store preview
      this.previewPlannings = [planning];

      // ✅ Safe date parsing
      const start = new Date(planning.startDate);
      const end = new Date(planning.endDate);

      const totalDays =
        Math.floor((end.getTime() - start.getTime()) / (1000 * 3600 * 24)) + 1;

      // ✅ Safe location
      const locationName =
        planning.location?.name || this.selectedLocation?.name || 'Non défini';

      this.previewSummary = {
        totalDays,
        totalHours: planning.totalHours ?? 0,
        startDate: planning.startDate,
        endDate: planning.endDate,
        locations: [locationName]
      };

      // ✅ Show preview popup
      this.showPlanningPreview = true;

      this.resetGenerationState();
    },

    error: (err) => {
      console.error('❌ Erreur génération:', err);
      this.showSimpleMessage('Erreur lors de la génération', 'error');
      this.resetGenerationState();
    }
  });
}

  /**
   * ✅ Confirmer et appliquer le planning
   */
 confirmPlanning(): void {
  if (!this.previewPlannings.length) return;

  if (!this.sessionId) {
    this.showSimpleMessage('❌ Session ID manquant', 'error');
    return;
  }

  const planning = this.previewPlannings[0];

  const locationId = planning.location?.id || this.selectedLocation?.id;

  if (!locationId) {
    this.showSimpleMessage('❌ Lieu manquant', 'error');
    return;
  }

  this.planningService.createPlanning(
    planning,
    this.sessionId,   // ✅ now guaranteed number
    locationId
  ).subscribe({
    next: (savedPlanning) => {
      this.planningForm.patchValue({
        startDate: savedPlanning.startDate,
        endDate: savedPlanning.endDate
      });

      this.generatedPlannings = [savedPlanning];

      this.showConfetti = true;
      setTimeout(() => (this.showConfetti = false), 2000);

      this.showSimpleMessage(
        `✅ Planning sauvegardé: ${this.previewSummary.totalDays} jours, ${this.previewSummary.totalHours}h`,
        'success'
      );

      this.cancelPreview();
    },
    error: (err) => {
      console.error(err);
      this.showSimpleMessage('Erreur lors de la sauvegarde', 'error');
    }
  });
}
private resetGenerationState(): void {
  this.isGenerating = false;
  this.activeQuickAction = null;
}

  /**
   * ❌ Annuler la prévisualisation
   */
  cancelPreview(): void {
    this.showPlanningPreview = false;
    this.previewPlannings = [];
    this.previewSummary = {
      totalDays: 0,
      totalHours: 0,
      startDate: '',
      endDate: '',
      locations: []
    };
  }

  /**
   * Retourne une couleur pour chaque barre du graphique
   */
  getBarColor(index: number): string {
    const colors = [
      'linear-gradient(to top, #6366f1, #818cf8)', // Indigo
      'linear-gradient(to top, #10b981, #34d399)', // Vert
      'linear-gradient(to top, #f59e0b, #fbbf24)', // Orange
      'linear-gradient(to top, #ef4444, #f87171)', // Rouge
      'linear-gradient(to top, #8b5cf6, #a78bfa)'  // Violet
    ];
    return colors[index % colors.length];
  }

  // ==================== GETTERS ====================

  get platformUrlControl(): FormControl | null {
    return this.planningForm.get('platformUrl') as FormControl;
  }

  get isOnsiteMode(): boolean {
    return this.planningForm.get('mode')?.value === PlanningMode.ONSITE;
  }

  get isOnlineMode(): boolean {
    return this.planningForm.get('mode')?.value === PlanningMode.ONLINE;
  }

  get isHybridMode(): boolean {
    return this.planningForm.get('mode')?.value === PlanningMode.HYBRID;
  }

  getLocationSectionTitle(): string {
    if (this.isOnlineMode) return 'Plateforme en ligne';
    if (this.isHybridMode) return 'Lieu hybride';
    return 'Lieu de formation';
  }

  getModeLabel(mode: string): string {
    return PlanningModeLabels[mode as PlanningMode] || mode;
  }

  getModeIcon(mode: string): string {
    return PlanningModeIcons[mode as PlanningMode] || '📅';
  }

  getModeGradient(mode: string): string {
    const found = this.planningModes.find(m => m.value === mode);
    return found?.gradient || 'linear-gradient(135deg, #6366f1, #818cf8)';
  }

  getModeDescription(mode: string): string {
    switch(mode) {
      case PlanningMode.ONSITE:
        return 'Formation en présentiel';
      case PlanningMode.ONLINE:
        return 'Formation à distance';
      case PlanningMode.HYBRID:
        return 'Format mixte';
      default:
        return '';
    }
  }

  get conflictStatusClass(): string {
    if (this.checkingConflict) return 'checking';
    if (this.hasConflict) return 'conflict';
    if (this.selectedLocation && this.planningForm.get('startDate')?.value) return 'available';
    return '';
  }

  get conflictStatusText(): string {
    if (this.checkingConflict) return 'Vérification...';
    if (this.hasConflict) return 'Conflit';
    if (this.selectedLocation && this.planningForm.get('startDate')?.value) return 'Disponible';
    return 'Définir dates';
  }

  hasAnyData(): boolean {
    return !!(
      this.planningForm.get('mode')?.value ||
      this.planningForm.get('startDate')?.value ||
      this.planningForm.get('endDate')?.value ||
      this.planningForm.get('totalHours')?.value ||
      this.selectedLocation
    );
  }

  getCompletionPercentage(): number {
    let total = 0;
    let completed = 0;
    
    if (this.planningForm.get('mode')?.value) { total++; completed++; }
    if (this.planningForm.get('startDate')?.value) { total++; completed++; }
    if (this.planningForm.get('endDate')?.value) { total++; completed++; }
    if (this.planningForm.get('totalHours')?.value) { total++; completed++; }
    
    if (this.isOnsiteMode || this.isHybridMode) {
      total++;
      if (this.selectedLocation) completed++;
    }
    
    return Math.round((completed / total) * 100) || 0;
  }

  getLocationTypeIcon(type: LocationType): string {
    switch(type) {
      case LocationType.ROOM: return '📍';
      case LocationType.ONLINE_PLATFORM: return '🌐';
      case LocationType.HYBRID: return '🔄';
      default: return '📍';
    }
  }

  getLocationTypeLabel(type: LocationType): string {
    switch(type) {
      case LocationType.ROOM: return 'Salle';
      case LocationType.ONLINE_PLATFORM: return 'En ligne';
      case LocationType.HYBRID: return 'Hybride';
      default: return 'Lieu';
    }
  }

  getLocationTypeColor(type: LocationType): string {
    switch(type) {
      case LocationType.ROOM: return '#6366f1';
      case LocationType.ONLINE_PLATFORM: return '#10b981';
      case LocationType.HYBRID: return '#f59e0b';
      default: return '#6366f1';
    }
  }

  getCapacityBarColor(): string {
    switch(this.capacityStatus) {
      case 'ok': return '#10b981';
      case 'warning': return '#f59e0b';
      case 'error': return '#ef4444';
      default: return '#6b7280';
    }
  }

  getCapacityDisplayText(): string {
    if (!this.selectedLocation) return '';
    
    const participants = this.sessionParticipants;
    const capacity = this.selectedLocation.capacity;
    
    if (this.capacityStatus === 'error') {
      return `❌ ${participants} participants > capacité ${capacity}`;
    } else {
      return `${participants}/${capacity} places (${Math.round(this.capacityPercentage)}%)`;
    }
  }

  canSubmit(): boolean {
    return !this.planningForm.invalid && 
           !this.isSubmitting && 
           !this.hasConflict && 
           this.capacityStatus !== 'error';
  }
  /**
 * Regenerate planning with same mode
 */
regenerateWithMode(): void {
  const mode = this.planningForm.get('mode')?.value;
  if (mode) {
    this.cancelPreview();
    setTimeout(() => {
      this.generatePlanningDemo();
    }, 100);
  }
}
}