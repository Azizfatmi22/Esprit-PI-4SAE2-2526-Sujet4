import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { User } from '../../../user';
import { Invoice, CartService } from '../../services/cart.service';
import { UserService } from '../../services/user.service';
import { CourseService } from '../../services/course.service';
import { FileUrlService } from '../../services/file-url.service';
import { CouponService } from '../../services/coupon.service';
import { Course, CourseSummary, Level } from '../../courses/modules/course.model';

interface PaidCourse {
  title: string;
  purchaseCount: number;
  lastPaidDate: string;
}

interface MyCourseCard extends PaidCourse {
  courseId: number | null;
  description: string;
  level: Level | 'UNKNOWN';
  rating?: number;
  enrolledStudents: number;
  totalChapters: number;
  totalDurationMinutes?: number;
  thumbnailUrl?: string;
  isFinished: boolean;
}

@Component({
  selector: 'app-mycourses',
  templateUrl: './mycourses.component.html',
  styleUrls: ['./mycourses.component.scss']
})
export class MycoursesComponent implements OnInit {
  currentUser: User | null = null;
  learnerId: string | null = null;

  loading = false;
  errorMessage: string | null = null;
  paidCourses: MyCourseCard[] = [];
  filteredCourses: MyCourseCard[] = [];
  searchTerm = '';

  Level = Level;

  constructor(
    private cartService: CartService,
    private userService: UserService,
    private courseService: CourseService,
    private fileUrlService: FileUrlService,
    private couponService: CouponService,
    private router: Router
  ) { }

  async ngOnInit(): Promise<void> {
    this.currentUser = this.userService.getUser() || null;
    if (!this.currentUser) {
      this.currentUser = (await this.userService.loadUser()) || null;
    }

    this.learnerId = this.currentUser?.id ?? null;
    if (!this.learnerId) {
      this.errorMessage = 'User not connected. Please login again.';
      return;
    }

    this.loadPaidCourses(this.learnerId);
  }

