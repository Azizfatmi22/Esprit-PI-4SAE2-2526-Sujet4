import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ReportingService } from '../../services/reporting.service';

interface EvaluationResult {
  id?: number;
  learnerName: string;
  type: string;
  scoreObtained: number;
  timeSpentSeconds: number;
  duration: number;
  totalPossiblePoints: number;
  percentage: number;
  isPassed: boolean;
  receivedAt: string | Date; // Corrected field name
  evaluationId?: number;
  vigilanceStatus: 'CLEAN' | 'SUSPICIOUS' | 'BANNED';
}

@Component({
  selector: 'app-evaluation-history',
  templateUrl: './evaluation-history.component.html',
  styleUrls: ['./evaluation-history.component.scss'],
})
export class EvaluationHistoryComponent implements OnInit {
  history: EvaluationResult[] = [];
  filteredHistory: EvaluationResult[] = [];
  searchTerm: string = '';
  resultFilter: string = 'all';
  vigilanceFilter: string = 'all'; // Updated filter
  isLoading: boolean = false;
  evaluationId!: number;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private reportingService: ReportingService,
  ) {}

  ngOnInit(): void {
    this.evaluationId = +this.route.snapshot.params['id'];
    this.loadHistory();
  }

  loadHistory(): void {
    if (!this.evaluationId) return;
    this.isLoading = true;
    this.reportingService.getEvaluationHistory(this.evaluationId).subscribe({
      next: (data: EvaluationResult[]) => {
        this.history = data || [];
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading history:', error);
        this.isLoading = false;
      },
    });
  }

  updateStatus(item: EvaluationResult, newStatus: string): void {
    if (!item.id) return;
    this.reportingService.updateVigilanceStatus(item.id, newStatus).subscribe({
      next: () => {
        item.vigilanceStatus = newStatus as any;
        this.applyFilters();
        console.log(`Status updated to ${newStatus}`);
      },
      error: (err) => {
        console.error('Failed to update status', err);
        alert('Error updating status.');
      },
    });
  }

  getStatusDescription(status: string): string {
    switch (status) {
      case 'CLEAN':
        return 'Normal activity: Submission looks legitimate.';
      case 'SUSPICIOUS':
        return 'Warning: Submission was unusually fast.';
      case 'BANNED':
        return 'Restricted: Certification is blocked.';
      default:
        return 'No status assigned.';
    }
  }

  get totalAttempts(): number {
    return this.history.length;
  }

  get successRate(): number {
    if (!this.history.length) return 0;
    const passed = this.history.filter((item) => item.isPassed).length;
    return Math.round((passed / this.history.length) * 100);
  }

  // Fixed: Counter now checks vigilanceStatus Enum
  get suspiciousCount(): number {
    return this.history.filter((item) => item.vigilanceStatus === 'SUSPICIOUS')
      .length;
  }

  get averageScore(): number {
    if (!this.history.length) return 0;
    const avg =
      this.history.reduce((sum, item) => sum + item.percentage, 0) /
      this.history.length;
    return Math.round(avg);
  }

  getInitials(name: string): string {
    if (!name) return '??';
    return name
      .split(' ')
      .map((n) => n.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  // Fixed Filter Logic
  applyFilters(): void {
    if (!this.history.length) {
      this.filteredHistory = [];
      return;
    }

    this.filteredHistory = this.history.filter((item) => {
      const matchesSearch =
        this.searchTerm === '' ||
        item.learnerName?.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesResult =
        this.resultFilter === 'all' ||
        (this.resultFilter === 'passed' && item.isPassed) ||
        (this.resultFilter === 'failed' && !item.isPassed);

      // Updated: Vigilance filter matches the Enum status
      const matchesVigilance =
        this.vigilanceFilter === 'all' ||
        item.vigilanceStatus === this.vigilanceFilter;

      return matchesSearch && matchesResult && matchesVigilance;
    });
  }

  formatDuration(seconds: number | undefined): string {
    if (seconds === undefined || seconds === null) return '00:00';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'CLEAN':
        return '✅';
      case 'SUSPICIOUS':
        return '⚠️';
      case 'BANNED':
        return '🚫';
      default:
        return '❓';
    }
  }
}
