import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ScheduleService } from '../../services/scheduleService.service';
import { PlanningService } from '../../../SessionPlanning/services/planning.service';
import { Schedule, ScheduleStatus, ScheduleType } from '../../models/Schedule';
import { Planning } from '../../../SessionPlanning/models/planning';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-schedule-form',
  templateUrl: './schedule-form.component.html',
  styleUrls: ['./schedule-form.component.scss']
})
export class ScheduleFormComponent implements OnInit, OnDestroy {
  planningId: number | null = null;
  planning: Planning | null = null;
  planningStartDate: Date | null = null;
  planningEndDate: Date | null = null;
  
  scheduleForm!: FormGroup;
  schedules: Schedule[] = [];
  selectedDate: Date | null = null;
  selectedSchedule: Schedule | null = null;
  isEditing: boolean = false;
  isSubmitting: boolean = false;
  loading: boolean = false;
  showForm: boolean = false;
  
  // Advanced features state
  showAnalyticsModal: boolean = false;
  analyticsData: any = null;
  statisticsData: any = null;
  efficiencyData: any = null;
  conflictCheckResult: boolean | null = null;
  conflictingSchedules: Schedule[] = [];
  isCheckingConflict: boolean = false;
  isAutoScheduling: boolean = false;
  isGeneratingWeekly: boolean = false;
  isCreatingRecurring: boolean = false;
  isSplittingSession: boolean = false;
  isShiftingSessions: boolean = false;
  isDuplicating: boolean = false;
  
  // Calendar
  currentMonth: Date = new Date();
  calendarDays: Date[] = [];
  weeks: Date[][] = [];
  schedulesByDate: Map<string, Schedule[]> = new Map();
  
  // Time slots
  timeSlots: string[] = [];
  
  // Drawer state
  activeAction: string | null = null;
  advancedActionsMinimized: boolean = false;
  
  // Auto-schedule config
  autoConfig = { 
    days: 5, 
    durationH: 2, 
    startHour: '09:00', 
    endHour: '18:00', 
    activeDays: ['LUN', 'MAR', 'MER', 'JEU', 'VEN'] 
  };
  
  // Weekly generation config
  weeklyConfig = { 
    fromDate: '', 
    toDate: '', 
    startTime: '09:00', 
    endTime: '12:00', 
    activeDays: ['LUN', 'MER', 'VEN'] 
  };
  weeklyPreview: { message: string, availableDays: number, skippedDays: number, sessionsToCreate: any[] } | null = null;
  
  // Shift config
  shiftConfig = { fromDate: '', direction: 1, offsetHours: 2 };
  shiftPreview: string | null = null;
  
  // Suggest / available slots
  suggestConfig = { durationH: 2, maxCount: 5 };
  suggestedSlots: Date[] = [];
  selectedSuggestedSlot: Date | null = null;
  
  availableSlotsConfig = { durationH: 2, daysAhead: 14 };
  availableSlots: Date[] = [];
  
  // Export
  exportConfig = { format: 'ical', from: '', to: '' };
  
  // Conflict alternative slots
  alternativeSlots: Date[] = [];
  isLoadingAlternatives: boolean = false;
  
  // Modal states
  showSplitModal: boolean = false;
  showDuplicateModal: boolean = false;
  showRecurringModal: boolean = false;
  showOptimizeModal: boolean = false;
  
  // Split modal
  splitBreakMinutes: number = 30;
  
  // Duplicate modal
  duplicateNewTitle: string = '';
  duplicateNewDateTime: string = '';
  
  // Recurring modal
  recurringStartDate: string = '';
  recurringWeeks: number = 4;
  recurringSelectedDays: string[] = ['LUN', 'MER', 'VEN'];
  
  // Optimize modal
  optimizePreferredStart: string = '';
  optimizeSuggestedSlots: Date[] = [];
  
  // Helper property
  minDateTime: string = '';
  
  // Enums
  ScheduleStatus = ScheduleStatus;
  ScheduleType = ScheduleType;
  
