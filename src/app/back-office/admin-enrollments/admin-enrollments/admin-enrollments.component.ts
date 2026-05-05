import { Component, OnInit } from '@angular/core';
import { AdminEnrollmentService, Enrollment } from '../../services/admin-enrollment.service';

type SortField = 'id' | 'learnerId' | 'courseId' | 'status' | 'progress' | 'enrolledDate';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-admin-enrollments',
  templateUrl: './admin-enrollments.component.html',
  styleUrls: ['./admin-enrollments.component.scss']
})
export class AdminEnrollmentsComponent implements OnInit {

  enrollments: Enrollment[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // Filtres
  showFilters = false;
  searchTerm = '';
  filterStatus = 'all';
  dateFrom = '';
  dateTo = '';

  // Tri
  sortField: SortField = 'enrolledDate';
  sortDirection: SortDirection = 'desc';

  // Pagination
  currentPage = 1;
  itemsPerPage = 10;

  constructor(private enrollmentService: AdminEnrollmentService) {}

  ngOnInit(): void {
    this.loadEnrollments();
  }

  loadEnrollments(): void {
    this.loading = true;
    this.enrollmentService.getAllEnrollments().subscribe({
      next: (data: Enrollment[]) => {
        this.enrollments = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des enrollments';
        this.loading = false;
      }
    });
  }

  updateStatus(enrollment: Enrollment, status: string): void {
    this.enrollmentService.updateStatus(enrollment.id, status).subscribe({
      next: (updated: Enrollment) => {
        enrollment.status = updated.status;
        enrollment.progress = updated.progress;
        this.successMessage = 'Statut mis à jour avec succès !';
        setTimeout(() => this.successMessage = null, 3000);
      },
      error: () => {
        this.error = 'Erreur lors de la mise à jour';
        setTimeout(() => this.error = null, 3000);
      }
    });
  }

  cancelEnrollment(enrollment: Enrollment): void {
    if (!confirm('Annuler cet enrollment ?')) return;
    this.enrollmentService.cancelEnrollment(enrollment.id).subscribe({
      next: () => {
        enrollment.status = 'CANCELLED';
        this.successMessage = 'Enrollment annulé !';
        setTimeout(() => this.successMessage = null, 3000);
      }
    });
  }

  // ===== STATS =====
  getTotal(): number { return this.enrollments.length; }
  getActive(): number { return this.enrollments.filter(e => e.status === 'ACTIVE').length; }
  getCompleted(): number { return this.enrollments.filter(e => e.status === 'COMPLETED').length; }
  getCancelled(): number { return this.enrollments.filter(e => e.status === 'CANCELLED').length; }
  getAvgProgress(): number {
    if (!this.enrollments.length) return 0;
    return Math.round(this.enrollments.reduce((s, e) => s + (e.progress || 0), 0) / this.enrollments.length);
  }

  getByStatus(): { status: string; count: number }[] {
    const map = new Map<string, number>();
    this.enrollments.forEach(e => map.set(e.status, (map.get(e.status) || 0) + 1));
    return Array.from(map.entries()).map(([status, count]) => ({ status, count }));
  }

  // ===== FILTRES =====
  get filteredEnrollments(): Enrollment[] {
    let filtered = [...this.enrollments];

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(e =>
        e.id?.toString().includes(term) ||
        e.learnerId?.toString().includes(term) ||
        e.courseId?.toString().includes(term) ||
        e.status?.toLowerCase().includes(term)
      );
    }

    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(e => e.status === this.filterStatus);
    }

    if (this.dateFrom) {
      filtered = filtered.filter(e => new Date(e.enrolledDate) >= new Date(this.dateFrom));
    }

    if (this.dateTo) {
      const to = new Date(this.dateTo);
      to.setHours(23, 59, 59);
      filtered = filtered.filter(e => new Date(e.enrolledDate) <= to);
    }

    filtered.sort((a, b) => {
      let aVal: any, bVal: any;
      switch (this.sortField) {
        case 'id':           aVal = a.id;                        bVal = b.id;           break;
        case 'learnerId':    aVal = a.learnerId;                 bVal = b.learnerId;    break;
        case 'courseId':     aVal = a.courseId;                  bVal = b.courseId;     break;
        case 'status':       aVal = a.status;                    bVal = b.status;       break;
        case 'progress':     aVal = a.progress;                  bVal = b.progress;     break;
        case 'enrolledDate': aVal = new Date(a.enrolledDate);    bVal = new Date(b.enrolledDate); break;
        default: return 0;
      }
      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return filtered;
  }

  get paginatedEnrollments(): Enrollment[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredEnrollments.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredEnrollments.length / this.itemsPerPage);
  }

  sort(field: SortField): void {
    this.sortDirection = this.sortField === field
      ? (this.sortDirection === 'asc' ? 'desc' : 'asc') : 'asc';
    this.sortField = field;
    this.currentPage = 1;
  }

  getSortIcon(field: SortField): string {
    if (this.sortField !== field) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterStatus = 'all';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
  }

  getPagesArray(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  formatDate(d: string): string {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('fr-FR', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  getStatusColor(status: string): string {
    const colors: any = {
      ACTIVE: '#10b981', COMPLETED: '#3b82f6',
      PENDING: '#f59e0b', CANCELLED: '#ef4444'
    };
    return colors[status] || '#64748b';
  }

  exportToCSV(): void {
    const headers = ['ID', 'Learner ID', 'Course ID', 'Statut', 'Progression', 'Date inscription', 'Date completion'];
    let csv = headers.join(',') + '\n';
    this.filteredEnrollments.forEach(e => {
      csv += `${e.id},${e.learnerId},${e.courseId},${e.status},${e.progress}%,${e.enrolledDate},${e.completedDate || ''}\n`;
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.setAttribute('href', URL.createObjectURL(blob));
    link.setAttribute('download', `enrollments_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
  countByProgress(min: number, max: number): number {
  return this.enrollments.filter(e => e.progress >= min && e.progress <= max).length;
}
}