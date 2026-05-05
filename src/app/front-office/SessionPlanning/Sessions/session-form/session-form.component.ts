import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../services/session.service';
import { UserService } from '../../../services/user.service';
import { SessionStatus } from '../../models/session';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-session-form',
  templateUrl: './session-form.component.html',
  styleUrls: ['./session-form.component.scss'],
  providers: [UserService]
})
export class SessionFormComponent implements OnInit {
  sessionForm!: FormGroup;
  isEditMode: boolean = false;
  sessionId: number | null = null;
  isSubmitting: boolean = false;
  statusMessage: string = '';
  statusType: 'success' | 'error' | 'warning' = 'success';
  
  // Course and trainer
  courseId: number | null = null;
  courseName: string = '';
  trainerId: string | null = null;
  
  // Availability checks
  checkingAvailability: boolean = false;
  isTrainerAvailable: boolean = true;
  isOverloaded: boolean = false;

  // Date validation
  minDate: string = '';
  dateError: boolean = false;

  // Loading state
  isLoading: boolean = false;

  // Enum exposé pour le template
  SessionStatus = SessionStatus;

  // Mapping frontend -> backend
  private readonly backendStatusMap: Record<SessionStatus, string> = {
    [SessionStatus.PLANNED]: 'PLANNED',
    [SessionStatus.ONGOING]: 'ONGOING',
    [SessionStatus.COMPLETED]: 'COMPLETED',
    [SessionStatus.CANCELLED]: 'CANCELED'
  };

  // Mapping backend -> frontend
  private readonly frontendStatusMap: Record<string, SessionStatus> = {
    'PLANNED': SessionStatus.PLANNED,
    'ONGOING': SessionStatus.ONGOING,
    'COMPLETED': SessionStatus.COMPLETED,
    'CANCELED': SessionStatus.CANCELLED
  };

  constructor(
    private fb: FormBuilder,
    private sessionService: SessionService,
    private authService: UserService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    // Récupérer le courseId IMMÉDIATEMENT dans le constructeur
    this.courseId = this.getCourseIdFromAllSources();
    console.log('🔍 [Constructor] courseId récupéré:', this.courseId);
  }

  ngOnInit(): void {
    console.log('🚀 ngOnInit - Démarrage avec courseId:', this.courseId);
    
    this.initForm();
    this.getTrainerId();
    
    // Vérifier à nouveau le courseId depuis les queryParams
    this.route.queryParams.subscribe(params => {
      if (params['courseId']) {
        this.courseId = Number(params['courseId']);
        console.log('📌 [queryParams] courseId trouvé:', this.courseId);
      }
    });

    // Vérifier l'état de navigation
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state) {
      const state = navigation.extras.state as any;
      if (state['courseId']) {
        this.courseId = Number(state['courseId']);
        console.log('📌 [navigation state] courseId trouvé:', this.courseId);
      }
      if (state['courseName']) {
        this.courseName = state['courseName'];
        console.log('📌 [navigation state] courseName trouvé:', this.courseName);
      }
    }

    // Vérifier l'URL directement
    const urlCourseId = this.extractCourseIdFromUrl();
    if (urlCourseId && !this.courseId) {
      this.courseId = urlCourseId;
      console.log('📌 [URL] courseId extrait:', this.courseId);
    }

    this.checkEditMode();
    
