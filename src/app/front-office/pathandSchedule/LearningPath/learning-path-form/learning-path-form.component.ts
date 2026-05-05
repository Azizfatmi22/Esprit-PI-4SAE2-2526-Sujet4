import { Component, OnInit, OnDestroy, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LearningPathService } from '../../services/learning-path.service';
import { SessionService } from '../../../SessionPlanning/services/session.service';
import { PlanningService } from '../../../SessionPlanning/services/planning.service';
import { CourseService } from '../../../services/course.service';
import { UserService } from '../../../services/user.service';
import { LearningPath, LearningLevel, LearningPathStatus } from '../../models/learning-path.model';
import { forkJoin, of, Subject } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-learning-path-form',
  templateUrl: './learning-path-form.component.html',
  styleUrls: ['./learning-path-form.component.scss']
})
export class LearningPathFormComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('hoursChart') hoursChartRef!: ElementRef;
  @ViewChild('difficultyChart') difficultyChartRef!: ElementRef;
  @ViewChild('progressChart') progressChartRef!: ElementRef;
  
  pathForm!: FormGroup;
  isEditMode: boolean = false;
  pathId: number | null = null;
  isSubmitting: boolean = false;
  loading: boolean = false;
  
  // Step wizard
  currentStep: number = 1;
  totalSteps: number = 4;
  selectedLevel: string = '';
  
  // Loading states
  loadingCourses: boolean = false;
  loadingSessions: boolean = false;
  loadingAnalytics: boolean = false;
  loadingFormData: boolean = true;
  loadingPlannings: Map<number, boolean> = new Map();
  
  // Courses
  courses: any[] = [];
  selectedCourses: any[] = [];
  courseSearchTerm: string = '';
  filteredCourses: any[] = [];
  
  // Sessions with Planning data
  allSessionsByCourse: Map<number, any[]> = new Map();
  availableSessionsByCourse: Map<number, any[]> = new Map();
  selectedSessions: any[] = [];
  sessionSearchTerm: string = '';
  
  // Planning cache
  planningsCache: Map<number, any> = new Map();
  
  // User
  currentUserId: string | null = null;
  
  // UI State
  showSuccess: boolean = false;
  showError: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  showWarningAlert: boolean = false;
  warningMessage: string = '';
  
  // Analytics Data
  pathComplexity: any = null;
  completionPrediction: any = null;
  learningSummary: string = '';
  optimalOrder: any[] = [];
  recommendations: string[] = [];
  
  // Dashboard Data
  dashboardData: any = {
    totalHours: 0,
    totalSessions: 0,
    totalCourses: 0,
    avgHoursPerSession: 0,
    estimatedWeeks: 0,
    hoursPerWeek: 5,
    difficultyDistribution: { easy: 0, medium: 0, hard: 0 },
    modeDistribution: { ONSITE: 0, ONLINE: 0, HYBRID: 0 },
    weeklySchedule: [],
    monthlyDistribution: [],
    riskScore: 0,
    efficiencyScore: 0,
    balanceScore: 0
  };
  
  // Chart instances
  private hoursChart: any;
  private difficultyChart: any;
  private progressChart: any;
  
  // Progress tracking
  loadingProgress: number = 0;
  loadingMessage: string = '';
  
  private destroy$ = new Subject<void>();
  
  // Enums
  LearningLevel = LearningLevel;
  LearningPathStatus = LearningPathStatus;
  
  levelOptions = [
    { value: LearningLevel.BEGINNER, label: 'Débutant', description: 'Pour les novices sans expérience' },
    { value: LearningLevel.INTERMEDIATE, label: 'Intermédiaire', description: 'Connaissances de base requises' },
    { value: LearningLevel.ADVANCED, label: 'Avancé', description: 'Expérience préalable nécessaire' }
  ];
  
  statusOptions = [
    { value: 'DRAFT', label: 'Brouillon', description: 'En cours de création' },
    { value: 'ACTIVE', label: 'Actif', description: 'Disponible pour les apprenants' },
    { value: 'PUBLISHED', label: 'Publié', description: 'Publié et accessible' },
    { value: 'ARCHIVED', label: 'Archivé', description: 'Conservé pour référence' }
  ];

  constructor(
    private fb: FormBuilder,
    private learningPathService: LearningPathService,
    private sessionService: SessionService,
    private planningService: PlanningService,
    private courseService: CourseService,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.getCurrentUser();
    this.checkEditMode();
    this.loadCourses();
    // Force status to DRAFT initially if in creation mode
    if (!this.isEditMode) {
      this.pathForm.patchValue({ status: 'DRAFT' });
    }
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.destroyCharts();
  }

  initForm(): void {
    this.pathForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      level: [LearningLevel.BEGINNER, Validators.required],
      status: [{ value: 'DRAFT', disabled: false }, Validators.required],
      objectives: ['', [Validators.required, Validators.minLength(20)]]
    });
    
    this.pathForm.get('level')?.valueChanges.subscribe(level => {
      this.selectedLevel = level;
      if (this.currentStep === 2) {
        this.loadCoursesByLevel(level);
      }
    });
    
    // If status is changed to non-DRAFT, validate that all sessions have planning
    this.pathForm.get('status')?.valueChanges.subscribe(newStatus => {
      if (newStatus !== 'DRAFT' && this.hasMissingPlanning) {
        this.pathForm.patchValue({ status: 'DRAFT' }, { emitEvent: false });
        this.showWarningMessage('⚠️ Impossible de passer le statut à autre que BROUILLON car certaines sessions n\'ont pas de planning.');
      }
    });
  }

  getCurrentUser(): void {
    const user = this.userService.getUser();
    this.currentUserId = user?.id || null;
  }

  loadCourses(): void {
    this.loadingCourses = true;
    this.courseService.getAllCourses(0, 1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.courses = response?.content || (Array.isArray(response) ? response : []);
          this.filteredCourses = [...this.courses];
          this.loadingCourses = false;
          this.loadingFormData = false;
        },
        error: (err) => {
          console.error('Error loading courses:', err);
          this.loadingCourses = false;
          this.loadingFormData = false;
        }
      });
  }

  // Load planning for a single session by ID
  loadPlanningForSession(sessionId: number): void {
    if (!sessionId || this.planningsCache.has(sessionId)) return;
    
    this.loadingPlannings.set(sessionId, true);
    this.planningService.getPlanningBySessionId(sessionId).pipe(
      catchError(err => {
        console.warn(`No planning for session ${sessionId}:`, err);
        return of([]);
      })
    ).subscribe({
      next: (plannings) => {
        if (plannings && plannings.length > 0) {
          this.planningsCache.set(sessionId, plannings[0]);
        }
        this.loadingPlannings.set(sessionId, false);
      },
      error: (err) => {
        console.error(`Error loading planning for session ${sessionId}:`, err);
        this.loadingPlannings.set(sessionId, false);
      }
    });
  }

  // Load plannings for multiple sessions
  loadPlanningsForSessions(sessionIds: number[]): void {
    const validIds = sessionIds.filter(id => id !== null && id !== undefined);
    validIds.forEach(sessionId => {
      if (!this.planningsCache.has(sessionId)) {
        this.loadPlanningForSession(sessionId);
      }
    });
  }

  checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.pathId = +id;
      this.loadPathData(this.pathId);
    }
  }

  loadPathData(id: number): void {
    this.loading = true;
    this.learningPathService.getLearningPath(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (path) => {
          this.pathForm.patchValue({
            title: path.title,
            description: path.description,
            level: path.level,
            status: path.status,
            objectives: path.objectives
          });
          this.selectedLevel = path.level;
          
          if (path.sessionIds && path.sessionIds.length > 0) {
            this.loadSelectedSessionsWithPlanning(path.sessionIds);
          }
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading path:', err);
          this.loading = false;
        }
      });
  }

  loadSelectedSessionsWithPlanning(sessionIds: number[]): void {
    this.loadingSessions = true;
    
    // First load all plannings for the sessions
    this.loadPlanningsForSessions(sessionIds);
    
    // Wait a bit for plannings to load
    setTimeout(() => {
      this.sessionService.getAllSessions()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (sessions) => {
            const selectedSessionsData = sessions.filter(s => s.id !== undefined && sessionIds.includes(s.id));
            
            this.selectedSessions = selectedSessionsData.map(session => {
              const planning = session.id ? this.planningsCache.get(session.id) : undefined;
              return {
                ...session,
                planning: planning,
                totalHours: planning?.totalHours || 0,
                startDate: planning?.startDate,
                endDate: planning?.endDate,
                mode: planning?.mode,
                hasPlanning: planning !== undefined
              };
            });
            
            const courseIds = [...new Set(this.selectedSessions.map(s => s.courseId).filter(id => id !== undefined))];
            courseIds.forEach(courseId => {
              const course = this.courses.find(c => c.id === courseId);
              if (course && !this.isCourseSelected(courseId)) {
                this.selectedCourses.push(course);
              }
            });
            
            this.updateDashboardData();
            this.loadingSessions = false;
          },
          error: (err) => {
            console.error('Error loading sessions:', err);
            this.loadingSessions = false;
          }
        });
    }, 500);
  }

  // ==================== DASHBOARD DATA CALCULATION ====================
  
  updateDashboardData(): void {
    const totalHours = this.totalHours;
    const sessionCount = this.selectedSessions.length;
    const courseCount = this.selectedCourses.length;
    
    this.dashboardData.totalHours = totalHours;
    this.dashboardData.totalSessions = sessionCount;
    this.dashboardData.totalCourses = courseCount;
    this.dashboardData.avgHoursPerSession = sessionCount > 0 ? Math.round(totalHours / sessionCount * 10) / 10 : 0;
    this.dashboardData.estimatedWeeks = Math.ceil(totalHours / this.dashboardData.hoursPerWeek);
    
    // Difficulty distribution based on hours per session
    this.dashboardData.difficultyDistribution = {
      easy: this.selectedSessions.filter(s => (s.totalHours || 0) < 4).length,
      medium: this.selectedSessions.filter(s => (s.totalHours || 0) >= 4 && (s.totalHours || 0) <= 7).length,
      hard: this.selectedSessions.filter(s => (s.totalHours || 0) > 7).length
    };
    
    // Mode distribution
    this.dashboardData.modeDistribution = {
      ONSITE: this.selectedSessions.filter(s => s.mode === 'ONSITE').length,
      ONLINE: this.selectedSessions.filter(s => s.mode === 'ONLINE').length,
      HYBRID: this.selectedSessions.filter(s => s.mode === 'HYBRID').length
    };
    
    this.generateWeeklySchedule();
    this.generateMonthlyDistribution();
    this.calculateScores();
    
    // Force status to DRAFT if any session missing planning
    if (this.hasMissingPlanning && this.pathForm.get('status')?.value !== 'DRAFT') {
      this.pathForm.patchValue({ status: 'DRAFT' }, { emitEvent: false });
      this.showWarningMessage('⚠️ Le statut a été forcé à BROUILLON car certaines sessions n\'ont pas de planning.');
    }
    
    setTimeout(() => this.initCharts(), 100);
  }
  
  generateWeeklySchedule(): void {
    const weeks = [];
    const now = new Date();
    
    for (let i = 0; i < 8; i++) {
      const weekStart = new Date(now);
      weekStart.setDate(now.getDate() + (i * 7));
      const weekEnd = new Date(weekStart);
      weekEnd.setDate(weekStart.getDate() + 6);
      
      let weekHours = 0;
      this.selectedSessions.forEach(session => {
        if (session.startDate && session.endDate) {
          const sessionStart = new Date(session.startDate);
          const sessionEnd = new Date(session.endDate);
          if (sessionStart <= weekEnd && sessionEnd >= weekStart) {
            weekHours += session.totalHours || 0;
          }
        }
      });
      
      weeks.push({
        week: i + 1,
        label: `Semaine ${i + 1}`,
        hours: Math.min(weekHours, 40),
        maxHours: 40
      });
    }
    
    this.dashboardData.weeklySchedule = weeks;
  }
  
  generateMonthlyDistribution(): void {
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];
    const distribution = Array(12).fill(0);
    
    this.selectedSessions.forEach(session => {
      if (session.startDate) {
        const month = new Date(session.startDate).getMonth();
        distribution[month] += session.totalHours || 0;
      }
    });
    
    this.dashboardData.monthlyDistribution = distribution.map((hours, index) => ({
      month: months[index],
      hours: hours
    }));
  }
  
  calculateScores(): void {
    let riskScore = 0;
    if (this.totalHours > 60) riskScore += 30;
    if (this.totalHours > 80) riskScore += 20;
    if (this.selectedSessions.length > 15) riskScore += 20;
    if (this.selectedSessions.length > 20) riskScore += 10;
    if (this.dashboardData.avgHoursPerSession > 8) riskScore += 20;
    this.dashboardData.riskScore = Math.min(riskScore, 100);
    
    let efficiencyScore = 80;
    if (this.dashboardData.avgHoursPerSession > 8) efficiencyScore -= 15;
    if (this.dashboardData.avgHoursPerSession < 4) efficiencyScore -= 10;
    if (this.selectedSessions.length > 10) efficiencyScore -= 10;
    if (this.selectedCourses.length > 5) efficiencyScore -= 5;
    this.dashboardData.efficiencyScore = Math.max(Math.min(efficiencyScore, 100), 0);
    
    let balanceScore = 70;
    const modeValues = Object.values(this.dashboardData.modeDistribution) as number[];
    const modeCount = modeValues.filter(v => v > 0).length;
    if (modeCount === 3) balanceScore += 20;
    else if (modeCount === 2) balanceScore += 10;
    
    const difficultyBalance = Math.abs(this.dashboardData.difficultyDistribution.easy - this.dashboardData.difficultyDistribution.hard);
    if (difficultyBalance <= 2) balanceScore += 10;
    else if (difficultyBalance <= 4) balanceScore += 5;
    
    this.dashboardData.balanceScore = Math.min(balanceScore, 100);
  }
  
  initCharts(): void {
    this.destroyCharts();
    
    setTimeout(() => {
      if (this.hoursChartRef?.nativeElement && this.dashboardData.weeklySchedule?.length > 0) {
        const ctx = this.hoursChartRef.nativeElement.getContext('2d');
        if (ctx) {
          this.hoursChart = new Chart(ctx, {
            type: 'bar',
            data: {
              labels: this.dashboardData.weeklySchedule.map((w: any) => w.label),
              datasets: [{
                label: 'Heures par semaine',
                data: this.dashboardData.weeklySchedule.map((w: any) => w.hours),
                backgroundColor: 'rgba(99, 102, 241, 0.7)',
                borderColor: '#6366f1',
                borderWidth: 1,
                borderRadius: 8
              }, {
                label: 'Maximum recommandé',
                data: this.dashboardData.weeklySchedule.map((w: any) => w.maxHours),
                type: 'line',
                borderColor: '#ef4444',
                borderWidth: 2,
                borderDash: [5, 5],
                fill: false,
                pointRadius: 0
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { position: 'top' } },
              scales: { y: { beginAtZero: true, title: { display: true, text: 'Heures' } } }
            }
          });
        }
      }
      
      if (this.difficultyChartRef?.nativeElement) {
        const ctx = this.difficultyChartRef.nativeElement.getContext('2d');
        if (ctx) {
          this.difficultyChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
              labels: ['Facile (<4h)', 'Moyen (4-7h)', 'Intensif (>7h)'],
              datasets: [{
                data: [
                  this.dashboardData.difficultyDistribution.easy,
                  this.dashboardData.difficultyDistribution.medium,
                  this.dashboardData.difficultyDistribution.hard
                ],
                backgroundColor: ['#10b981', '#f59e0b', '#ef4444'],
                borderWidth: 0
              }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
          });
        }
      }
      
      if (this.progressChartRef?.nativeElement) {
        const ctx = this.progressChartRef.nativeElement.getContext('2d');
        if (ctx) {
          this.progressChart = new Chart(ctx, {
            type: 'pie',
            data: {
              labels: ['Présentiel', 'En ligne', 'Hybride'],
              datasets: [{
                data: [
                  this.dashboardData.modeDistribution.ONSITE,
                  this.dashboardData.modeDistribution.ONLINE,
                  this.dashboardData.modeDistribution.HYBRID
                ],
                backgroundColor: ['#6366f1', '#10b981', '#f59e0b'],
                borderWidth: 0
              }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
          });
        }
      }
    }, 100);
  }
  
  destroyCharts(): void {
    if (this.hoursChart) { this.hoursChart.destroy(); this.hoursChart = null; }
    if (this.difficultyChart) { this.difficultyChart.destroy(); this.difficultyChart = null; }
    if (this.progressChart) { this.progressChart.destroy(); this.progressChart = null; }
  }

  // ==================== STEP NAVIGATION ====================
  
  nextStep(): void {
    if (this.validateCurrentStep()) {
      if (this.currentStep < this.totalSteps) {
        this.currentStep++;
        this.loadStepData();
      }
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  validateCurrentStep(): boolean {
    switch(this.currentStep) {
      case 1:
        if (!this.pathForm.get('title')?.value || !this.pathForm.get('description')?.value) {
          this.showErrorMessage('Veuillez remplir le titre et la description');
          return false;
        }
        if (!this.pathForm.get('objectives')?.value) {
          this.showErrorMessage('Veuillez remplir les objectifs pédagogiques');
          return false;
        }
        return true;
      case 2:
        if (this.selectedCourses.length === 0) {
          this.showErrorMessage('Veuillez sélectionner au moins un cours');
          return false;
        }
        return true;
      case 3:
        if (this.selectedSessions.length === 0) {
          this.showErrorMessage('Veuillez sélectionner au moins une session');
          return false;
        }
        return true;
      default:
        return true;
    }
  }

  loadStepData(): void {
    switch(this.currentStep) {
      case 2:
        this.loadCoursesByLevel(this.selectedLevel);
        break;
      case 3:
        this.loadSessionsForSelectedCourses();
        break;
      case 4:
        this.analyzePath();
        break;
    }
  }

  showErrorMessage(message: string): void {
    this.errorMessage = message;
    this.showError = true;
    setTimeout(() => this.showError = false, 3000);
  }

  showWarningMessage(message: string): void {
    this.warningMessage = message;
    this.showWarningAlert = true;
    setTimeout(() => this.showWarningAlert = false, 4000);
  }

  // ==================== STEP 2: COURSES BY LEVEL ====================
  
  loadCoursesByLevel(level: string): void {
    this.loadingCourses = true;
    this.learningPathService.filterCoursesByLevel(level)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (courses) => {
          this.courses = courses;
          this.filteredCourses = courses;
          this.loadingCourses = false;
        },
        error: (err) => {
          console.error('Error loading courses:', err);
          this.loadingCourses = false;
        }
      });
  }

  toggleCourse(course: any): void {
    const index = this.selectedCourses.findIndex(c => c.id === course.id);
    if (index === -1) {
      this.selectedCourses.push(course);
      this.showSuccessMessage(`Cours "${course.title}" ajouté`);
    } else {
      this.selectedCourses.splice(index, 1);
      const sessionsToRemove = this.selectedSessions.filter(s => s.courseId === course.id);
      sessionsToRemove.forEach(s => {
        const sessionIndex = this.selectedSessions.findIndex(ss => ss.id === s.id);
        if (sessionIndex !== -1) {
          this.selectedSessions.splice(sessionIndex, 1);
        }
      });
      this.allSessionsByCourse.delete(course.id);
      this.availableSessionsByCourse.delete(course.id);
      this.updateDashboardData();
      this.showSuccessMessage(`Cours "${course.title}" retiré`);
    }
  }

  isCourseSelected(courseId: number): boolean {
    return this.selectedCourses.some(c => c.id === courseId);
  }

  filterCourses(): void {
    if (!this.courseSearchTerm) {
      this.filteredCourses = [...this.courses];
      return;
    }
    const term = this.courseSearchTerm.toLowerCase();
    this.filteredCourses = this.courses.filter(course => 
      course.title?.toLowerCase().includes(term) ||
      course.description?.toLowerCase().includes(term)
    );
  }

  // ==================== STEP 3: SESSIONS ====================
  
  loadSessionsForSelectedCourses(): void {
    this.loadingSessions = true;
    const courseIds = this.selectedCourses.map(c => c.id);
    
    const sessionRequests = courseIds.map(courseId => 
      this.sessionService.getSessionsByUserAndCourse(this.currentUserId!, courseId)
        .pipe(catchError(err => {
          console.error(`Error loading sessions for course ${courseId}:`, err);
          return of([]);
        }))
    );
    
    forkJoin(sessionRequests).subscribe({
      next: (sessionsArrays) => {
        const allSessions = sessionsArrays.flat();
        const validSessions = allSessions.filter(session => session.status === 'PLANNED');
        
        // Load plannings for all valid sessions
        const sessionIds = validSessions.map(s => s.id).filter(id => id !== undefined && id !== null);
        this.loadPlanningsForSessions(sessionIds);
        
        // Wait for plannings to load
        setTimeout(() => {
          const sessionsWithPlanning = validSessions.map(session => {
            const planning = session.id ? this.planningsCache.get(session.id) : undefined;
            return {
              ...session,
              planning: planning,
              totalHours: planning?.totalHours || 0,
              startDate: planning?.startDate,
              endDate: planning?.endDate,
              mode: planning?.mode,
              hasPlanning: planning !== undefined
            };
          });
          
          // Group by course
          sessionsWithPlanning.forEach(session => {
            const courseId = session.courseId;
            if (courseId !== undefined) {
              if (!this.allSessionsByCourse.has(courseId)) {
                this.allSessionsByCourse.set(courseId, []);
              }
              this.allSessionsByCourse.get(courseId)!.push(session);
              this.availableSessionsByCourse.set(courseId, [...this.allSessionsByCourse.get(courseId)!]);
            }
          });
          
          this.loadingSessions = false;
        }, 500);
      },
      error: (err) => {
        console.error('Error loading sessions:', err);
        this.loadingSessions = false;
      }
    });
  }

  getSessionsForCourse(courseId: number): any[] {
    return this.availableSessionsByCourse.get(courseId) || [];
  }

  addSession(session: any): void {
    if (!session || !session.id) return;
    
    if (!this.selectedSessions.find(s => s.id === session.id)) {
      // Check if session has planning
      if (!session.hasPlanning) {
        this.showWarningMessage(`⚠️ La session "${session.title}" n'a pas de planning. Le parcours restera en mode BROUILLON tant que des sessions sont sans planning.`);
      }
      
      this.selectedSessions.push(session);
      this.updateDashboardData();
      const hoursInfo = session.hasPlanning ? `(${session.totalHours}h)` : '(sans planning)';
      this.showSuccessMessage(`Session "${session.title}" ajoutée ${hoursInfo}`);
      
      // Force status to DRAFT if any missing planning
      if (this.hasMissingPlanning && this.pathForm.get('status')?.value !== 'DRAFT') {
        this.pathForm.patchValue({ status: 'DRAFT' }, { emitEvent: false });
        this.showWarningMessage('⚠️ Le statut a été forcé à BROUILLON car certaines sessions n\'ont pas de planning.');
      }
    }
  }

  removeSession(sessionId: number): void {
    const session = this.selectedSessions.find(s => s.id === sessionId);
    if (session) {
      this.selectedSessions = this.selectedSessions.filter(s => s.id !== sessionId);
      this.updateDashboardData();
      this.showSuccessMessage(`Session "${session.title}" retirée`);
      
      // If after removal there are no missing plannings, allow status change
      if (!this.hasMissingPlanning && this.pathForm.get('status')?.value === 'DRAFT') {
        // Optionally, you could set it back to original status, but better to keep DRAFT until user manually changes.
        // No auto change.
      }
    }
  }

  isSessionSelected(sessionId: number): boolean {
    return this.selectedSessions.some(s => s.id === sessionId);
  }

  filterAllSessions(): void {
    if (!this.sessionSearchTerm) {
      this.selectedCourses.forEach(course => {
        const sessions = this.allSessionsByCourse.get(course.id) || [];
        this.availableSessionsByCourse.set(course.id, [...sessions]);
      });
      return;
    }
    
    const term = this.sessionSearchTerm.toLowerCase();
    this.selectedCourses.forEach(course => {
      const sessions = this.allSessionsByCourse.get(course.id) || [];
      const filtered = sessions.filter(session => 
        session.title?.toLowerCase().includes(term) ||
        session.id?.toString().includes(term)
      );
      this.availableSessionsByCourse.set(course.id, filtered);
    });
  }

  // ==================== TOTAL HOURS ====================
  
  get totalHours(): number {
    return this.selectedSessions.reduce((sum, s) => sum + (s.totalHours || 0), 0);
  }

  // ==================== PLANNING VALIDATION ====================
  
  get hasMissingPlanning(): boolean {
    return this.selectedSessions.some(s => !s.hasPlanning);
  }

  // ==================== STEP 4: ANALYSIS ====================
  
  analyzePath(): void {
    this.loadingAnalytics = true;
    this.updateDashboardData();
    this.generateRecommendations();
    this.initCharts();
    this.loadingAnalytics = false;
  }

  generateRecommendations(): void {
    this.recommendations = [];
    const totalHours = this.totalHours;
    const sessionCount = this.selectedSessions.length;
    
    const sessionsWithoutPlanning = this.selectedSessions.filter(s => !s.hasPlanning);
    if (sessionsWithoutPlanning.length > 0) {
      this.recommendations.push(`⚠️ ${sessionsWithoutPlanning.length} session(s) sans planning défini - Le parcours ne peut pas être activé.`);
    }
    
    if (this.selectedCourses.length === 1) {
      this.recommendations.push('💡 Considérez ajouter plus de cours pour diversifier les compétences');
    }
    
    if (sessionCount < 3) {
      this.recommendations.push('📚 Ajoutez plus de sessions pour un parcours plus complet');
    }
    
    if (sessionCount > 10) {
      this.recommendations.push('⚠️ Beaucoup de sessions - envisagez de diviser en plusieurs parcours');
    }
    
    if (totalHours < 20) {
      this.recommendations.push('⏱️ Parcours court - idéal pour une introduction rapide');
    } else if (totalHours > 60) {
      this.recommendations.push('⚠️ Parcours long - risque de fatigue, prévoyez des pauses');
    } else if (totalHours >= 20 && totalHours <= 50) {
      this.recommendations.push('✅ Charge horaire équilibrée');
    }
    
    const avgHours = sessionCount > 0 ? totalHours / sessionCount : 0;
    if (avgHours > 8) {
      this.recommendations.push('⚠️ Sessions trop longues en moyenne (plus de 8h) - risque de surcharge');
    }
    
    if (this.dashboardData.riskScore > 70) {
      this.recommendations.push('🚨 Risque élevé détecté! Réduisez la charge horaire ou étalez sur plus de semaines');
    }
    
    if (this.dashboardData.efficiencyScore < 50) {
      this.recommendations.push('📉 Efficacité faible - essayez de regrouper les sessions par thème');
    }
  }

  // ==================== UTILITY METHODS ====================
  
  getLevelLabel(level: string): string {
    const map: any = { 'BEGINNER': 'Débutant', 'INTERMEDIATE': 'Intermédiaire', 'ADVANCED': 'Avancé' };
    return map[level] || level;
  }

  getStatusLabel(status: string): string {
    const map: any = { 'DRAFT': 'Brouillon', 'ACTIVE': 'Actif', 'PUBLISHED': 'Publié', 'ARCHIVED': 'Archivé' };
    return map[status] || status;
  }

  getLevelColor(level: string): string {
    const colors: any = { 'BEGINNER': '#10b981', 'INTERMEDIATE': '#f59e0b', 'ADVANCED': '#ef4444' };
    return colors[level] || '#6b7280';
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

  getStepClass(step: number): string {
    if (step < this.currentStep) return 'completed';
    if (step === this.currentStep) return 'active';
    return '';
  }

  getStepIcon(step: number): string {
    if (step < this.currentStep) return '✓';
    return step.toString();
  }

  getCourseName(courseId: number): string {
    const course = this.courses.find(c => c.id === courseId);
    return course?.title || course?.name || 'Cours non trouvé';
  }

  getRiskColor(score: number): string {
    if (score >= 70) return '#ef4444';
    if (score >= 40) return '#f59e0b';
    return '#10b981';
  }

  getScoreColor(score: number): string {
    if (score >= 70) return '#10b981';
    if (score >= 50) return '#f59e0b';
    return '#ef4444';
  }

  showSuccessMessage(message: string): void {
    this.successMessage = message;
    this.showSuccess = true;
    setTimeout(() => this.showSuccess = false, 2000);
  }

  onSubmit(): void {
    if (this.pathForm.invalid) {
      this.showErrorMessage('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (this.selectedCourses.length === 0) {
      this.showErrorMessage('Veuillez sélectionner au moins un cours');
      return;
    }

    if (this.selectedSessions.length === 0) {
      this.showErrorMessage('Veuillez ajouter au moins une session au parcours');
      return;
    }

    // If any session missing planning, force status to DRAFT and warn
    if (this.hasMissingPlanning) {
      this.pathForm.patchValue({ status: 'DRAFT' });
      this.showWarningMessage('⚠️ Impossible de publier ou activer le parcours car certaines sessions n\'ont pas de planning. Le statut reste BROUILLON.');
      // Optionally, you could prevent submission entirely or allow but with DRAFT.
      // We'll allow submission but status is forced to DRAFT.
    }

    this.isSubmitting = true;
    const formValue = this.pathForm.value;

    const pathData: LearningPath = {
      title: formValue.title,
      description: formValue.description,
      level: formValue.level,
      status: formValue.status, // Will be DRAFT if missing planning
      objectives: formValue.objectives,
      totalHours: this.totalHours,
      sessionIds: this.selectedSessions.map(s => s.id).filter(id => id !== undefined)
    };

    if (this.isEditMode && this.pathId) {
      this.learningPathService.updateLearningPath(this.pathId, pathData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.showSuccessMessage('Parcours mis à jour avec succès !');
            setTimeout(() => this.router.navigate(['/learning-paths', this.pathId]), 1500);
          },
          error: (err) => {
            console.error('Error updating path:', err);
            this.showErrorMessage('Erreur lors de la mise à jour');
            this.isSubmitting = false;
          }
        });
    } else {
      this.learningPathService.createLearningPath(pathData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (created) => {
            this.showSuccessMessage('Parcours créé avec succès !');
            setTimeout(() => this.router.navigate(['/learning-paths', created.id]), 1500);
          },
          error: (err) => {
            console.error('Error creating path:', err);
            this.showErrorMessage('Erreur lors de la création');
            this.isSubmitting = false;
          }
        });
    }
  }

  cancel(): void {
    this.router.navigate(['/learning-paths']);
  }
}