  monthNames = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];
  
  weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  dayCodes = ['LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM', 'DIM'];
  
  weekDaysShort = [
    { name: 'Lun', code: 'LUN' },
    { name: 'Mar', code: 'MAR' },
    { name: 'Mer', code: 'MER' },
    { name: 'Jeu', code: 'JEU' },
    { name: 'Ven', code: 'VEN' },
    { name: 'Sam', code: 'SAM' },
    { name: 'Dim', code: 'DIM' }
  ];
  
  typeOptions = [
    { value: ScheduleType.LIVE, label: 'En direct', icon: '🎥', color: '#6366f1' },
    { value: ScheduleType.RECORDED, label: 'Enregistré', icon: '📹', color: '#10b981' },
    { value: ScheduleType.WORKSHOP, label: 'Atelier', icon: '🔧', color: '#f59e0b' }
  ];
  
  statusOptions = [
    { value: ScheduleStatus.PENDING, label: 'En attente', icon: '⏳', color: '#f59e0b', bg: '#fed7aa' },
    { value: ScheduleStatus.CONFIRMED, label: 'Confirmé', icon: '✅', color: '#10b981', bg: '#d1fae5' },
    { value: ScheduleStatus.ACTIVE, label: 'Actif', icon: '▶️', color: '#6366f1', bg: '#eef2ff' },
    { value: ScheduleStatus.CANCELLED, label: 'Annulé', icon: '❌', color: '#ef4444', bg: '#fee2e2' }
  ];
  
  private destroy$ = new Subject<void>();
  private conflictCheckTimeout: any;

  constructor(
    private fb: FormBuilder,
    private scheduleService: ScheduleService,
    private planningService: PlanningService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  successMessage: string = '';
  errorMessage: string = '';
  warningMessage: string = '';

  showSuccess(message: string) {
    this.successMessage = message;
    setTimeout(() => this.clearMessages(), 3000);
  }

  showError(message: string) {
    this.errorMessage = message;
    setTimeout(() => this.clearMessages(), 4000);
  }

  showWarning(message: string) {
    this.warningMessage = message;
    setTimeout(() => this.clearMessages(), 3000);
  }

  clearMessages() {
    this.successMessage = '';
    this.errorMessage = '';
    this.warningMessage = '';
  }

  ngOnInit(): void {
    this.initForm();
    this.generateTimeSlots();
    this.getRouteParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.conflictCheckTimeout) {
      clearTimeout(this.conflictCheckTimeout);
    }
  }

  initForm(): void {
    this.scheduleForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      notes: ['', [Validators.maxLength(500)]],
      startTime: ['', Validators.required],
      endTime: ['', Validators.required],
      type: [ScheduleType.LIVE, Validators.required],
      status: [ScheduleStatus.PENDING, Validators.required]
    });
  }

  generateTimeSlots(): void {
    const slots = [];
    for (let hour = 8; hour <= 20; hour++) {
      slots.push(`${hour.toString().padStart(2, '0')}:00`);
      slots.push(`${hour.toString().padStart(2, '0')}:30`);
    }
    this.timeSlots = slots;
  }

  getRouteParams(): void {
    this.route.queryParams.subscribe(params => {
      if (params['planningId']) {
        this.planningId = +params['planningId'];
        if (this.planningId) {
          this.loadPlanningData(this.planningId);
        }
      }
    });
    
    const scheduleId = this.route.snapshot.params['id'];
    if (scheduleId) {
      this.isEditing = true;
      this.loadScheduleById(+scheduleId);
    }
  }

  loadPlanningData(planningId: number): void {
    this.planningService.getPlanningById(planningId).subscribe({
      next: (planning) => {
        this.planning = planning;
        this.planningStartDate = planning.startDate ? new Date(planning.startDate) : null;
        this.planningEndDate = planning.endDate ? new Date(planning.endDate) : null;
        
        if (this.planningStartDate) {
          this.currentMonth = new Date(this.planningStartDate);
          this.weeklyConfig.fromDate = this.formatDateForInput(this.planningStartDate);
          this.weeklyConfig.toDate = this.formatDateForInput(this.planningEndDate || this.planningStartDate);
        }
        
        this.loadSchedules();
        this.generateCalendar();
      },
      error: (err) => console.error('Error loading planning:', err)
    });
  }

  loadScheduleById(id: number): void {
    this.scheduleService.getScheduleById(id).subscribe({
      next: (schedule) => {
        this.selectedSchedule = schedule;
        this.planningId = schedule.planningId ?? null;
        this.selectedDate = new Date(schedule.startTime);
        this.isEditing = true;
        this.showForm = true;
        this.populateFormForEdit(schedule);
        
        if (this.planningId) {
          this.loadPlanningData(this.planningId);
        }
      },
      error: (err) => console.error('Error loading schedule:', err)
    });
  }

  loadSchedules(): void {
    if (!this.planningId) return;
    
    this.loading = true;
    this.scheduleService.getSchedulesByPlanningId(this.planningId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (schedules) => {
          this.schedules = schedules;
          this.groupSchedulesByDate();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading schedules:', err);
          this.loading = false;
        }
      });
  }

  groupSchedulesByDate(): void {
    this.schedulesByDate.clear();
    this.schedules.forEach(schedule => {
      const dateKey = new Date(schedule.startTime).toDateString();
      if (!this.schedulesByDate.has(dateKey)) {
        this.schedulesByDate.set(dateKey, []);
      }
      this.schedulesByDate.get(dateKey)!.push(schedule);
    });
  }

  generateCalendar(): void {
    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();
    
    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);
    
    let firstDayWeekday = firstDayOfMonth.getDay();
    firstDayWeekday = firstDayWeekday === 0 ? 7 : firstDayWeekday;
    
    const daysFromPrevMonth = firstDayWeekday - 1;
    
    this.calendarDays = [];
    
    const prevMonth = new Date(year, month, 0);
    const prevMonthDays = prevMonth.getDate();
    for (let i = daysFromPrevMonth - 1; i >= 0; i--) {
      const date = new Date(year, month - 1, prevMonthDays - i);
      this.calendarDays.push(date);
    }
    
    for (let i = 1; i <= lastDayOfMonth.getDate(); i++) {
      this.calendarDays.push(new Date(year, month, i));
    }
    
    const remainingDays = 42 - this.calendarDays.length;
    for (let i = 1; i <= remainingDays; i++) {
      this.calendarDays.push(new Date(year, month + 1, i));
    }
    
    this.weeks = [];
    for (let i = 0; i < this.calendarDays.length; i += 7) {
      this.weeks.push(this.calendarDays.slice(i, i + 7));
    }
  }

  previousMonth(): void {
    this.currentMonth = new Date(
      this.currentMonth.getFullYear(),
      this.currentMonth.getMonth() - 1,
      1
    );
    this.generateCalendar();
  }

  nextMonth(): void {
    this.currentMonth = new Date(
      this.currentMonth.getFullYear(),
      this.currentMonth.getMonth() + 1,
      1
    );
    this.generateCalendar();
  }

  isCurrentMonth(date: Date): boolean {
    return date.getMonth() === this.currentMonth.getMonth();
  }

  isDateInPlanningRange(date: Date): boolean {
    if (!this.planningStartDate || !this.planningEndDate) return true;
    const planningStart = new Date(this.planningStartDate);
    const planningEnd = new Date(this.planningEndDate);
    planningStart.setHours(0, 0, 0, 0);
    planningEnd.setHours(0, 0, 0, 0);
    date.setHours(0, 0, 0, 0);
    return date >= planningStart && date <= planningEnd;
  }

  isDateSelectable(date: Date): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    date.setHours(0, 0, 0, 0);
    return this.isDateInPlanningRange(date) && date >= today;
  }

  isToday(date: Date): boolean {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  }

  selectDate(date: Date): void {
    if (!this.isDateSelectable(date)) {
      if (date < new Date()) {
        this.showWarning('Impossible d\'ajouter une séance à une date passée');
      } else if (!this.isDateInPlanningRange(date)) {
        this.showWarning('Cette date est en dehors de la période du planning');
      }
      return;
    }
    
    this.selectedDate = date;
    this.showForm = true;
    this.isEditing = false;
    this.selectedSchedule = null;
    this.resetFormForDate(date);
  }

  selectSchedule(schedule: Schedule, event: Event): void {
    event.stopPropagation();
    this.selectedSchedule = schedule;
    this.selectedDate = new Date(schedule.startTime);
    this.isEditing = true;
    this.showForm = true;
    this.populateFormForEdit(schedule);
  }

  resetFormForDate(date: Date): void {
    const startDateTime = new Date(date);
    startDateTime.setHours(9, 0, 0, 0);
    const endDateTime = new Date(startDateTime);
    endDateTime.setHours(12, 0, 0, 0);
    
    this.scheduleForm.reset({
      title: '',
      notes: '',
      startTime: this.formatDateTimeLocal(startDateTime),
      endTime: this.formatDateTimeLocal(endDateTime),
      type: ScheduleType.LIVE,
      status: ScheduleStatus.PENDING
    });
  }

  populateFormForEdit(schedule: Schedule): void {
    this.scheduleForm.patchValue({
      title: schedule.title,
      notes: schedule.notes,
      startTime: this.formatDateTimeLocal(new Date(schedule.startTime)),
      endTime: this.formatDateTimeLocal(new Date(schedule.endTime)),
      type: schedule.type,
      status: schedule.status
    });
  }

  formatDateTimeLocal(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  getDateTimeForSlot(date: Date, timeSlot: string): string {
    const [hours, minutes] = timeSlot.split(':');
    const newDate = new Date(date);
    newDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
    return this.formatDateTimeLocal(newDate);
  }

  cancelForm(): void {
    this.showForm = false;
    this.selectedDate = null;
    this.selectedSchedule = null;
    this.isEditing = false;
  }

  onSubmit(): void {
    if (this.scheduleForm.invalid) {
      this.markFormGroupTouched(this.scheduleForm);
      return;
    }
    
    if (!this.planningId) {
      this.showError('Planning ID manquant');
      return;
    }
    
    this.isSubmitting = true;
    const formValue = this.scheduleForm.value;
    
    const scheduleData: Schedule = {
      title: formValue.title,
      notes: formValue.notes,
      startTime: new Date(formValue.startTime),
      endTime: new Date(formValue.endTime),
      type: formValue.type,
      status: formValue.status,
      planningId: this.planningId
    };
    
    const request = this.isEditing && this.selectedSchedule?.id
      ? this.scheduleService.updateSchedule(this.selectedSchedule.id, scheduleData)
      : this.scheduleService.addScheduleToPlanning(this.planningId, scheduleData);
    
    request.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadSchedules();
          this.cancelForm();
          this.isSubmitting = false;
          this.showSuccess('Séance enregistrée avec succès');
        },
        error: (err) => {
          console.error('Error saving schedule:', err);
          this.isSubmitting = false;
          this.showError(err.error?.message || 'Erreur lors de l\'enregistrement');
        }
      });
  }

  deleteSchedule(scheduleId: number, event: Event): void {
    event.stopPropagation();
    if (confirm('Supprimer cette séance ?')) {
      this.scheduleService.deleteSchedule(scheduleId).subscribe({
        next: () => {
          this.loadSchedules();
          this.showSuccess('Séance supprimée');
        },
        error: (err) => console.error('Error deleting schedule:', err)
      });
    }
  }

  updateScheduleStatus(scheduleId: number, status: ScheduleStatus, event: Event): void {
    event.stopPropagation();
    this.scheduleService.updateScheduleStatus(scheduleId, status).subscribe({
      next: () => {
        this.loadSchedules();
        this.showSuccess(`Statut mis à jour: ${status}`);
      },
      error: (err) => console.error('Error updating status:', err)
    });
  }

  getSchedulesForDate(date: Date): Schedule[] {
    return this.schedulesByDate.get(date.toDateString()) || [];
  }

  formatTime(date: Date): string {
    return new Date(date).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  getTotalHours(): number {
    return this.schedules.reduce((sum, s) => {
      const duration = (new Date(s.endTime).getTime() - new Date(s.startTime).getTime()) / (1000 * 60 * 60);
      return sum + duration;
    }, 0);
  }

  getTotalDays(): number {
    if (!this.planningStartDate || !this.planningEndDate) return 0;
    const diff = this.planningEndDate.getTime() - this.planningStartDate.getTime();
    return Math.ceil(diff / (1000 * 60 * 60 * 24)) + 1;
  }

  getStatusColor(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.color || '#6b7280';
  }

  validateTimeRange(): void {
    const start = this.scheduleForm.get('startTime')?.value;
    const end = this.scheduleForm.get('endTime')?.value;
    
    if (start && end && new Date(start) >= new Date(end)) {
      this.scheduleForm.get('endTime')?.setErrors({ invalidRange: true });
    } else {
      this.scheduleForm.get('endTime')?.setErrors(null);
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

  goBack(): void {
    if (this.planningId) {
      this.router.navigate(['/plannings', this.planningId]);
    } else {
      this.router.navigate(['/plannings']);
    }
  }

  getMonthName(): string {
    return `${this.monthNames[this.currentMonth.getMonth()]} ${this.currentMonth.getFullYear()}`;
  }

  // ==================== HELPER METHODS ====================
  
  formatDateForInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getDayCode(date: Date): string {
    const days = ['DIM', 'LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM'];
    return days[date.getDay()];
  }

  isDateHasSchedule(date: Date): boolean {
    return this.schedulesByDate.has(date.toDateString());
  }

  updateMinDateTime(): void {
    const now = new Date();
    this.minDateTime = this.formatDateTimeLocal(now);
  }

  convertDayCode(dayCode: string): string {
    const mapping: { [key: string]: string } = {
      'LUN': 'MONDAY',
      'MAR': 'TUESDAY',
      'MER': 'WEDNESDAY',
      'JEU': 'THURSDAY',
      'VEN': 'FRIDAY',
      'SAM': 'SATURDAY',
      'DIM': 'SUNDAY'
    };
    return mapping[dayCode] || dayCode;
  }

  // ==================== DRAWER METHODS ====================
  
  openAction(action: string): void {
    this.activeAction = this.activeAction === action ? null : action;
    this.shiftPreview = null;
    this.suggestedSlots = [];
    this.availableSlots = [];
    this.weeklyPreview = null;
    
    if (action === 'analytics' && this.planningId) {
      this.loadAnalyticsData();
    }
  }

  closeAction(): void { 
    this.activeAction = null; 
  }

  toggleAdvancedActions(): void {
    this.advancedActionsMinimized = !this.advancedActionsMinimized;
  }

  loadAnalyticsData(): void {
    if (!this.planningId) return;
    
    this.scheduleService.getScheduleAnalytics(this.planningId).subscribe({
      next: (data) => { this.analyticsData = data; },
      error: () => this.showError('Erreur chargement analytics')
    });
    
    this.scheduleService.getScheduleStatistics(this.planningId).subscribe({
      next: (stats) => { this.statisticsData = stats; },
      error: () => this.showError('Erreur chargement statistiques')
    });
    
    this.scheduleService.getScheduleEfficiency(this.planningId).subscribe({
      next: (efficiency) => { this.efficiencyData = efficiency; },
      error: () => this.showError('Erreur chargement efficacité')
    });
  }

  // ==================== CALENDAR STATS ====================
  
  getSessionsThisMonth(): number {
    const currentMonth = this.currentMonth.getMonth();
    const currentYear = this.currentMonth.getFullYear();
    
    return this.schedules.filter(schedule => {
      const scheduleDate = new Date(schedule.startTime);
      return scheduleDate.getMonth() === currentMonth && 
             scheduleDate.getFullYear() === currentYear &&
             schedule.status !== ScheduleStatus.CANCELLED;
    }).length;
  }

  getSessionsNextMonth(): number {
    const nextMonth = new Date(this.currentMonth);
    nextMonth.setMonth(nextMonth.getMonth() + 1);
    const nextMonthIndex = nextMonth.getMonth();
    const nextYear = nextMonth.getFullYear();
    
    return this.schedules.filter(schedule => {
      const scheduleDate = new Date(schedule.startTime);
      return scheduleDate.getMonth() === nextMonthIndex && 
             scheduleDate.getFullYear() === nextYear &&
             schedule.status !== ScheduleStatus.CANCELLED;
    }).length;
  }

  getHoursThisMonth(): number {
    const currentMonth = this.currentMonth.getMonth();
    const currentYear = this.currentMonth.getFullYear();
    
    const totalMinutes = this.schedules
      .filter(schedule => {
        const scheduleDate = new Date(schedule.startTime);
        return scheduleDate.getMonth() === currentMonth && 
               scheduleDate.getFullYear() === currentYear &&
               schedule.status !== ScheduleStatus.CANCELLED;
      })
      .reduce((sum, schedule) => {
        const duration = (new Date(schedule.endTime).getTime() - new Date(schedule.startTime).getTime()) / (1000 * 60);
        return sum + duration;
      }, 0);
      
    return Math.round(totalMinutes / 60);
  }

  getScheduleDuration(): string {
    if (!this.selectedSchedule) return '';
    const start = new Date(this.selectedSchedule.startTime);
    const end = new Date(this.selectedSchedule.endTime);
    const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60);
    return `${hours}h`;
  }

  getScheduleDurationHours(): number {
    if (!this.selectedSchedule) return 2;
    const start = new Date(this.selectedSchedule.startTime);
    const end = new Date(this.selectedSchedule.endTime);
    return Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60));
  }

  canSplitSession(): boolean {
    if (!this.selectedSchedule) return false;
    const start = new Date(this.selectedSchedule.startTime);
    const end = new Date(this.selectedSchedule.endTime);
    const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60);
    return hours >= 4;
  }

  isScheduleToday(): boolean {
    if (!this.selectedSchedule) return false;
    const today = new Date().toDateString();
    const scheduleDate = new Date(this.selectedSchedule.startTime).toDateString();
    return today === scheduleDate;
  }

  // ==================== AUTO SCHEDULE ====================
  
  toggleAutoDay(d: string): void {
    const idx = this.autoConfig.activeDays.indexOf(d);
    idx > -1 ? this.autoConfig.activeDays.splice(idx, 1) : this.autoConfig.activeDays.push(d);
  }

  runAutoSchedule(): void {
    if (!this.planningId || !this.planningStartDate || !this.planningEndDate) {
      this.showError('Planning non trouvé');
      return;
    }
    
    this.isAutoScheduling = true;
    this.scheduleService.autoSchedule(this.planningId, this.autoConfig.days).subscribe({
      next: (schedules) => {
        const validSchedules = schedules.filter(s => {
          const startDate = new Date(s.startTime);
          return startDate >= this.planningStartDate! && startDate <= this.planningEndDate!;
        });
        this.loadSchedules();
        this.isAutoScheduling = false;
        this.showSuccess(`${validSchedules.length} séances planifiées automatiquement`);
        this.closeAction();
      },
      error: (err) => { 
        this.isAutoScheduling = false; 
        this.showError(err.error?.message || 'Erreur lors de l\'auto-planification'); 
      }
    });
  }

  // ==================== WEEKLY GENERATION ====================
  
  toggleWeeklyDay(d: string): void {
    const idx = this.weeklyConfig.activeDays.indexOf(d);
    idx > -1 ? this.weeklyConfig.activeDays.splice(idx, 1) : this.weeklyConfig.activeDays.push(d);
    this.previewWeekly();
  }

  previewWeekly(): void {
    if (!this.planningStartDate || !this.planningEndDate) {
      this.weeklyPreview = { message: 'Les dates du planning ne sont pas définies', availableDays: 0, skippedDays: 0, sessionsToCreate: [] };
      return;
    }
    
    const startDate = new Date(this.weeklyConfig.fromDate);
    const endDate = new Date(this.weeklyConfig.toDate);
    
    if (startDate < this.planningStartDate || endDate > this.planningEndDate || startDate > endDate) {
      return;
    }
    
    const existingDates = new Set(
      this.schedules.filter(s => s.status !== ScheduleStatus.CANCELLED).map(s => new Date(s.startTime).toDateString())
    );
    
    const sessionsToCreate: any[] = [];
    let currentDate = new Date(startDate);
    
    while (currentDate <= endDate) {
      const dayCode = this.getDayCode(currentDate);
      if (this.weeklyConfig.activeDays.includes(dayCode)) {
        const hasSchedule = existingDates.has(currentDate.toDateString());
        sessionsToCreate.push({
          date: new Date(currentDate),
          dayName: currentDate.toLocaleDateString('fr-FR', { weekday: 'long' }),
          formattedDate: this.formatDateForInput(currentDate),
          hasSchedule: hasSchedule,
          willCreate: !hasSchedule
        });
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    const availableDays = sessionsToCreate.filter(s => !s.hasSchedule).length;
    const skippedDays = sessionsToCreate.filter(s => s.hasSchedule).length;
    
    let message = availableDays === 0 
      ? `⚠️ Aucun jour disponible - tous les jours ont déjà des séances.`
      : `📅 ${availableDays} séances seront créées (${skippedDays} jour(s) ignoré). Horaire: ${this.weeklyConfig.startTime} - ${this.weeklyConfig.endTime}`;
    
    this.weeklyPreview = { message, availableDays, skippedDays, sessionsToCreate };
  }

  confirmWeekly(): void {
    if (!this.planningId || !this.weeklyPreview || this.weeklyPreview.availableDays === 0) {
      this.showWarning('Aucune nouvelle séance à générer');
      return;
    }
    
    const startDate = new Date(this.weeklyConfig.fromDate);
    const endDate = new Date(this.weeklyConfig.toDate);
    const [startHour, startMinute] = this.weeklyConfig.startTime.split(':');
    const [endHour, endMinute] = this.weeklyConfig.endTime.split(':');
    
    const existingDates = new Set(
      this.schedules.filter(s => s.status !== ScheduleStatus.CANCELLED).map(s => new Date(s.startTime).toDateString())
    );
    
    const sessionsToCreate: Schedule[] = [];
    let currentDate = new Date(startDate);
    
    while (currentDate <= endDate) {
      const dayCode = this.getDayCode(currentDate);
      if (this.weeklyConfig.activeDays.includes(dayCode) && !existingDates.has(currentDate.toDateString())) {
        const startDateTime = new Date(currentDate);
        startDateTime.setHours(parseInt(startHour), parseInt(startMinute), 0, 0);
        const endDateTime = new Date(currentDate);
        endDateTime.setHours(parseInt(endHour), parseInt(endMinute), 0, 0);
        
        sessionsToCreate.push({
          title: `Session du ${currentDate.toLocaleDateString('fr-FR')}`,
          notes: `Généré automatiquement`,
          startTime: startDateTime,
          endTime: endDateTime,
          type: ScheduleType.LIVE,
          status: ScheduleStatus.PENDING,
          planningId: this.planningId
        } as Schedule);
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    if (sessionsToCreate.length === 0) {
      this.showWarning('Aucune nouvelle séance à générer');
      return;
    }
    
    this.isGeneratingWeekly = true;
    this.scheduleService.bulkCreateSchedules(this.planningId, sessionsToCreate).subscribe({
      next: (created) => {
        this.loadSchedules();
        this.isGeneratingWeekly = false;
        this.weeklyPreview = null;
        this.showSuccess(`${created.length} séances générées`);
        this.closeAction();
      },
      error: (err) => {
        this.isGeneratingWeekly = false;
        this.showError(err.error?.message || 'Erreur lors de la génération');
      }
    });
  }

  // ==================== SHIFT SESSIONS ====================
  
  buildShiftPreview(): void {
    if (!this.shiftConfig.fromDate || !this.planningStartDate || !this.planningEndDate) {
      this.shiftPreview = null;
      return;
    }
    
    const fromDate = new Date(this.shiftConfig.fromDate);
    const dir = this.shiftConfig.direction > 0 ? 'avancées' : 'reculées';
    this.shiftPreview = `Les séances après ${this.formatDateTimeLocal(fromDate)} seront ${dir} de ${this.shiftConfig.offsetHours}h.`;
    
    if (this.shiftConfig.direction > 0) {
      this.shiftPreview += ` ⚠️ Vérifiez la fin du planning (${this.formatDateForInput(this.planningEndDate)}).`;
    } else {
      this.shiftPreview += ` ⚠️ Vérifiez le début du planning (${this.formatDateForInput(this.planningStartDate)}).`;
    }
  }

  confirmShift(): void {
    if (!this.planningId || !this.shiftConfig.fromDate) {
      this.showError('Paramètres manquants');
      return;
    }
    
    this.isShiftingSessions = true;
    this.scheduleService.shiftFutureSessions(
      this.planningId,
      new Date(this.shiftConfig.fromDate),
      this.shiftConfig.direction * this.shiftConfig.offsetHours
    ).subscribe({
      next: () => {
        this.loadSchedules();
        this.isShiftingSessions = false;
        this.showSuccess('Sessions décalées avec succès');
        this.closeAction();
      },
      error: (err) => { 
        this.isShiftingSessions = false; 
        this.showError(err.error?.message || 'Erreur lors du décalage'); 
      }
    });
  }

  // ==================== SUGGEST SLOTS ====================
  
  runSuggest(): void {
    if (!this.planningId || !this.planningStartDate || !this.planningEndDate) {
      this.showError('Planning non trouvé');
      return;
    }
    
    this.isLoadingAlternatives = true;
    this.scheduleService.getAlternativeSlots(this.planningId, this.suggestConfig.durationH, this.suggestConfig.maxCount).subscribe({
      next: (slots) => { 
        this.suggestedSlots = slots.map(s => new Date(s)).filter(slot => slot >= this.planningStartDate! && slot <= this.planningEndDate!);
        this.isLoadingAlternatives = false;
        if (this.suggestedSlots.length === 0) this.showWarning('Aucun créneau disponible');
      },
      error: (err) => {
        this.isLoadingAlternatives = false;
        this.showError(err.error?.message || 'Erreur lors de la recherche');
      }
    });
  }

  applySuggestedSlot(slot: Date): void {
    if ((this.planningStartDate && slot < this.planningStartDate) || (this.planningEndDate && slot > this.planningEndDate)) {
      this.showWarning('Ce créneau est en dehors du planning');
      return;
    }
    
    const end = new Date(slot.getTime() + (this.suggestConfig.durationH * 3600000));
    if (this.planningEndDate && end > this.planningEndDate) {
      this.showWarning('La séance dépasserait la fin du planning');
      return;
    }
    
    this.scheduleForm.patchValue({
      startTime: this.formatDateTimeLocal(slot),
      endTime: this.formatDateTimeLocal(end)
    });
    this.showSuccess('Créneau appliqué');
    this.checkConflicts();
    this.closeAction();
  }

  // ==================== AVAILABLE SLOTS ====================
  
  runAvailableSlots(): void {
    if (!this.planningId || !this.planningStartDate || !this.planningEndDate) {
      this.showError('Planning non trouvé');
      return;
    }
    
    this.loading = true;
    const daysAhead = Math.ceil((this.planningEndDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
    
    this.scheduleService.getAvailableTimeSlots(this.planningId, this.availableSlotsConfig.durationH, Math.min(daysAhead, 30)).subscribe({
      next: (slots) => { 
        this.availableSlots = slots.map(s => new Date(s)).filter(slot => slot >= this.planningStartDate! && slot <= this.planningEndDate!);
        this.loading = false;
        if (this.availableSlots.length === 0) this.showWarning('Aucun créneau disponible');
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.error?.message || 'Erreur lors de la recherche');
      }
    });
  }

  applyAvailableSlot(slot: Date): void {
    if ((this.planningStartDate && slot < this.planningStartDate) || (this.planningEndDate && slot > this.planningEndDate)) {
      this.showWarning('Ce créneau est en dehors du planning');
      return;
    }
    
    const end = new Date(slot.getTime() + (this.availableSlotsConfig.durationH * 3600000));
    if (this.planningEndDate && end > this.planningEndDate) {
      this.showWarning('La séance dépasserait la fin du planning');
      return;
    }
    
    this.scheduleForm.patchValue({
      startTime: this.formatDateTimeLocal(slot),
      endTime: this.formatDateTimeLocal(end)
    });
    this.showSuccess('Créneau appliqué');
    this.checkConflicts();
    this.closeAction();
  }

  // ==================== EXPORT ====================
  
  runExport(): void {
    if (!this.planningId) return;
    
    const fromDate = this.exportConfig.from ? new Date(this.exportConfig.from) : this.planningStartDate;
    const toDate = this.exportConfig.to ? new Date(this.exportConfig.to) : this.planningEndDate;
    
    if (this.exportConfig.format === 'ical') {
      this.scheduleService.exportToIcal(this.planningId).subscribe({
        next: (data) => {
          const blob = new Blob([data], { type: 'text/calendar' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a'); 
          a.href = url;
          a.download = `planning_${this.planningId}.ics`;
          a.click();
          URL.revokeObjectURL(url);
          this.showSuccess('Export iCal réussi');
          this.closeAction();
        },
        error: () => this.showError('Erreur lors de l\'export')
      });
    } else if (this.exportConfig.format === 'csv') {
      this.scheduleService.getSchedulesByPlanningId(this.planningId).subscribe({
        next: (allSchedules) => {
          const schedules = allSchedules.filter(s => {
            const startDate = new Date(s.startTime);
            return startDate >= fromDate! && startDate <= toDate!;
          });
          
          const csvRows = [['Titre', 'Date', 'Début', 'Fin', 'Type', 'Statut', 'Notes']];
          schedules.forEach(s => {
            csvRows.push([
              s.title,
              new Date(s.startTime).toLocaleDateString('fr-FR'),
              new Date(s.startTime).toLocaleTimeString('fr-FR'),
              new Date(s.endTime).toLocaleTimeString('fr-FR'),
              s.type,
              s.status,
              s.notes || ''
            ]);
          });
          const csvContent = csvRows.map(row => row.join(';')).join('\n');
          const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `planning_${this.planningId}.csv`;
          a.click();
          URL.revokeObjectURL(url);
          this.showSuccess(`Export CSV réussi (${schedules.length} séances)`);
          this.closeAction();
        },
        error: () => this.showError('Erreur lors de l\'export CSV')
      });
    }
  }

  // ==================== CONFLICT METHODS ====================
  
  checkConflicts(): void {
    if (!this.planningId) return;
    const start = this.scheduleForm.get('startTime')?.value;
    const end = this.scheduleForm.get('endTime')?.value;
    if (!start || !end || new Date(start) >= new Date(end)) return;

    this.isCheckingConflict = true;
    this.conflictCheckResult = null;

    this.scheduleService.findConflicts(this.planningId, new Date(start), new Date(end))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (conflicts) => {
          this.conflictingSchedules = conflicts;
          this.conflictCheckResult = conflicts.length > 0;
          this.isCheckingConflict = false;
          if (conflicts.length > 0) {
            this.showWarning(`${conflicts.length} conflit(s) détecté(s)`);
            this.loadAlternativeSlotsInline();
          }
        },
        error: () => {
          this.isCheckingConflict = false;
          this.showError('Impossible de vérifier les conflits');
        }
      });
  }

  loadAlternativeSlotsInline(): void {
    if (!this.planningId) return;
    const start = this.scheduleForm.get('startTime')?.value;
    const end = this.scheduleForm.get('endTime')?.value;
    if (!start || !end) return;

    const durationHours = Math.round((new Date(end).getTime() - new Date(start).getTime()) / (1000 * 60 * 60));
    this.isLoadingAlternatives = true;
    this.scheduleService.getAlternativeSlots(this.planningId, durationHours, 3)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (slots) => {
          this.alternativeSlots = slots.map(s => new Date(s));
          this.isLoadingAlternatives = false;
        },
        error: () => { this.isLoadingAlternatives = false; }
      });
  }

  applyAlternativeSlot(slot: Date): void {
    const start = this.scheduleForm.get('startTime')?.value;
    const end = this.scheduleForm.get('endTime')?.value;
    const duration = new Date(end).getTime() - new Date(start).getTime();
    const newEnd = new Date(slot.getTime() + duration);

    this.scheduleForm.patchValue({
      startTime: this.formatDateTimeLocal(slot),
      endTime: this.formatDateTimeLocal(newEnd)
    });

    this.conflictCheckResult = null;
    this.conflictingSchedules = [];
    this.alternativeSlots = [];
    this.checkConflicts();
  }

  checkConflictWithBuffer(): void {
    if (!this.planningId) return;
    const start = this.scheduleForm.get('startTime')?.value;
    const end = this.scheduleForm.get('endTime')?.value;
    if (!start || !end) {
      this.showWarning('Sélectionnez d\'abord une plage horaire');
      return;
    }
    this.isCheckingConflict = true;
    this.scheduleService.checkConflictWithBuffer(this.planningId, new Date(start), new Date(end))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (hasConflict) => {
          this.isCheckingConflict = false;
          if (hasConflict) {
            this.showWarning('Conflit dans la zone tampon (±30 min)');
          } else {
            this.showSuccess('Aucun conflit avec la zone tampon');
          }
        },
        error: () => {
          this.isCheckingConflict = false;
          this.showError('Erreur lors de la vérification');
        }
      });
  }

  onTimeChange(): void {
    this.validateTimeRange();
    this.conflictCheckResult = null;
    this.conflictingSchedules = [];
    clearTimeout(this.conflictCheckTimeout);
    this.conflictCheckTimeout = setTimeout(() => this.checkConflicts(), 600);
  }

  // ==================== SPLIT MODAL ====================

  openSplitModal(): void {
    if (!this.canSplitSession()) {
      this.showWarning('La session doit durer au moins 4 heures');
      return;
    }
    if (!this.isSplitWithinPlanningRange()) {
      this.showWarning('Les parties divisées dépasseraient la période du planning');
      return;
    }
    this.showSplitModal = true;
    this.splitBreakMinutes = 30;
  }

  closeSplitModal(): void {
    this.showSplitModal = false;
  }

  isSplitWithinPlanningRange(): boolean {
    if (!this.selectedSchedule || !this.planningStartDate || !this.planningEndDate) return false;
    const start = new Date(this.selectedSchedule.startTime);
    const end = new Date(this.selectedSchedule.endTime);
    const duration = (end.getTime() - start.getTime()) / 2;
    const part1End = new Date(start.getTime() + duration);
    const part2Start = new Date(start.getTime() + duration + (this.splitBreakMinutes * 60000));
    return part1End <= this.planningEndDate && part2Start >= this.planningStartDate;
  }

  getSplitPart1Time(): string {
    if (!this.selectedSchedule) return '';
    const start = new Date(this.selectedSchedule.startTime);
    const end = new Date(this.selectedSchedule.endTime);
    const duration = (end.getTime() - start.getTime()) / 2;
    const part1End = new Date(start.getTime() + duration);
    return `${this.formatTime(start)} - ${this.formatTime(part1End)}`;
  }

  getSplitPart2Time(): string {
    if (!this.selectedSchedule) return '';
    const start = new Date(this.selectedSchedule.startTime);
    const end = new Date(this.selectedSchedule.endTime);
    const duration = (end.getTime() - start.getTime()) / 2;
    const part2Start = new Date(start.getTime() + duration + (this.splitBreakMinutes * 60000));
    return `${this.formatTime(part2Start)} - ${this.formatTime(end)}`;
  }

  confirmSplit(): void {
    if (!this.selectedSchedule || !this.isSplitWithinPlanningRange()) {
      this.showError('Impossible de diviser la séance');
      this.closeSplitModal();
      return;
    }
    
    this.isSplittingSession = true;
    this.scheduleService.splitSession(this.selectedSchedule.id!, this.splitBreakMinutes).subscribe({
      next: (sessions) => {
        this.loadSchedules();
        this.isSplittingSession = false;
        this.closeSplitModal();
        this.showSuccess(`Séance divisée en ${sessions.length} parties`);
      },
      error: (err) => {
        this.isSplittingSession = false;
        this.showError(err.error?.message || 'Erreur lors de la division');
      }
    });
  }

  // ==================== DUPLICATE MODAL ====================

  openDuplicateModal(): void {
    if (!this.selectedSchedule) return;
    
    this.showDuplicateModal = true;
    this.duplicateNewTitle = `${this.selectedSchedule.title} (Copie)`;
    
    const defaultDate = new Date(this.selectedSchedule.startTime);
    defaultDate.setDate(defaultDate.getDate() + 1);
    
    if (this.planningEndDate && defaultDate > this.planningEndDate) {
      defaultDate.setTime(this.planningEndDate.getTime());
    }
    if (this.planningStartDate && defaultDate < this.planningStartDate) {
      defaultDate.setTime(this.planningStartDate.getTime());
    }
    
    this.duplicateNewDateTime = this.formatDateTimeLocal(defaultDate);
    this.updateMinDateTime();
  }

  closeDuplicateModal(): void {
    this.showDuplicateModal = false;
    this.duplicateNewTitle = '';
    this.duplicateNewDateTime = '';
  }

  confirmDuplicate(): void {
    if (!this.selectedSchedule || !this.duplicateNewDateTime) return;
    
    const newDate = new Date(this.duplicateNewDateTime);
    const endTime = new Date(this.selectedSchedule.endTime);
    const duration = endTime.getTime() - new Date(this.selectedSchedule.startTime).getTime();
    const newEndTime = new Date(newDate.getTime() + duration);
    
    if (this.planningStartDate && newDate < this.planningStartDate) {
      this.showError(`Date avant le début du planning`);
      return;
    }
    if (this.planningEndDate && newEndTime > this.planningEndDate) {
      this.showError(`La séance dépasserait la fin du planning`);
      return;
    }
    
    this.isDuplicating = true;
    this.scheduleService.duplicateSchedule(this.selectedSchedule.id!, newDate).subscribe({
      next: (duplicate) => {
        this.loadSchedules();
        this.isDuplicating = false;
        this.closeDuplicateModal();
        this.showSuccess(`Séance dupliquée: ${duplicate.title}`);
      },
      error: (err) => {
        this.isDuplicating = false;
        this.showError(err.error?.message || 'Erreur lors de la duplication');
      }
    });
  }

  // ==================== RECURRING MODAL ====================

  openRecurringModal(): void {
    if (!this.selectedSchedule) return;
    
    this.showRecurringModal = true;
    const defaultStart = new Date();
    defaultStart.setDate(defaultStart.getDate() + 1);
    
    if (this.planningStartDate && defaultStart < this.planningStartDate) {
      this.recurringStartDate = this.formatDateForInput(this.planningStartDate);
    } else if (this.planningEndDate && defaultStart > this.planningEndDate) {
      this.recurringStartDate = this.formatDateForInput(this.planningEndDate);
    } else {
      this.recurringStartDate = this.formatDateForInput(defaultStart);
    }
    
    this.recurringWeeks = 4;
    this.recurringSelectedDays = ['LUN', 'MER', 'VEN'];
  }

  closeRecurringModal(): void {
    this.showRecurringModal = false;
  }

  isDaySelected(dayCode: string): boolean {
    return this.recurringSelectedDays.includes(dayCode);
  }

  toggleRecurringDay(dayCode: string): void {
    const index = this.recurringSelectedDays.indexOf(dayCode);
    if (index > -1) {
      this.recurringSelectedDays.splice(index, 1);
    } else {
      this.recurringSelectedDays.push(dayCode);
    }
  }

  getRecurringPreviewDates(): Date[] {
    if (!this.recurringStartDate || this.recurringSelectedDays.length === 0 || !this.planningStartDate || !this.planningEndDate) return [];
    
    const dates: Date[] = [];
    const startDate = new Date(this.recurringStartDate);
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + (this.recurringWeeks * 7));
    
    const actualEndDate = endDate < this.planningEndDate ? endDate : this.planningEndDate;
    const actualStartDate = startDate > this.planningStartDate ? startDate : this.planningStartDate;
    
    let currentDate = new Date(actualStartDate);
    
    while (currentDate <= actualEndDate) {
      const dayCode = this.getDayCode(currentDate);
      if (this.recurringSelectedDays.includes(dayCode)) {
        const hasExistingSchedule = this.schedules.some(s => 
          new Date(s.startTime).toDateString() === currentDate.toDateString() && 
          s.status !== ScheduleStatus.CANCELLED
        );
        if (!hasExistingSchedule) {
          dates.push(new Date(currentDate));
        }
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    return dates;
  }

  confirmRecurring(): void {
    if (!this.planningId || !this.selectedSchedule) return;
    
    const previewDates = this.getRecurringPreviewDates();
    if (previewDates.length === 0) {
      this.showWarning('Aucune date disponible');
      return;
    }
    
    const days = this.recurringSelectedDays.map(day => this.convertDayCode(day));
    
    this.isCreatingRecurring = true;
    this.scheduleService.createRecurringSessions(
      this.planningId, this.selectedSchedule, new Date(this.recurringStartDate), this.recurringWeeks, days
    ).subscribe({
      next: (sessions) => {
        const validSessions = sessions.filter(s => {
          const startDate = new Date(s.startTime);
          return startDate >= this.planningStartDate! && startDate <= this.planningEndDate!;
        });
        this.loadSchedules();
        this.isCreatingRecurring = false;
        this.closeRecurringModal();
        this.showSuccess(`${validSessions.length} sessions récurrentes créées`);
      },
      error: (err) => {
        this.isCreatingRecurring = false;
        this.showError(err.error?.message || 'Erreur lors de la création');
      }
    });
  }

  // ==================== OPTIMIZE MODAL ====================

  openOptimizeModal(): void {
    if (!this.selectedSchedule) return;
    
    this.showOptimizeModal = true;
    let preferredDate = new Date(this.selectedSchedule.startTime);
    
    if (this.planningStartDate && preferredDate < this.planningStartDate) {
      preferredDate = new Date(this.planningStartDate);
    }
    if (this.planningEndDate && preferredDate > this.planningEndDate) {
      preferredDate = new Date(this.planningEndDate);
    }
    
    this.optimizePreferredStart = this.formatDateTimeLocal(preferredDate);
    this.optimizeSuggestedSlots = [];
    this.loadOptimizeSuggestions();
    this.updateMinDateTime();
  }

  closeOptimizeModal(): void {
    this.showOptimizeModal = false;
    this.optimizePreferredStart = '';
    this.optimizeSuggestedSlots = [];
  }

  loadOptimizeSuggestions(): void {
    if (!this.planningId || !this.selectedSchedule) return;
    
    const duration = this.getScheduleDurationHours();
    this.scheduleService.getAlternativeSlots(this.planningId, duration, 5).subscribe({
      next: (slots) => {
        this.optimizeSuggestedSlots = slots.map(s => new Date(s)).filter(slot => {
          const endTime = new Date(slot.getTime() + (duration * 3600000));
          return slot >= this.planningStartDate! && endTime <= this.planningEndDate!;
        });
      },
      error: (err) => console.error(err)
    });
  }

  selectOptimizeSlot(slot: Date): void {
    const endTime = new Date(slot.getTime() + (this.getScheduleDurationHours() * 3600000));
    if ((this.planningStartDate && slot < this.planningStartDate) || (this.planningEndDate && endTime > this.planningEndDate)) {
      this.showWarning('Ce créneau est en dehors du planning');
      return;
    }
    this.optimizePreferredStart = this.formatDateTimeLocal(slot);
  }

  confirmOptimize(): void {
    if (!this.selectedSchedule?.id || !this.optimizePreferredStart) return;
    
    const newStart = new Date(this.optimizePreferredStart);
    const newEnd = new Date(newStart.getTime() + (this.getScheduleDurationHours() * 3600000));
    
    if ((this.planningStartDate && newStart < this.planningStartDate) || (this.planningEndDate && newEnd > this.planningEndDate)) {
      this.showError('La séance optimisée dépasserait le planning');
      return;
    }
    
    this.scheduleService.optimizeScheduleTime(this.selectedSchedule.id, newStart).subscribe({
      next: (optimized) => {
        this.loadSchedules();
        this.closeOptimizeModal();
        this.showSuccess('Séance optimisée');
        if (this.selectedSchedule?.id === optimized.id) {
          this.populateFormForEdit(optimized);
        }
      },
      error: (err) => this.showError(err.error?.message || 'Erreur lors de l\'optimisation')
    });
  }

  // ==================== GETTERS ====================
  
  get titleControl() { return this.scheduleForm.get('title'); }
  get notesControl() { return this.scheduleForm.get('notes'); }
  get startTimeControl() { return this.scheduleForm.get('startTime'); }
  get endTimeControl() { return this.scheduleForm.get('endTime'); }
}