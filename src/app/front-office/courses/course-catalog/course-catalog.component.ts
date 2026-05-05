import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CourseService } from '../../services/course.service';
import { CourseSummary, Level } from '../../courses/modules/course.model';
import { FileUrlService } from '../../services/file-url.service';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';
import { CartService, Invoice } from '../../services/cart.service';


@Component({
  selector: 'app-course-catalog',
  templateUrl: './course-catalog.component.html',
  styleUrls: ['./course-catalog.component.scss'],
})
export class CourseCatalogComponent implements OnInit {
  courses: CourseSummary[] = [];
  filteredCourses: CourseSummary[] = [];
  loading = true;
  searchTerm = '';
  selectedLevel: Level | 'ALL' = 'ALL';
  sortBy: 'newest' | 'popular' | 'price-low' | 'price-high' = 'newest';
  Currentuser: User | null = null;
  private paidStudentsByCourseTitle = new Map<string, number>();

  Level = Level;

  constructor(
    private courseService: CourseService,
    private fileUrlService: FileUrlService,
    private router: Router,
    private userService: UserService,
    private cartService: CartService,
  ) {}

  ngOnInit(): void {
    this.Currentuser = this.userService.getUser() || null;
    this.loadCourses();
  }
  loadCourses(): void {
    this.loading = true;

    // 1. Get the trainerId (using your environment variable or a hardcoded ID)
    const trainerId = this.userService.getUser()?.id;

    if (!trainerId) {
      console.error('Trainer ID is not available');
      this.loading = false;
      return;
    }

    forkJoin({
      coursesResponse: this.courseService.getCoursesByTrainer(trainerId),
      directInvoices: this.cartService.getDirectInvoicesAll().pipe(catchError(() => of([] as Invoice[]))),
      installmentInvoices: this.cartService.getInstallmentInvoicesAll().pipe(catchError(() => of([] as Invoice[])))
    }).subscribe({
      next: ({ coursesResponse, directInvoices, installmentInvoices }) => {
        console.log('Received courses from API:', coursesResponse);
        const paidInvoices = [...directInvoices, ...installmentInvoices].filter((invoice) =>
          this.isPaidInvoice(invoice)
        );

        this.buildPaidStudentsCount(paidInvoices);

        this.courses = coursesResponse;

        this.applyFilters();
        this.loading = false;
        console.log(
          `Loaded ${this.courses.length} published courses for trainer ${trainerId}`,
        );
      },
      error: (error) => {
        console.error('Error loading trainer courses:', error);
        this.loading = false;
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

  getPaidStudentsCount(courseTitle: string): number {
    const key = this.normalizeTitle(courseTitle);
    return this.paidStudentsByCourseTitle.get(key) || 0;
  }

  private normalizeTitle(title: string): string {
    return (title || '').trim().toLowerCase().replace(/\s+/g, ' ');
  }

  applyFilters(): void {
    let filtered = [...this.courses];

    // Filter by search term
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(
        (course) =>
          course.title.toLowerCase().includes(term) ||
          course.description.toLowerCase().includes(term),
      );
    }

    // Filter by level
    if (this.selectedLevel !== 'ALL') {
      filtered = filtered.filter(
        (course) => course.level === this.selectedLevel,
      );
    }

    // Sort
    switch (this.sortBy) {
      case 'newest':
        // Assuming courses are already sorted by creation date
        break;
      case 'popular':
        filtered.sort((a, b) => this.getPaidStudentsCount(b.title) - this.getPaidStudentsCount(a.title));
        break;
      case 'price-low':
        filtered.sort((a, b) => a.price - b.price);
        break;
      case 'price-high':
        filtered.sort((a, b) => b.price - a.price);
        break;
    }

    this.filteredCourses = filtered;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onLevelChange(level: Level | 'ALL'): void {
    this.selectedLevel = level;
    this.applyFilters();
  }

  onSortChange(): void {
    this.applyFilters();
  }

  getLevelBadgeClass(level: Level): string {
    switch (level) {
      case Level.BEGINNER:
        return 'level-beginner';
      case Level.INTERMEDIATE:
        return 'level-intermediate';
      case Level.ADVANCED:
        return 'level-advanced';
      default:
        return '';
    }
  }

  getLevelLabel(level: Level): string {
    switch (level) {
      case Level.BEGINNER:
        return 'Beginner';
      case Level.INTERMEDIATE:
        return 'Intermediate';
      case Level.ADVANCED:
        return 'Advanced';
      default:
        return level;
    }
  }

  formatDuration(minutes?: number): string {
    if (!minutes) return 'N/A';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${mins}m`;
  }

  getThumbnailUrl(thumbnailUrl?: string, coursId?: number): string {
    return this.fileUrlService.getThumbnailUrl(thumbnailUrl || '', coursId);
  }

  onImageError(event: any): void {
    // Fallback to placeholder gradient background
    event.target.style.display = 'none';
    const parent = event.target.parentElement;
    if (parent) {
      parent.classList.add('thumbnail-error');
    }
  }

  enrollInCourse(courseId: number): void {
    // Navigate to course preview page
    this.router.navigate(['/catalog', courseId]);
  }

  viewCourseDetails(courseId: number): void {
    // Navigate to course preview page
    this.router.navigate(['/catalog', courseId]);
  }

  isTrainer(): boolean {
    return this.Currentuser?.roles?.includes('TRAINER') || false;
  }

  isCoursOwner(trainerId?: number): boolean {
    return this.Currentuser?.id === trainerId;
  }

  openEditModal(courseId: number): void {
    // Navigate to edit course page
    this.router.navigate(['/course/edit', courseId]);
  }

  viewSessions(courseId: number): void {
  this.router.navigate(['/sessionsList'], { 
    queryParams: { courseId } 
  });
}

  deleteCourse(courseId: number, courseName: string): void {
    // Show confirmation dialog
    if (
      confirm(
        `Are you sure you want to delete "${courseName}"? This action cannot be undone.`,
      )
    ) {
      this.courseService.deleteCourse(courseId).subscribe({
        next: () => {
          // Remove the course from the list
          this.courses = this.courses.filter((c) => c.id !== courseId);
          this.applyFilters();
          alert('Course deleted successfully!');
        },
        error: (error) => {
          console.error('Error deleting course:', error);
          alert('Failed to delete course. Please try again.');
        },
      });
    }
  }
}
