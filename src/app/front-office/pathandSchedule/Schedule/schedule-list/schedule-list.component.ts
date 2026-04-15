import { Component, OnInit, Input, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { ScheduleService } from '../../services/scheduleService.service';
import { Schedule, ScheduleStatus, ScheduleType, ScheduleAnalytics, ScheduleStatistics, ScheduleEfficiency } from '../../models/Schedule';
import { forkJoin, Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-schedule-list',
  templateUrl: './schedule-list.component.html',
  styleUrls: ['./schedule-list.component.scss']
})
export class ScheduleListComponent implements OnInit, OnDestroy, AfterViewInit {
  @Input() planningId!: number;
  @ViewChild('statusChart') statusChartRef!: ElementRef;
  @ViewChild('typeChart') typeChartRef!: ElementRef;
  @ViewChild('weeklyChart') weeklyChartRef!: ElementRef;
  
  // Data
  schedules: Schedule[] = [];
  filteredSchedules: Schedule[] = [];
  loading: boolean = false;
  analytics: ScheduleAnalytics | null = null;
  statistics: ScheduleStatistics | null = null;
  efficiency: ScheduleEfficiency | null = null;
  
  // UI State
  viewMode: 'list' | 'calendar' | 'analytics' = 'list';
  showFormModal: boolean = false;
  editingSchedule: Schedule | null = null;
  showDeleteConfirm: boolean = false;
  scheduleToDelete: number | null = null;
  
  // Filters
  statusFilter: string = 'all';
  typeFilter: string = 'all';
  searchTerm: string = '';
  dateRange: { start: Date | null; end: Date | null } = { start: null, end: null };
  
  // Bulk selection
  selectedSchedules: Set<number> = new Set();
  
  // Calendar view
  currentCalendarDate: Date = new Date();
  calendarDays: Date[] = [];
  calendarEvents: Map<string, Schedule[]> = new Map();
  
  // Charts
  private statusChart: any;
  private typeChart: any;
  private weeklyChart: any;
  
  // Search debounce
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();
  
  // Enums for template
  ScheduleStatus = ScheduleStatus;
  ScheduleType = ScheduleType;
  
  statusOptions = [
    { value: 'all', label: 'Tous', icon: '📋' },
    { value: 'PENDING', label: 'En attente', icon: '⏳', color: '#f59e0b', bg: '#fed7aa' },
    { value: 'ACTIVE', label: 'Actif', icon: '▶️', color: '#10b981', bg: '#d1fae5' },
    { value: 'CONFIRMED', label: 'Confirmé', icon: '✅', color: '#6366f1', bg: '#eef2ff' },
    { value: 'CANCELLED', label: 'Annulé', icon: '❌', color: '#ef4444', bg: '#fee2e2' }
  ];
  
  typeOptions = [
    { value: 'all', label: 'Tous', icon: '📋' },
    { value: 'LIVE', label: 'En direct', icon: '🎥', color: '#6366f1' },
    { value: 'RECORDED', label: 'Enregistré', icon: '📹', color: '#10b981' },
    { value: 'WORKSHOP', label: 'Atelier', icon: '🔧', color: '#f59e0b' }
  ];

  constructor(private scheduleService: ScheduleService) {}

