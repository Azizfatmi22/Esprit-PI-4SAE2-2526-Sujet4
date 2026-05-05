import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AdminCourseService } from '../services/admin-course.service';
import { Course } from '../../front-office/courses/modules/course.model';
import { FileUrlService } from '../../front-office/services/file-url.service';
import { AdminPaymentService, Invoice } from '../services/admin-payment.service';

@Component({
  selector: 'app-admin-course',
  templateUrl: './admin-course.component.html',
  styleUrl: './admin-course.component.scss',
})
export class AdminCourseComponent implements OnInit {
  courses: Course[] = [];
  isLoading = true;
  errorMessage = '';
  private paidStudentsByCourseTitle = new Map<string, number>();
  searchTerm = '';
  selectedStatus = 'ALL';
  selectedLevel = 'ALL';
  sortBy = 'students-desc';
  showPaidOnly = false;

  stats = {
    totalCourses: 0,
    publishedCourses: 0,
    draftCourses: 0,
    totalStudents: 0,
    estimatedRevenue: 0,
    topCourseTitle: '-',
  };

  // Modal
  selectedCourse: Course | null = null;
  modalLoading = false;

  constructor(
    private adminCourseService: AdminCourseService,
    private fileUrlService: FileUrlService,
    private adminPaymentService: AdminPaymentService,
  ) {}

  getThumbnailUrl(thumbnailUrl?: string, courseId?: number): string {
    return this.fileUrlService.getThumbnailUrl(thumbnailUrl || '', courseId);
  }

  ngOnInit(): void {
    this.loadCourses();
  }

  get filteredCourses(): Course[] {
    let list = [...this.courses];

    const query = this.searchTerm.trim().toLowerCase();
    if (query) {
      list = list.filter((course) => {
        const title = (course.title || '').toLowerCase();
        const description = (course.description || '').toLowerCase();
        return title.includes(query) || description.includes(query);
      });
    }

    if (this.selectedStatus !== 'ALL') {
      list = list.filter((course) => (course.status || '').toUpperCase() === this.selectedStatus);
    }

    if (this.selectedLevel !== 'ALL') {
      list = list.filter((course) => (course.level || '').toUpperCase() === this.selectedLevel);
    }

    if (this.showPaidOnly) {
      list = list.filter((course) => this.getPaidStudentsCount(course.title) > 0);
    }

    switch (this.sortBy) {
      case 'title-asc':
        list.sort((a, b) => (a.title || '').localeCompare(b.title || ''));
        break;
      case 'title-desc':
        list.sort((a, b) => (b.title || '').localeCompare(a.title || ''));
        break;
      case 'price-asc':
        list.sort((a, b) => this.toNumber(a.price) - this.toNumber(b.price));
        break;
      case 'price-desc':
        list.sort((a, b) => this.toNumber(b.price) - this.toNumber(a.price));
        break;
      case 'students-asc':
        list.sort((a, b) => this.getPaidStudentsCount(a.title) - this.getPaidStudentsCount(b.title));
        break;
      case 'students-desc':
      default:
        list.sort((a, b) => this.getPaidStudentsCount(b.title) - this.getPaidStudentsCount(a.title));
        break;
    }

    return list;
  }

  get statusOptions(): string[] {
    const statusSet = new Set(
      this.courses
        .map((course) => (course.status || '').toUpperCase())
        .filter((status) => !!status)
    );

    return ['ALL', ...Array.from(statusSet).sort()];
  }

  get levelOptions(): string[] {
    const levelSet = new Set(
      this.courses
        .map((course) => (course.level || '').toUpperCase())
        .filter((level) => !!level)
    );

    return ['ALL', ...Array.from(levelSet).sort()];
  }

  loadCourses(): void {
    this.isLoading = true;
    forkJoin({
      coursesResponse: this.adminCourseService.getAllCourses(),
      directInvoices: this.adminPaymentService.getDirectInvoices().pipe(catchError(() => of([] as Invoice[]))),
      installmentInvoices: this.adminPaymentService.getInstallmentInvoices().pipe(catchError(() => of([] as Invoice[])))
    }).subscribe({
      next: ({ coursesResponse, directInvoices, installmentInvoices }) => {
        this.courses = Array.isArray(coursesResponse) ? coursesResponse : (coursesResponse.content ?? []);

        const paidInvoices = [...directInvoices, ...installmentInvoices].filter((invoice) =>
          this.isPaidInvoice(invoice)
        );
        this.buildPaidStudentsCount(paidInvoices);

        this.updateStats();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load courses:', err);
        this.errorMessage = 'Failed to load courses.';
        this.isLoading = false;
      },
    });
  }

