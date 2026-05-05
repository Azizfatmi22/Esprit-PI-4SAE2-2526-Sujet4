import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CourseService } from '../../services/course.service';
import { FileUrlService } from '../../services/file-url.service';
import { CourseSummary, Level } from '../../courses/modules/course.model';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';
import { CartService, Invoice } from '../../services/cart.service';
import { CouponService } from '../../services/coupon.service';

@Component({
  selector: 'app-course-learner-list',
  templateUrl: './course-learner-list.component.html',
  styleUrls: ['./course-learner-list.component.scss']
})

export class CourseLearnerList implements OnInit {
  courses: CourseSummary[] = [];
  filteredCourses: CourseSummary[] = [];
  loading = true;
  Currentuser: User | null = null;
  searchTerm = '';
  selectedLevel: Level | 'ALL' = 'ALL';
  sortBy: 'newest' | 'popular' | 'price-low' | 'price-high' = 'newest';
  private paidStudentsByCourseTitle = new Map<string, number>();
  private learnerPurchasedCourseTitles = new Set<string>();

  Level = Level;

  constructor(
    private courseService: CourseService,
    private fileUrlService: FileUrlService,
    private router: Router,
    private userService: UserService,
    private cartService: CartService,
    private couponService: CouponService
  ) { }

  async ngOnInit(): Promise<void> {
    this.Currentuser = this.userService.getUser() || null;
    if (!this.Currentuser) {
      this.Currentuser = (await this.userService.loadUser()) || null;
    }
    this.loadCourses();
  }

  loadCourses(): void {
    this.loading = true;
    const learnerId = (this.Currentuser?.id || '').toString();

    forkJoin({
      coursesResponse: this.courseService.getAllCourses(0, 100),
      directInvoices: this.cartService.getDirectInvoicesAll().pipe(catchError(() => of([] as Invoice[]))),
      installmentInvoices: this.cartService.getInstallmentInvoicesAll().pipe(catchError(() => of([] as Invoice[]))),
      couponClaims: learnerId ? this.couponService.getMyClaims(learnerId).pipe(catchError(() => of([]))) : of([])
    }).subscribe({
      next: ({ coursesResponse, directInvoices, installmentInvoices, couponClaims }) => {
        const allCourses = coursesResponse.content || coursesResponse;
        const allPaidInvoices = [...directInvoices, ...installmentInvoices].filter((invoice) =>
          this.isPaidInvoice(invoice)
        );

        this.buildPaidStudentsCount(allPaidInvoices);
        this.buildPurchasedCoursesForCurrentLearner(allPaidInvoices);

        // Add courses access via coupons
        couponClaims.forEach(claim => {
          if (claim.used && claim.courseName) {
            this.learnerPurchasedCourseTitles.add(this.normalizeTitle(claim.courseName));
          }
        });

        // Filter to show only PUBLISHED courses
        this.courses = allCourses.filter((course: CourseSummary) => course.status === 'PUBLISHED');
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading courses:', error);
        this.loading = false;
      }
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

  private buildPurchasedCoursesForCurrentLearner(invoices: Invoice[]): void {
    const learnerId = (this.Currentuser?.id || '').toString();
    this.learnerPurchasedCourseTitles.clear();

    if (!learnerId) {
      return;
    }

    invoices
      .filter((invoice) => (invoice.learnerId || '').toString() === learnerId)
      .forEach((invoice) => {
        (invoice.purchasedCourses || []).forEach((title) => {
          const key = this.normalizeTitle(title);
          if (key) {
            this.learnerPurchasedCourseTitles.add(key);
          }
        });
      });
  }

  private normalizeTitle(title: string): string {
    return (title || '').trim().toLowerCase().replace(/\s+/g, ' ');
  }

  applyFilters(): void {
    let filtered = [...this.courses].filter(
      (course) => !this.learnerPurchasedCourseTitles.has(this.normalizeTitle(course.title))
    );

    // Filter by search term
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(course =>
        course.title.toLowerCase().includes(term) ||
        course.description.toLowerCase().includes(term)
      );
    }

    // Filter by level
    if (this.selectedLevel !== 'ALL') {
      filtered = filtered.filter(course => course.level === this.selectedLevel);
    }

    // Sort
    switch (this.sortBy) {
      case 'newest':
        // Assuming courses are already sorted by creation date
        break;
      case 'popular':
        filtered.sort((a, b) => (b.enrolledStudents || 0) - (a.enrolledStudents || 0));
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
    const learnerId = this.Currentuser?.id;
    if (!learnerId) {
      alert('Utilisateur non connecté. Veuillez vous reconnecter.');
      return;
    }

    const course = this.courses.find(c => c.id === courseId) || this.filteredCourses.find(c => c.id === courseId);
    if (!course) {
      alert('Données du cours invalides');
      return;
    }

    const price = course.price || 0;
    const title = course.title;

    this.cartService.addCourseToCart(learnerId, courseId, title, price).subscribe({
      next: () => {
        alert(`✅ ${title} ajouté au panier !`);
      },
      error: (error) => {
        console.error('Erreur ajout panier:', error);

        if (error.status === 409) {
          alert('Ce cours est déjà dans votre panier !');
        } else if (error.status === 401 || error.status === 403) {
          alert('Session expirée. Veuillez vous reconnecter.');
        } else if (error.status === 400) {
          const details = error?.error?.message || error?.error || 'Requête invalide.';
          alert(`Impossible d'ajouter le cours au panier: ${details}`);
        } else {
          const details = error?.error?.message || error?.message || 'Veuillez réessayer.';
          alert(`Erreur lors de l'ajout au panier (${error.status || 'N/A'}): ${details}`);
        }
      }
    });
  }
  viewCourseDetails(courseId: number): void {
    // Navigate to course preview page
    this.router.navigate(['/learner/courses', courseId]);
  }
}