  ngOnInit(): void {
    if (this.planningId) {
      this.loadAllData();
    }
    
    // Setup search debounce
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.applyFilters();
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initCharts(), 500);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.destroyCharts();
  }

  loadAllData(): void {
    this.loading = true;
    
    forkJoin({
      schedules: this.scheduleService.getSchedulesByPlanningId(this.planningId),
      analytics: this.scheduleService.getScheduleAnalytics(this.planningId),
      statistics: this.scheduleService.getScheduleStatistics(this.planningId),
      efficiency: this.scheduleService.getScheduleEfficiency(this.planningId)
    }).subscribe({
      next: (results) => {
        this.schedules = results.schedules;
        this.filteredSchedules = results.schedules;
        this.analytics = results.analytics;
        this.statistics = results.statistics;
        this.efficiency = results.efficiency;
        this.loading = false;
        this.generateCalendarDays();
        this.generateCalendarEvents();
        setTimeout(() => this.initCharts(), 200);
      },
      error: (err) => {
        console.error('Error loading schedule data:', err);
        this.loading = false;
      }
    });
  }

  // ==================== FILTERS ====================
  
  onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  applyFilters(): void {
    let filtered = [...this.schedules];
    
    // Status filter
    if (this.statusFilter !== 'all') {
      filtered = filtered.filter(s => s.status === this.statusFilter);
    }
    
    // Type filter
    if (this.typeFilter !== 'all') {
      filtered = filtered.filter(s => s.type === this.typeFilter);
    }
    
    // Search filter
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(s => 
        s.title.toLowerCase().includes(term) ||
        s.notes?.toLowerCase().includes(term)
      );
    }
    
    // Date range filter
    if (this.dateRange.start) {
      filtered = filtered.filter(s => new Date(s.startTime) >= this.dateRange.start!);
    }
    if (this.dateRange.end) {
      filtered = filtered.filter(s => new Date(s.endTime) <= this.dateRange.end!);
    }
    
    this.filteredSchedules = filtered;
  }

  resetFilters(): void {
    this.statusFilter = 'all';
    this.typeFilter = 'all';
    this.searchTerm = '';
    this.dateRange = { start: null, end: null };
    this.applyFilters();
  }

  hasActiveFilters(): boolean {
    return this.statusFilter !== 'all' || 
           this.typeFilter !== 'all' || 
           this.searchTerm !== '' ||
           this.dateRange.start !== null ||
           this.dateRange.end !== null;
  }

  // ==================== BULK OPERATIONS ====================
  
  toggleSelection(scheduleId: number): void {
    if (this.selectedSchedules.has(scheduleId)) {
      this.selectedSchedules.delete(scheduleId);
    } else {
      this.selectedSchedules.add(scheduleId);
    }
  }

  toggleSelectAll(): void {
    if (this.isAllSelected()) {
      this.selectedSchedules.clear();
    } else {
      this.filteredSchedules.forEach(s => this.selectedSchedules.add(s.id!));
    }
  }

  isAllSelected(): boolean {
    return this.filteredSchedules.length > 0 && 
           this.selectedSchedules.size === this.filteredSchedules.length;
  }

  bulkUpdateStatus(status: ScheduleStatus): void {
    const ids = Array.from(this.selectedSchedules);
    if (ids.length === 0) return;
    
    if (confirm(`Mettre à jour ${ids.length} session(s) vers "${this.getStatusLabel(status)}" ?`)) {
      this.scheduleService.bulkUpdateStatus(ids, status).subscribe({
        next: () => {
          this.loadAllData();
          this.selectedSchedules.clear();
        },
        error: (err) => console.error('Error updating statuses:', err)
      });
    }
  }

  bulkDelete(): void {
    const ids = Array.from(this.selectedSchedules);
    if (ids.length === 0) return;
    
    if (confirm(`Supprimer ${ids.length} session(s) ? Cette action est irréversible.`)) {
      this.scheduleService.bulkDelete(ids).subscribe({
        next: () => {
          this.loadAllData();
          this.selectedSchedules.clear();
        },
        error: (err) => console.error('Error deleting schedules:', err)
      });
    }
  }

  // ==================== CRUD OPERATIONS ====================
  
  openScheduleForm(schedule?: Schedule): void {
    this.editingSchedule = schedule || null;
    this.showFormModal = true;
  }

  closeScheduleForm(): void {
    this.showFormModal = false;
    this.editingSchedule = null;
  }

  onScheduleSaved(savedSchedule: Schedule): void {
    this.closeScheduleForm();
    this.loadAllData();
  }

  updateScheduleStatus(scheduleId: number, status: ScheduleStatus): void {
    this.scheduleService.updateScheduleStatus(scheduleId, status).subscribe({
      next: () => {
        this.loadAllData();
      },
      error: (err) => console.error('Error updating status:', err)
    });
  }

  confirmDelete(scheduleId: number): void {
    this.scheduleToDelete = scheduleId;
    this.showDeleteConfirm = true;
  }

  deleteSchedule(): void {
    if (this.scheduleToDelete) {
      this.scheduleService.deleteSchedule(this.scheduleToDelete).subscribe({
        next: () => {
          this.loadAllData();
          this.showDeleteConfirm = false;
          this.scheduleToDelete = null;
        },
        error: (err) => console.error('Error deleting schedule:', err)
      });
    }
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.scheduleToDelete = null;
  }

  // ==================== CALENDAR VIEW ====================
  
  generateCalendarDays(): void {
    const year = this.currentCalendarDate.getFullYear();
    const month = this.currentCalendarDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    
    this.calendarDays = [];
    const startDay = firstDay.getDay();
    for (let i = 0; i < startDay; i++) {
      this.calendarDays.push(new Date(year, month, -startDay + i + 1));
    }
    for (let i = 1; i <= lastDay.getDate(); i++) {
      this.calendarDays.push(new Date(year, month, i));
    }
    const remainingDays = 42 - this.calendarDays.length;
    for (let i = 1; i <= remainingDays; i++) {
      this.calendarDays.push(new Date(year, month + 1, i));
    }
  }

  generateCalendarEvents(): void {
    this.calendarEvents.clear();
    this.schedules.forEach(schedule => {
      const dateKey = new Date(schedule.startTime).toDateString();
      if (!this.calendarEvents.has(dateKey)) {
        this.calendarEvents.set(dateKey, []);
      }
      this.calendarEvents.get(dateKey)!.push(schedule);
    });
  }

  getEventsForDay(date: Date): Schedule[] {
    return this.calendarEvents.get(date.toDateString()) || [];
  }

  previousMonth(): void {
    this.currentCalendarDate = new Date(
      this.currentCalendarDate.getFullYear(),
      this.currentCalendarDate.getMonth() - 1,
      1
    );
    this.generateCalendarDays();
    this.generateCalendarEvents();
  }

  nextMonth(): void {
    this.currentCalendarDate = new Date(
      this.currentCalendarDate.getFullYear(),
      this.currentCalendarDate.getMonth() + 1,
      1
    );
    this.generateCalendarDays();
    this.generateCalendarEvents();
  }

  isToday(date: Date): boolean {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  }

  isCurrentMonth(date: Date): boolean {
    return date.getMonth() === this.currentCalendarDate.getMonth();
  }

  // ==================== CHARTS ====================
  
  initCharts(): void {
    this.destroyCharts();
    
    if (!this.analytics) return;
    
    // Status Distribution Chart
    if (this.statusChartRef?.nativeElement && this.statistics) {
      const ctx = this.statusChartRef.nativeElement.getContext('2d');
      this.statusChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['Confirmé', 'Actif', 'En attente', 'Annulé'],
          datasets: [{
            data: [
              this.statistics.confirmed || 0,
              this.statistics.active || 0,
              this.statistics.pending || 0,
              this.statistics.cancelled || 0
            ],
            backgroundColor: ['#10b981', '#6366f1', '#f59e0b', '#ef4444'],
            borderWidth: 0
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom' } }
        }
      });
    }
    
    // Type Distribution Chart
    if (this.typeChartRef?.nativeElement && this.analytics.typeDistribution) {
      const ctx = this.typeChartRef.nativeElement.getContext('2d');
      this.typeChart = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: ['En direct', 'Enregistré', 'Atelier'],
          datasets: [{
            data: [
              this.analytics.typeDistribution.LIVE || 0,
              this.analytics.typeDistribution.RECORDED || 0,
              this.analytics.typeDistribution.WORKSHOP || 0
            ],
            backgroundColor: ['#6366f1', '#10b981', '#f59e0b'],
            borderWidth: 0
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom' } }
        }
      });
    }
    
    // Weekly Distribution Chart
    if (this.weeklyChartRef?.nativeElement && this.analytics.dailyDistribution) {
      const ctx = this.weeklyChartRef.nativeElement.getContext('2d');
      const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
      const dayLabels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
      const data = days.map(day => this.analytics?.dailyDistribution[day] || 0);
      
      this.weeklyChart = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: dayLabels,
          datasets: [{
            label: 'Nombre de sessions',
            data: data,
            backgroundColor: 'rgba(99, 102, 241, 0.7)',
            borderColor: '#6366f1',
            borderWidth: 1,
            borderRadius: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: { y: { beginAtZero: true, title: { display: true, text: 'Sessions' } } }
        }
      });
    }
  }

  destroyCharts(): void {
    if (this.statusChart) { this.statusChart.destroy(); this.statusChart = null; }
    if (this.typeChart) { this.typeChart.destroy(); this.typeChart = null; }
    if (this.weeklyChart) { this.weeklyChart.destroy(); this.weeklyChart = null; }
  }

  // ==================== UTILITY METHODS ====================
  
  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.label || status;
  }

  getStatusIcon(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.icon || '📋';
  }

  getStatusColor(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.color || '#6b7280';
  }

  getStatusBgColor(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option?.bg || '#f3f4f6';
  }

  getTypeIcon(type: string): string {
    const option = this.typeOptions.find(o => o.value === type);
    return option?.icon || '📋';
  }

  getTypeLabel(type: string): string {
    const option = this.typeOptions.find(o => o.value === type);
    return option?.label || type;
  }

  getTypeColor(type: string): string {
    const option = this.typeOptions.find(o => o.value === type);
    return option?.color || '#6b7280';
  }

  formatDuration(schedule: Schedule): string {
    const start = new Date(schedule.startTime);
    const end = new Date(schedule.endTime);
    const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60);
    return `${hours}h`;
  }

  formatTime(date: Date): string {
    return new Date(date).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  getEfficiencyLevel(): string {
    if (!this.efficiency) return 'N/A';
    const score = this.efficiency.efficiencyScore;
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Bon';
    if (score >= 40) return 'Moyen';
    return 'À améliorer';
  }

  getEfficiencyColor(): string {
    if (!this.efficiency) return '#6b7280';
    const score = this.efficiency.efficiencyScore;
    if (score >= 80) return '#10b981';
    if (score >= 60) return '#6366f1';
    if (score >= 40) return '#f59e0b';
    return '#ef4444';
  }

  exportSchedules(): void {
    this.scheduleService.exportSchedules(this.planningId).subscribe({
      next: (data) => {
        const blob = new Blob([data], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `schedules_planning_${this.planningId}.json`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Error exporting:', err)
    });
  }

  autoSchedule(): void {
    const daysNeeded = prompt('Nombre de jours à planifier automatiquement ?', '5');
    if (daysNeeded) {
      this.scheduleService.autoSchedule(this.planningId, parseInt(daysNeeded)).subscribe({
        next: () => this.loadAllData(),
        error: (err) => console.error('Error auto-scheduling:', err)
      });
    }
  }
}