  private isPaidInvoice(invoice: Invoice): boolean {
    const status = (invoice.status || '').toUpperCase();
    return status === 'PAID' || status === 'SUCCESS' || status === 'COMPLETED';
  }

  private buildPaidStudentsCount(invoices: Invoice[]): void {
    const learnersByTitle = new Map<string, Set<string>>();

    invoices.forEach((invoice) => {
      const learnerId = (invoice.learnerId || '').toString();
      if (!learnerId) {
        return;
      }

      (invoice.purchasedCourses || []).forEach((title) => {
        const key = this.normalizeTitle(title);
        if (!key) {
          return;
        }

        if (!learnersByTitle.has(key)) {
          learnersByTitle.set(key, new Set<string>());
        }

        learnersByTitle.get(key)?.add(learnerId);
      });
    });

    this.paidStudentsByCourseTitle.clear();
    learnersByTitle.forEach((learners, titleKey) => {
      this.paidStudentsByCourseTitle.set(titleKey, learners.size);
    });
  }

  getPaidStudentsCount(courseTitle?: string): number {
    const key = this.normalizeTitle(courseTitle || '');
    return this.paidStudentsByCourseTitle.get(key) || 0;
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = 'ALL';
    this.selectedLevel = 'ALL';
    this.sortBy = 'students-desc';
    this.showPaidOnly = false;
  }

  trackByCourseId(_index: number, course: Course): number | string {
    return course.id ?? course.title ?? _index;
  }

  toggleCourseStatus(course: Course): void {
    const id = course.id;
    if (id == null) {
      return;
    }

    const currentStatus = (course.status || '').toUpperCase();
    const nextStatus = currentStatus === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED';
    this.updateStatus(id, nextStatus);
  }

  private updateStats(): void {
    const coursesWithStudents = this.courses.map((course) => {
      const paidStudents = this.getPaidStudentsCount(course.title);
      return { course, paidStudents };
    });

    const topCourse = coursesWithStudents.reduce<{ title: string; count: number } | null>((best, item) => {
      if (!best || item.paidStudents > best.count) {
        return { title: item.course.title || '-', count: item.paidStudents };
      }

      return best;
    }, null);

    this.stats.totalCourses = this.courses.length;
    this.stats.publishedCourses = this.courses.filter((course) => (course.status || '').toUpperCase() === 'PUBLISHED').length;
    this.stats.draftCourses = this.courses.filter((course) => (course.status || '').toUpperCase() === 'DRAFT').length;
    this.stats.totalStudents = coursesWithStudents.reduce((sum, item) => sum + item.paidStudents, 0);
    this.stats.estimatedRevenue = coursesWithStudents.reduce((sum, item) => {
      return sum + this.toNumber(item.course.price) * item.paidStudents;
    }, 0);
    this.stats.topCourseTitle = topCourse && topCourse.count > 0 ? topCourse.title : '-';
  }

  private toNumber(value: unknown): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private normalizeTitle(title: string): string {
    return (title || '').trim().toLowerCase().replace(/\s+/g, ' ');
  }

  viewCourse(id: number): void {
    this.modalLoading = true;
    this.selectedCourse = null;
    this.adminCourseService.getCourseById(id).subscribe({
      next: (course) => { this.selectedCourse = course; this.modalLoading = false; },
      error: () => { this.modalLoading = false; },
    });
  }

  closeModal(): void {
    this.selectedCourse = null;
  }

  updateStatus(id: number, status: string): void {
    this.adminCourseService.updateCourseStatus(id, status).subscribe({
      next: () => this.loadCourses(),
      error: (err) => console.error('Failed to update status:', err),
    });
  }

  deleteCourse(id: number): void {
    if (!confirm('Delete this course?')) return;
    this.adminCourseService.deleteCourse(id).subscribe({
      next: () => this.loadCourses(),
      error: (err) => console.error('Failed to delete course:', err),
    });
  }
}
