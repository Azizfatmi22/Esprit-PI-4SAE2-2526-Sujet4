// learning-path-list.component.ts

import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { LearningPathService } from '../../services/learning-path.service';
import { LearningPath, LearningLevel, LearningPathStatus } from '../../models/learning-path.model';

@Component({
  selector: 'app-learning-path-list',
  templateUrl: './learning-path-list.component.html',
  styleUrls: ['./learning-path-list.component.scss']
})
export class LearningPathListComponent implements OnInit {
  learningPaths: LearningPath[] = [];
  filteredPaths: LearningPath[] = [];
  loading: boolean = false;
  
  // Search and filters
  searchTerm: string = '';
  selectedLevel: string = '';
  selectedStatus: string = '';
  hoursMin: number | null = null;
  hoursMax: number | null = null;
  showAdvancedFilters: boolean = false;
  
  // Analytics
  totalPaths: number = 0;
  activePaths: number = 0;
  totalHours: number = 0;
  avgHoursPerPath: number = 0;
  completionRate: number = 0;
  
  // UI State
  showAnalyticsModal: boolean = false;
  showSuccess: boolean = false;
  showError: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  
  // Enums for template
  LearningLevel = LearningLevel;
  LearningPathStatus = LearningPathStatus;
  
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
    private learningPathService: LearningPathService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadLearningPaths();
  }

  loadLearningPaths(): void {
    this.loading = true;
    this.learningPathService.getAllLearningPaths().subscribe({
      next: (paths) => {
        this.learningPaths = paths;
        this.filteredPaths = paths;
        this.calculateStats();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading learning paths:', err);
        this.errorMessage = 'Erreur lors du chargement des parcours';
        this.showError = true;
        setTimeout(() => this.hideMessages(), 3000);
        this.loading = false;
      }
    });
  }

  calculateStats(): void {
    this.totalPaths = this.learningPaths.length;
    this.activePaths = this.learningPaths.filter(p => p.status === 'ACTIVE').length;
    this.totalHours = this.learningPaths.reduce((sum, p) => sum + (p.totalHours || 0), 0);
    this.avgHoursPerPath = this.totalPaths > 0 ? Math.round(this.totalHours / this.totalPaths) : 0;
    this.completionRate = this.totalPaths > 0 ? Math.round((this.activePaths / this.totalPaths) * 100) : 0;
  }

  applyFilters(): void {
    this.filteredPaths = this.learningPaths.filter(path => {
      const matchesSearch = this.searchTerm === '' || 
        path.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        path.description.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (path.objectives && path.objectives.toLowerCase().includes(this.searchTerm.toLowerCase()));
      
      const matchesLevel = this.selectedLevel === '' || path.level === this.selectedLevel;
      const matchesStatus = this.selectedStatus === '' || path.status === this.selectedStatus;
      
      const matchesHours = this.checkHoursFilter(path.totalHours || 0);
      
      return matchesSearch && matchesLevel && matchesStatus && matchesHours;
    });
  }

  checkHoursFilter(hours: number): boolean {
    if (this.hoursMin !== null && hours < this.hoursMin) return false;
    if (this.hoursMax !== null && hours > this.hoursMax) return false;
    return true;
  }

  onSearch(): void {
    this.applyFilters();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedLevel = '';
    this.selectedStatus = '';
    this.hoursMin = null;
    this.hoursMax = null;
    this.applyFilters();
  }

  resetFilters(): void {
    this.clearFilters();
    this.showAdvancedFilters = false;
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.selectedLevel || this.selectedStatus || 
              this.hoursMin !== null || this.hoursMax !== null);
  }

  getCountByStatus(status: string): number {
    return this.learningPaths.filter(p => p.status === status).length;
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

  get levelStats(): any[] {
    const counts = this.learningPaths.reduce((acc, path) => {
      acc[path.level] = (acc[path.level] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
    
    return this.levelOptions.map(level => ({
      ...level,
      count: counts[level.value] || 0,
      percentage: this.totalPaths > 0 ? (counts[level.value] || 0) / this.totalPaths * 100 : 0
    }));
  }

  get statusStats(): any[] {
    const counts = this.learningPaths.reduce((acc, path) => {
      acc[path.status] = (acc[path.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
    
    return this.statusOptions.map(status => ({
      ...status,
      count: counts[status.value] || 0
    }));
  }

  viewPath(id: number): void {
    this.router.navigate(['/learning-paths', id]);
  }

  editPath(id: number): void {
    this.router.navigate(['/learning-paths/edit', id]);
  }

  deletePath(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce parcours ? Cette action est irréversible.')) {
      this.learningPathService.deleteLearningPath(id).subscribe({
        next: () => {
          this.successMessage = 'Parcours supprimé avec succès';
          this.showSuccess = true;
          setTimeout(() => this.hideMessages(), 3000);
          this.loadLearningPaths();
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

  createPath(): void {
    this.router.navigate(['/learning-paths/new']);
  }

  importPath(): void {
    // TODO: Implement import functionality
    console.log('Import path');
  }

  exportPaths(): void {
    // TODO: Implement export functionality
    console.log('Export paths');
  }

  showAnalytics(): void {
    this.showAnalyticsModal = true;
  }

  closeAnalytics(): void {
    this.showAnalyticsModal = false;
  }

  hideMessages(): void {
    this.showSuccess = false;
    this.showError = false;
    this.successMessage = '';
    this.errorMessage = '';
  }
}