    console.log('✅ ngOnInit - Final - courseId:', this.courseId, 'trainerId:', this.trainerId);
  }

  /**
   * Extrait le courseId de toutes les sources possibles
   */
  private getCourseIdFromAllSources(): number | null {
    // 1. QueryParams
    const queryParam = this.route.snapshot.queryParamMap.get('courseId');
    if (queryParam) {
      console.log('📥 Source: queryParams ->', queryParam);
      return Number(queryParam);
    }

    // 2. Navigation state
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state) {
      const state = navigation.extras.state as any;
      if (state['courseId']) {
        console.log('📥 Source: navigation state ->', state['courseId']);
        return Number(state['courseId']);
      }
    }

    // 3. URL segments
    const urlCourseId = this.extractCourseIdFromUrl();
    if (urlCourseId) {
      console.log('📥 Source: URL ->', urlCourseId);
      return urlCourseId;
    }

    // 4. Session storage (fallback)
    const storedCourseId = sessionStorage.getItem('lastCourseId');
    if (storedCourseId) {
      console.log('📥 Source: sessionStorage ->', storedCourseId);
      return Number(storedCourseId);
    }

    return null;
  }

  /**
   * Extrait le courseId de l'URL si présent
   */
  private extractCourseIdFromUrl(): number | null {
    const url = this.router.url;
    const match = url.match(/[?&]courseId=(\d+)/);
    if (match && match[1]) {
      return Number(match[1]);
    }
    
    // Chercher dans le chemin /courses/123/sessions/new
    const pathMatch = url.match(/\/courses\/(\d+)\/sessions/);
    if (pathMatch && pathMatch[1]) {
      return Number(pathMatch[1]);
    }
    
    return null;
  }

  initForm(): void {
    // Définir la date minimum comme aujourd'hui (format YYYY-MM-DD)
    const today = new Date();
    this.minDate = today.toISOString().split('T')[0];

    this.sessionForm = this.fb.group({
      status: [SessionStatus.PLANNED, Validators.required],
      maxParticipants: ['', [Validators.required, Validators.min(1), Validators.max(100)]],
      date: [this.minDate, [Validators.required, this.futureDateValidator.bind(this)]],
      attendance: this.fb.group({
        registeredCount: [0, [Validators.min(0)]],
        attendedCount: [0, [Validators.min(0)]]
      })
    });

    // Vérifier la date à chaque changement
    this.sessionForm.get('date')?.valueChanges.subscribe((date) => {
      this.validateSelectedDate(date);
      if (!this.isEditMode) {
        this.checkTrainerAvailabilityAndOverload();
      }
    });

    // Écouter les changements de maxParticipants
    this.sessionForm.get('maxParticipants')?.valueChanges.subscribe(value => {
      if (value > 100) {
        this.sessionForm.get('maxParticipants')?.setErrors({ max: true });
      }
    });
  }

  /**
   * Validateur personnalisé pour vérifier que la date n'est pas passée
   */
  futureDateValidator(control: any): { [key: string]: boolean } | null {
    if (!control.value) return null;
    
    const selectedDate = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (selectedDate < today) {
      return { 'pastDate': true };
    }
    return null;
  }

  /**
   * Valide la date sélectionnée et met à jour l'état d'erreur
   */
  validateSelectedDate(date: string): void {
    if (!date) {
      this.dateError = false;
      return;
    }

    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    this.dateError = selectedDate < today;
    
    if (this.dateError) {
      this.showStatus('Impossible de choisir une date passée', 'warning');
    }
  }

  /**
   * Vérifie si la date est valide (non passée)
   */
  isDateValid(): boolean {
    const date = this.sessionForm.get('date')?.value;
    if (!date) return false;
    
    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    return selectedDate >= today;
  }

  getTrainerId(): void {
    const user = this.authService.getUser();
    this.trainerId = user?.id || null;
    console.log('👤 Trainer ID récupéré:', this.trainerId);
    
    if (!this.trainerId) {
      this.showStatus('⚠️ Utilisateur non connecté', 'warning');
    }
  }

  checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.sessionId = +id;
      this.loadSessionData(this.sessionId);
    } else if (!this.courseId) {
      // Si on est en mode création et pas de courseId, afficher une erreur
      console.error('❌ Mode création sans courseId');
      this.showStatus('ID du cours manquant - Retour à la liste', 'error');
      setTimeout(() => this.router.navigate(['/sessions']), 2000);
    }
  }

  loadSessionData(id: number): void {
    this.isLoading = true;
    this.sessionService.getSessionById(id).subscribe({
      next: (session: any) => {
        const backendStatus = session.status as string;
        const frontendStatus = this.frontendStatusMap[backendStatus] || SessionStatus.PLANNED;
        
        // Récupérer le courseId de la session si disponible
        if (session.courseId) {
          this.courseId = session.courseId;
          console.log('📚 courseId récupéré depuis la session:', this.courseId);
          
          // Sauvegarder pour référence future
          sessionStorage.setItem('lastCourseId', String(this.courseId));
        }
        
        // Récupérer le nom du cours si disponible
        if (session.courseName) {
          this.courseName = session.courseName;
        }
        
        // Gérer la date de façon sécurisée
        let dateValue = this.minDate;
        
        if (session.createdAt) {
          const dateStr = String(session.createdAt);
          if (dateStr.includes('T')) {
            dateValue = dateStr.split('T')[0];
          } else {
            dateValue = dateStr;
          }
        }
        
        this.sessionForm.patchValue({
          status: frontendStatus,
          maxParticipants: session.maxParticipants,
          date: dateValue
        });
        
        this.validateSelectedDate(dateValue);
        this.isLoading = false;
      },
      error: (error) => {
        this.showStatus('Erreur lors du chargement de la session', 'error');
        console.error('Error loading session:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Vérifie la disponibilité ET la surcharge du formateur
   */
  checkTrainerAvailabilityAndOverload(): void {
    const date = this.sessionForm.get('date')?.value;
    
    if (!date || !this.trainerId || this.isEditMode || !this.isDateValid()) {
      this.isTrainerAvailable = true;
      this.isOverloaded = false;
      return;
    }
    
    this.checkingAvailability = true;
    
    forkJoin({
      overloaded: this.sessionService.checkTrainerOverload(this.trainerId, date).pipe(
        catchError(err => {
          console.error('Erreur check overload:', err);
          return of(false);
        })
      )
    }).subscribe({
      next: (results) => {
        this.isOverloaded = results.overloaded;
        this.checkingAvailability = false;
        
        if (results.overloaded) {
          this.showStatus('⚠️ Formateur serait surchargé (3+ sessions)', 'warning');
        } else {
          this.showStatus('✅ Formateur disponible', 'success');
        }
      },
      error: (err) => {
        console.error('Erreur vérification:', err);
        this.checkingAvailability = false;
        this.showStatus('Erreur de vérification', 'error');
      }
    });
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
    return labels[status] || 'Inconnu';
  }

  calculateAttendanceRate(): number {
    const registered = this.sessionForm.get('attendance')?.get('registeredCount')?.value || 0;
    const attended = this.sessionForm.get('attendance')?.get('attendedCount')?.value || 0;
    
    if (registered === 0) return 0;
    return Math.round((attended / registered) * 100);
  }

  cancelSession(): void {
    if (!this.sessionId) return;
    
    if (confirm('Êtes-vous sûr de vouloir annuler cette session ?')) {
      this.sessionService.cancelSession(this.sessionId).subscribe({
        next: () => {
          this.showStatus('Session annulée avec succès', 'success');
          setTimeout(() => this.navigateBack(), 1500);
        },
        error: (err) => {
          this.showStatus('Erreur lors de l\'annulation', 'error');
          console.error('Error cancelling session:', err);
        }
      });
    }
  }

  /**
   * Navigation de retour avec préservation du courseId
   */
  navigateBack(): void {
    if (this.courseId) {
      // Sauvegarder pour plus tard
      sessionStorage.setItem('lastCourseId', String(this.courseId));
      
      this.router.navigate(['/sessions'], { 
        queryParams: { courseId: this.courseId } 
      });
    } else {
      this.router.navigate(['/sessions']);
    }
  }

  onSubmit(): void {
    console.log('📤 Tentative de soumission avec courseId:', this.courseId);

    if (this.sessionForm.invalid) {
      this.markFormGroupTouched(this.sessionForm);
      this.showStatus('Veuillez corriger les erreurs dans le formulaire', 'error');
      return;
    }

    // Vérification STRICTE du courseId
    if (!this.courseId) {
      console.error('❌ ERREUR CRITIQUE: courseId est null ou undefined');
      console.log('URL actuelle:', this.router.url);
      console.log('QueryParams:', this.route.snapshot.queryParamMap.keys);
      console.log('State:', (this.router.getCurrentNavigation()?.extras?.state));
      
      this.showStatus('❌ ID du cours manquant - Veuillez réessayer depuis la liste des cours', 'error');
      
      // Proposer de retourner à la liste
      setTimeout(() => {
        if (confirm('Retourner à la liste des sessions ?')) {
          this.router.navigate(['/sessions']);
        }
      }, 1000);
      
      return;
    }

    // Vérification supplémentaire de la date
    if (!this.isDateValid()) {
      this.showStatus('Impossible de créer une session avec une date passée', 'error');
      return;
    }

    if (!this.isEditMode) {
      // La surcharge est un warning, pas un blocage
      if (this.isOverloaded) {
        if (!confirm('⚠️ Le formateur sera surchargé ce jour-là. Voulez-vous continuer ?')) {
          return;
        }
      }
    }

    this.isSubmitting = true;
    const formValue = this.sessionForm.value;

    const frontendStatus = formValue.status as SessionStatus;
    const backendStatus = this.backendStatusMap[frontendStatus];

    console.log('📦 Données à soumettre:', {
      date: formValue.date,
      status: backendStatus,
      courseId: this.courseId,
      trainerId: this.trainerId,
      maxParticipants: formValue.maxParticipants
    });

    const sessionData: any = {
      status: backendStatus,
      maxParticipants: formValue.maxParticipants,
      createdAt: formValue.date,
      courseId: this.courseId, // Utilisation du courseId stocké
      trainerId: this.trainerId
    };

    // Ajouter attendance si présent
    if (formValue.attendance && (formValue.attendance.registeredCount > 0 || formValue.attendance.attendedCount > 0)) {
      sessionData.attendance = {
        registeredCount: formValue.attendance.registeredCount ?? 0,
        attendedCount: formValue.attendance.attendedCount ?? 0
      };
    }

    if (this.isEditMode && this.sessionId) {
      this.sessionService.updateSession(this.sessionId, sessionData).subscribe({
        next: (response) => {
          console.log('✅ Session mise à jour:', response);
          this.showStatus('✅ Session mise à jour avec succès', 'success');
          setTimeout(() => this.navigateBack(), 1500);
        },
        error: (error) => {
          this.showStatus('❌ Erreur lors de la mise à jour', 'error');
          console.error('Update error:', error);
          this.isSubmitting = false;
        }
      });
    } else {
      this.sessionService.createSession(sessionData).subscribe({
        next: (response) => {
          console.log('✅ Session créée:', response);
          this.showStatus('✅ Session créée avec succès', 'success');
          
          // Sauvegarder le courseId pour la prochaine fois
          if (this.courseId) {
            sessionStorage.setItem('lastCourseId', String(this.courseId));
          }
          
          setTimeout(() => this.navigateBack(), 1500);
        },
        error: (error) => {
          this.showStatus('❌ Erreur lors de la création', 'error');
          console.error('Create error:', error);
          this.isSubmitting = false;
        }
      });
    }
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  showStatus(message: string, type: 'success' | 'error' | 'warning'): void {
    this.statusMessage = message;
    this.statusType = type;
    
    if (type === 'success') {
      setTimeout(() => {
        this.statusMessage = '';
      }, 3000);
    }
  }

  getAvailabilityTooltip(): string {
    if (this.checkingAvailability) return 'Vérification en cours...';
    if (!this.isTrainerAvailable) return '❌ Formateur non disponible';
    if (this.isOverloaded) return '⚠️ Formateur surchargé';
    return '✅ Formateur disponible';
  }

  getAvailabilityStatusText(): string {
    if (this.checkingAvailability) return 'Vérification...';
    if (!this.isTrainerAvailable) return 'Non disponible';
    if (this.isOverloaded) return 'Surcharge';
    return 'Disponible';
  }

  /**
   * Retourne la couleur pour l'affichage du statut de disponibilité
   */
  getAvailabilityColor(): string {
    if (!this.isTrainerAvailable) return '#ef4444'; // Rouge
    if (this.isOverloaded) return '#f59e0b'; // Orange
    return '#10b981'; // Vert
  }

  /**
   * Getter pour vérifier si le formulaire est prêt à être soumis
   */
  get canSubmit(): boolean {
    return this.sessionForm.valid && 
           !this.isSubmitting && 
           this.isDateValid() && 
           !!this.courseId; // Vérifie que courseId est présent
  }

  /**
   * Getter pour le titre du formulaire
   */
  get formTitle(): string {
    if (this.isEditMode) {
      return this.courseName ? `Modifier session - ${this.courseName}` : 'Modifier session';
    }
    return this.courseName ? `Nouvelle session - ${this.courseName}` : 'Nouvelle session';
  }
}