  private loadPaidCourses(learnerId: string): void {
    this.loading = true;
    this.errorMessage = null;

    forkJoin({
      direct: this.cartService.getDirectInvoices(learnerId),
      installment: this.cartService.getInstallmentInvoices(learnerId),
      coursesResponse: this.courseService.getAllCourses(0, 500),
      claims: this.couponService.getMyClaims(learnerId).pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ direct, installment, coursesResponse, claims }) => {
        const paidInvoices = [...direct, ...installment].filter((invoice) =>
          this.isPaidInvoice(invoice)
        );
        const paidTitles = this.extractPaidCourses(paidInvoices);

        // Include claimed courses
        claims.forEach(claim => {
          if (!paidTitles.some(p => p.title.toLowerCase() === claim.courseName.toLowerCase())) {
            paidTitles.push({
              title: claim.courseName,
              purchaseCount: 1,
              lastPaidDate: claim.claimedAt ? new Date(claim.claimedAt).toISOString() : new Date().toISOString()
            });
          }
        });

        // Ensure array is sorted by date again just in case
        paidTitles.sort((a, b) => new Date(b.lastPaidDate).getTime() - new Date(a.lastPaidDate).getTime());

        const allCourses: CourseSummary[] = coursesResponse?.content || coursesResponse || [];
        const courseLookup = new Map<string, CourseSummary>();

        allCourses.forEach((course) => {
          const key = this.normalizeTitle(course.title);
          if (!courseLookup.has(key)) {
            courseLookup.set(key, course);
          }
        });

        const cards: MyCourseCard[] = paidTitles.map((paid) => {
          const matched = courseLookup.get(this.normalizeTitle(paid.title));
          return {
            ...paid,
            courseId: matched?.id ?? null,
            description: matched?.description || 'Paid course content available for this learner.',
            level: matched?.level ?? 'UNKNOWN',
            rating: matched?.rating,
            enrolledStudents: matched?.enrolledStudents ?? 0,
            totalChapters: matched?.totalChapters ?? 0,
            totalDurationMinutes: matched?.totalDurationMinutes,
            thumbnailUrl: matched?.thumbnailUrl,
            isFinished: false
          };
        });

        const cardsWithIds = cards.filter((c) => c.courseId != null);

        if (cardsWithIds.length === 0) {
          this.paidCourses = cards;
          this.applyFilters();
          this.loading = false;
          return;
        }

        const detailsRequests = cardsWithIds.map((card) =>
          this.courseService.getCourseWithChapters(card.courseId as number).pipe(catchError(() => of(null)))
        );

        forkJoin(detailsRequests).subscribe({
          next: (details) => {
            const detailMap = new Map<number, Course>();
            details.forEach((courseDetail) => {
              if (courseDetail?.id != null) {
                detailMap.set(courseDetail.id, courseDetail);
              }
            });

            this.paidCourses = cards.map((card) => {
              if (!card.courseId || !learnerId) {
                return card;
              }

              const detail = detailMap.get(card.courseId);
              return {
                ...card,
                isFinished: this.isCourseFinished(learnerId, card.courseId, detail)
              };
            });

            this.applyFilters();
            this.loading = false;
          },
          error: () => {
            this.paidCourses = cards;
            this.applyFilters();
            this.loading = false;
          }
        });
      },
      error: () => {
        this.errorMessage = 'Unable to load your paid courses right now.';
        this.loading = false;
      }
    });
  }

  private isPaidInvoice(invoice: Invoice): boolean {
    const status = (invoice.status || '').toUpperCase();
    return status === 'PAID' || status === 'SUCCESS' || status === 'COMPLETED';
  }

  private extractPaidCourses(invoices: Invoice[]): PaidCourse[] {
    const map = new Map<string, PaidCourse>();

    invoices.forEach((invoice) => {
      const paidDate = invoice.issueDate || '';
      (invoice.purchasedCourses || []).forEach((courseTitle) => {
        const cleanTitle = (courseTitle || '').trim();
        if (!cleanTitle) {
          return;
        }

        const existing = map.get(cleanTitle);
        if (!existing) {
          map.set(cleanTitle, {
            title: cleanTitle,
            purchaseCount: 1,
            lastPaidDate: paidDate
          });
          return;
        }

        existing.purchaseCount += 1;
        if (new Date(paidDate).getTime() > new Date(existing.lastPaidDate).getTime()) {
          existing.lastPaidDate = paidDate;
        }
      });
    });

    return Array.from(map.values()).sort(
      (a, b) => new Date(b.lastPaidDate).getTime() - new Date(a.lastPaidDate).getTime()
    );
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = [...this.paidCourses];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter((course) =>
        course.title.toLowerCase().includes(term) ||
        course.description.toLowerCase().includes(term)
      );
    }

    this.filteredCourses = filtered;
  }

  getLevelBadgeClass(level: Level | 'UNKNOWN'): string {
    switch (level) {
      case Level.BEGINNER:
        return 'level-beginner';
      case Level.INTERMEDIATE:
        return 'level-intermediate';
      case Level.ADVANCED:
        return 'level-advanced';
      default:
        return 'level-beginner';
    }
  }

  getLevelLabel(level: Level | 'UNKNOWN'): string {
    switch (level) {
      case Level.BEGINNER:
        return 'Beginner';
      case Level.INTERMEDIATE:
        return 'Intermediate';
      case Level.ADVANCED:
        return 'Advanced';
      default:
        return 'Paid';
    }
  }

  formatDuration(minutes?: number): string {
    if (!minutes) {
      return 'N/A';
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${mins}m`;
  }

  getThumbnailUrl(thumbnailUrl?: string, courseId?: number | null): string {
    return this.fileUrlService.getThumbnailUrl(thumbnailUrl || '', courseId || undefined);
  }

  onImageError(event: any): void {
    event.target.style.display = 'none';
    const parent = event.target.parentElement;
    if (parent) {
      parent.classList.add('thumbnail-error');
    }
  }

  openCourseContent(course: MyCourseCard): void {
    if (!course.courseId || !this.learnerId || course.isFinished) {
      return;
    }

    this.router.navigate(['/learner/enrolled-student', course.courseId, this.learnerId]);
  }

  private normalizeTitle(title: string): string {
    return (title || '').trim().toLowerCase().replace(/\s+/g, ' ');
  }

  private isCourseFinished(learnerId: string, courseId: number, detail: Course | null | undefined): boolean {
    if (!detail?.chapters?.length) {
      return false;
    }

    const totalLessons = detail.chapters.reduce((sum, chapter) => {
      return sum + (chapter.contentBlocks?.length || 0);
    }, 0);

    if (totalLessons === 0) {
      return false;
    }

    const key = `enrolled_progress_${learnerId}_${courseId}`;
    const raw = localStorage.getItem(key);
    if (!raw) {
      return false;
    }

    try {
      const parsed = JSON.parse(raw);
      const completedCount = Array.isArray(parsed.completedBlockIds)
        ? parsed.completedBlockIds.length
        : 0;

      return completedCount >= totalLessons;
    } catch {
      return false;
    }
  }

  formatDate(date: string): string {
    if (!date) {
      return 'N/A';
    }

    return new Date(date).toLocaleDateString();
  }
}
