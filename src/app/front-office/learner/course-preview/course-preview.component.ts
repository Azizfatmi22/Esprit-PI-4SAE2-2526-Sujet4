import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseService } from '../../services/course.service';
import { FileUrlService } from '../../services/file-url.service';
import { UserService } from '../../services/user.service';
import { Course, Chapter, ContentBlock, CourseAttachment, Level, ContentType } from '../../courses/modules/course.model';
import { CartService } from '../../services/cart.service';
import { CouponService } from '../../services/coupon.service';
@Component({
  selector: 'app-course-preview',
  templateUrl: './course-preview.component.html',
  styleUrls: ['./course-preview.component.scss']
})
export class CoursePreviewComponent implements OnInit {
  course: Course | null = null;
  loading = true;
  errorMessage = '';
  currentUserId: string | null = null;
  selectedChapter: Chapter | null = null;
  selectedContentBlock: ContentBlock | null = null;
  expandedChapters: Set<number> = new Set();
  isRedeemed = false;
  couponCode = '';
  isRedeeming = false;

  // Enums for template
  Level = Level;
  ContentType = ContentType;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private fileUrlService: FileUrlService,
    private userService: UserService,
    private cartService: CartService,
    private couponService: CouponService
  ) { }

  ngOnInit(): void {
    this.currentUserId = this.userService.getUser()?.id || null;
    if (!this.currentUserId) {
      this.userService.loadUser().then((user) => {
        this.currentUserId = user?.id || null;
      }).catch(() => {
        this.currentUserId = null;
      });
    }

    const courseId = this.route.snapshot.paramMap.get('id');
    const queryCoupon = this.route.snapshot.queryParamMap.get('couponCode');
    if (queryCoupon) {
      this.couponCode = queryCoupon;
    }

    if (courseId) {
      this.loadCourse(+courseId);
      this.checkExistingClaim(+courseId);
    } else {
      this.errorMessage = 'Course ID not provided';
      this.loading = false;
    }
  }

  checkExistingClaim(courseId: number): void {
    if (!this.currentUserId) {
      // If user not loaded yet, retry after a short delay
      setTimeout(() => {
        if (this.currentUserId) this.checkExistingClaim(courseId);
      }, 1000);
      return;
    }

    this.couponService.getMyClaims(this.currentUserId).subscribe({
      next: (claims) => {
        const existingClaim = claims.find(c => c.courseId === courseId);
        if (existingClaim) {
          this.isRedeemed = true;
          console.log('✨ Persistent access granted via existing coupon claim');
        }
      }
    });
  }

  loadCourse(courseId: number): void {
    this.loading = true;
    this.courseService.getCourseWithChapters(courseId).subscribe({
      next: (course) => {
        // Allow viewing all courses (status check disabled for development)
        // TODO: Re-enable production status check
        // if (course.status !== 'PUBLISHED') {
        //   this.errorMessage = 'This course is not published yet and cannot be viewed.';
        //   this.loading = false;
        //   return;
        // }

        this.course = course;
        this.loading = false;

        // Auto-select first chapter and first content block
        if (course.chapters && course.chapters.length > 0) {
          this.selectedChapter = course.chapters[0];
          this.expandedChapters.add(course.chapters[0].id!);

          if (course.chapters[0].contentBlocks && course.chapters[0].contentBlocks.length > 0) {
            this.selectedContentBlock = course.chapters[0].contentBlocks[0];
          }
        }
      },
      error: (error) => {
        console.error('Error loading course:', error);
        this.errorMessage = 'Failed to load course. Please try again.';
        this.loading = false;
      }
    });
  }

  selectChapter(chapter: Chapter): void {
    this.selectedChapter = chapter;

    // Toggle expansion
    if (this.expandedChapters.has(chapter.id!)) {
      this.expandedChapters.delete(chapter.id!);
    } else {
      this.expandedChapters.add(chapter.id!);
    }

    // Select first content block of the chapter
    if (chapter.contentBlocks && chapter.contentBlocks.length > 0) {
      this.selectedContentBlock = chapter.contentBlocks[0];
    }
  }

  selectContentBlock(contentBlock: ContentBlock, chapterIndex: number, blockIndex: number): void {
    // Only allow viewing first chapter's first 2 lessons as demo OR if redeemed via coupon
    if (this.isRedeemed || (chapterIndex === 0 && blockIndex < 2)) {
      this.selectedContentBlock = contentBlock;
    } else {
      // Show enrollment required message
      alert('This lesson is locked. Please enroll in the course or use a coupon to access all content.');
    }
  }

  isContentLocked(chapterIndex: number, blockIndex: number): boolean {
    if (this.isRedeemed) return false;
    // First chapter, first 2 lessons are free preview
    return !(chapterIndex === 0 && blockIndex < 2);
  }

  redeemCoupon(): void {
    if (!this.couponCode || !this.currentUserId || !this.course?.id) return;

    this.isRedeeming = true;
    this.couponService.redeemCoupon(this.couponCode, this.currentUserId, this.course.id).subscribe({
      next: () => {
        this.isRedeemed = true;
        this.isRedeeming = false;
        alert('🎟️ Coupon validated! You now have full access to the course.');
      },
      error: (err) => {
        this.isRedeeming = false;
        alert(err.error?.message || 'Invalid coupon code for this course');
      }
    });
  }

  isChapterExpanded(chapterId: number): boolean {
    return this.expandedChapters.has(chapterId);
  }

  getSelectedChapterIndex(): number {
    if (!this.course?.chapters || !this.selectedChapter?.id) return -1;
    return this.course.chapters.findIndex(ch => ch.id === this.selectedChapter!.id);
  }

  getSelectedContentBlockIndex(): number {
    if (!this.selectedChapter?.contentBlocks || !this.selectedContentBlock?.id) return -1;
    return this.selectedChapter.contentBlocks.findIndex(block => block.id === this.selectedContentBlock!.id);
  }

  hasPreviousLesson(): boolean {
    const chapterIndex = this.getSelectedChapterIndex();
    const blockIndex = this.getSelectedContentBlockIndex();
    if (chapterIndex < 0 || blockIndex < 0) return false;
    if (blockIndex > 0) return true;
    if (!this.course?.chapters) return false;

    for (let i = chapterIndex - 1; i >= 0; i--) {
      if ((this.course.chapters[i].contentBlocks?.length || 0) > 0) {
        return true;
      }
    }
    return false;
  }

  hasNextLesson(): boolean {
    const chapterIndex = this.getSelectedChapterIndex();
    const blockIndex = this.getSelectedContentBlockIndex();
    if (chapterIndex < 0 || blockIndex < 0) return false;
    if (!this.selectedChapter?.contentBlocks || !this.course?.chapters) return false;

    if (blockIndex < this.selectedChapter.contentBlocks.length - 1) return true;

    for (let i = chapterIndex + 1; i < this.course.chapters.length; i++) {
      if ((this.course.chapters[i].contentBlocks?.length || 0) > 0) {
        return true;
      }
    }
    return false;
  }

  goToPreviousLesson(): void {
    if (!this.course?.chapters || !this.hasPreviousLesson()) return;

    const chapterIndex = this.getSelectedChapterIndex();
    const blockIndex = this.getSelectedContentBlockIndex();
    if (chapterIndex < 0 || blockIndex < 0) return;

    if (blockIndex > 0) {
      const targetBlock = this.selectedChapter!.contentBlocks![blockIndex - 1];
      this.selectContentBlock(targetBlock, chapterIndex, blockIndex - 1);
      return;
    }

    for (let i = chapterIndex - 1; i >= 0; i--) {
      const blocks = this.course.chapters[i].contentBlocks || [];
      if (blocks.length > 0) {
        const targetChapter = this.course.chapters[i];
        const targetIndex = blocks.length - 1;
        this.selectedChapter = targetChapter;
        this.expandedChapters.add(targetChapter.id!);
        this.selectContentBlock(blocks[targetIndex], i, targetIndex);
        return;
      }
    }
  }

  goToNextLesson(): void {
    if (!this.course?.chapters || !this.hasNextLesson()) return;

    const chapterIndex = this.getSelectedChapterIndex();
    const blockIndex = this.getSelectedContentBlockIndex();
    if (chapterIndex < 0 || blockIndex < 0) return;

    const currentBlocks = this.selectedChapter?.contentBlocks || [];
    if (blockIndex < currentBlocks.length - 1) {
      const targetBlock = currentBlocks[blockIndex + 1];
      this.selectContentBlock(targetBlock, chapterIndex, blockIndex + 1);
      return;
    }

    for (let i = chapterIndex + 1; i < this.course.chapters.length; i++) {
      const blocks = this.course.chapters[i].contentBlocks || [];
      if (blocks.length > 0) {
        const targetChapter = this.course.chapters[i];
        this.selectedChapter = targetChapter;
        this.expandedChapters.add(targetChapter.id!);
        this.selectContentBlock(blocks[0], i, 0);
        return;
      }
    }
  }

  getLessonCounter(): string {
    const chapterIndex = this.getSelectedChapterIndex();
    const blockIndex = this.getSelectedContentBlockIndex();
    if (chapterIndex < 0 || blockIndex < 0 || !this.course?.chapters) return '';

    const totalLessons = this.course.chapters
      .map(ch => ch.contentBlocks?.length || 0)
      .reduce((sum, count) => sum + count, 0);

    let absoluteLesson = 0;
    for (let i = 0; i < chapterIndex; i++) {
      absoluteLesson += this.course.chapters[i].contentBlocks?.length || 0;
    }
    absoluteLesson += blockIndex + 1;

    return `Lesson ${absoluteLesson} of ${totalLessons}`;
  }

  getCurriculumProgress(): string {
    if (!this.course?.chapters) return '0%';

    const totalLessons = this.course.chapters
      .map(ch => ch.contentBlocks?.length || 0)
      .reduce((sum, count) => sum + count, 0);

    if (totalLessons === 0) return '0%';

    const freeLessons = Math.min(2, this.course.chapters[0]?.contentBlocks?.length || 0);
    const percentage = Math.round((freeLessons / totalLessons) * 100);
    return `${percentage}%`;
  }

  getCurriculumProgressWidth(): number {
    if (!this.course?.chapters) return 0;

    const totalLessons = this.course.chapters
      .map(ch => ch.contentBlocks?.length || 0)
      .reduce((sum, count) => sum + count, 0);

    if (totalLessons === 0) return 0;

    const freeLessons = Math.min(2, this.course.chapters[0]?.contentBlocks?.length || 0);
    return Math.max(4, Math.round((freeLessons / totalLessons) * 100));
  }

  getThumbnailUrl(thumbnailUrl?: string, coursId?: number): string {
    return this.fileUrlService.getThumbnailUrl(thumbnailUrl || '', coursId);
  }

  getContentUrl(contentBlock: ContentBlock): string {
    if (!contentBlock.data) return '';

    // If data is already a URL
    if (contentBlock.data.startsWith('http') || contentBlock.data.startsWith('/api')) {
      return this.fileUrlService.getFileUrl(contentBlock.data);
    }

    // Otherwise treat as filename
    switch (contentBlock.type) {
      case ContentType.IMAGE:
        return this.fileUrlService.getImageUrl(contentBlock.data);
      case ContentType.VIDEO:
        return this.fileUrlService.getVideoUrl(contentBlock.data);
      case ContentType.PDF:
        return this.fileUrlService.getPdfUrl(contentBlock.data);
      case ContentType.FILE:
        return this.fileUrlService.getFileUrl(contentBlock.data);
      default:
        return contentBlock.data;
    }
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

  getContentTypeIcon(type: ContentType): string {
    switch (type) {
      case ContentType.TEXT:
        return '📝';
      case ContentType.IMAGE:
        return '🖼️';
      case ContentType.VIDEO:
        return '🎥';
      case ContentType.PDF:
        return '📄';
      case ContentType.FILE:
        return '📁';
      default:
        return '📄';
    }
  }

  onImageError(event: any): void {
    event.target.style.display = 'none';
  }

  enrollInCourse(): void {
    const learnerId = this.currentUserId || this.userService.getUser()?.id || null;
    if (!learnerId) {
      alert('Utilisateur non connecte. Veuillez vous reconnecter.');
      return;
    }

    const courseId = this.course?.id;
    if (!courseId || !this.course) {
      alert('Donnees du cours invalides');
      return;
    }

    const price = this.course.price || 0;
    const title = this.course.title;

    this.cartService.addCourseToCart(learnerId, courseId, title, price).subscribe({
      next: () => {
        alert(`✅ ${title} ajoute au panier !`);
      },
      error: (error) => {
        console.error('Erreur ajout panier:', error);

        if (error.status === 409) {
          alert('Ce cours est deja dans votre panier !');
        } else if (error.status === 401 || error.status === 403) {
          alert('Session expiree. Veuillez vous reconnecter.');
        } else if (error.status === 400) {
          const details = error?.error?.message || error?.error || 'Requete invalide.';
          alert(`Impossible d'ajouter le cours au panier: ${details}`);
        } else {
          const details = error?.error?.message || error?.message || 'Veuillez reessayer.';
          alert(`Erreur lors de l'ajout au panier (${error.status || 'N/A'}): ${details}`);
        }
      }
    });
  }

  getCurrentLearnerId(): string {
    return this.userService.getUser()?.id || '1';
  }

  startEnrolledStudentCourse(): void {
    if (!this.course?.id) return;

    const learnerId = this.getCurrentLearnerId();
    this.router.navigate(['/learner/enrolled-student', this.course.id, learnerId]);
  }

  goBack(): void {
    this.router.navigate(['/learner/courses']);
  }

  downloadAttachment(attachment: CourseAttachment): void {
    if (this.course?.id && attachment.id) {
      this.courseService.downloadAttachment(this.course.id, attachment.id).subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = attachment.fileName;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (error) => {
          console.error('Error downloading attachment:', error);
          alert('Failed to download attachment');
        }
      });
    }
  }
}
