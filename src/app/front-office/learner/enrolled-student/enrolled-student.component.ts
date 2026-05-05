import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CourseService } from '../../services/course.service';
import { EvaluationService } from '../../services/evaluation.service';
import { FileUrlService } from '../../services/file-url.service';
import { Course, Chapter, ContentBlock, ContentType, CourseAttachment } from '../../courses/modules/course.model';

interface FlatLesson {
  chapterIndex: number;
  blockIndex: number;
  block: ContentBlock;
}

@Component({
  selector: 'app-enrolled-student',
  templateUrl: './enrolled-student.component.html',
  styleUrls: ['./enrolled-student.component.scss']
})
export class EnrolledStudentComponent implements OnInit {
  course: Course | null = null;
  learnerId = '';
  courseId = 0;

  loading = true;
  errorMessage = '';
  selectedChapterIndex = 0;
  selectedBlockIndex = 0;

  completedBlockIds = new Set<number>();
  expandedChapters = new Set<number>();

  evaluationLoading = false;

  ContentType = ContentType;
  private pdfViewerUrlCache = new Map<string, SafeResourceUrl>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private evaluationService: EvaluationService,
    private fileUrlService: FileUrlService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    const courseIdParam = this.route.snapshot.paramMap.get('courseId');
    const learnerIdParam = this.route.snapshot.paramMap.get('learnerId');

    if (!courseIdParam || !learnerIdParam) {
      this.errorMessage = 'Missing learner or course information.';
      this.loading = false;
      return;
    }

    this.courseId = Number(courseIdParam);
    this.learnerId = learnerIdParam;
    this.loadCourse();
  }

  private loadCourse(): void {
    this.loading = true;
    this.courseService.getCourseWithChapters(this.courseId).subscribe({
      next: (course) => {
        this.course = course;
        this.loading = false;

        if (course.chapters && course.chapters.length > 0) {
          this.selectedChapterIndex = 0;
          this.selectedBlockIndex = 0;
          this.expandedChapters.add(course.chapters[0].id || 0);
        }

        this.restoreProgress();
      },
      error: (err) => {
        console.error('Failed to load enrolled course:', err);
        this.errorMessage = 'Unable to load the enrolled course.';
        this.loading = false;
      }
    });
  }

  get selectedChapter(): Chapter | null {
    if (!this.course?.chapters?.length) return null;
    return this.course.chapters[this.selectedChapterIndex] || null;
  }

  get selectedBlock(): ContentBlock | null {
    const chapter = this.selectedChapter;
    if (!chapter?.contentBlocks?.length) return null;
    return chapter.contentBlocks[this.selectedBlockIndex] || null;
  }

  get flatLessons(): FlatLesson[] {
    if (!this.course?.chapters) return [];

    const lessons: FlatLesson[] = [];
    this.course.chapters.forEach((chapter, chapterIndex) => {
      (chapter.contentBlocks || []).forEach((block, blockIndex) => {
        lessons.push({ chapterIndex, blockIndex, block });
      });
    });

    return lessons;
  }

  get totalLessons(): number {
    return this.flatLessons.length;
  }

  get completedLessonsCount(): number {
    return this.flatLessons.filter(item => !!item.block.id && this.completedBlockIds.has(item.block.id)).length;
  }

  get isCourseCompleted(): boolean {
    return this.totalLessons > 0 && this.completedLessonsCount === this.totalLessons;
  }

  get currentLessonLinearIndex(): number {
    return this.flatLessons.findIndex(
      item => item.chapterIndex === this.selectedChapterIndex && item.blockIndex === this.selectedBlockIndex,
    );
  }

  get progressPercent(): number {
    if (!this.totalLessons) return 0;
    return Math.round((this.completedLessonsCount / this.totalLessons) * 100);
  }

  isChapterExpanded(chapterId: number): boolean {
    return this.expandedChapters.has(chapterId);
  }

  toggleChapter(chapter: Chapter): void {
    const id = chapter.id || 0;
    if (!id) return;
    if (this.expandedChapters.has(id)) {
      this.expandedChapters.delete(id);
    } else {
      this.expandedChapters.add(id);
    }
  }

  isBlockLocked(chapterIndex: number, blockIndex: number): boolean {
    const targetIndex = this.flatLessons.findIndex(
      x => x.chapterIndex === chapterIndex && x.blockIndex === blockIndex,
    );

    if (targetIndex < 0) return true;

    if (targetIndex === 0) return false;

    const previous = this.flatLessons[targetIndex - 1];
    return !previous.block.id || !this.completedBlockIds.has(previous.block.id);
  }

  selectBlock(chapterIndex: number, blockIndex: number): void {
    if (this.isBlockLocked(chapterIndex, blockIndex)) {
      alert('This lesson is locked. Complete the previous lesson first.');
      return;
    }

    this.selectedChapterIndex = chapterIndex;
    this.selectedBlockIndex = blockIndex;

    const chapter = this.course?.chapters?.[chapterIndex];
    if (chapter?.id) this.expandedChapters.add(chapter.id);

    this.persistProgress();
  }

  canMarkCurrentAsDone(): boolean {
    const block = this.selectedBlock;
    if (!block?.id) return false;
    if (block.type === ContentType.VIDEO) return false;
    return !this.completedBlockIds.has(block.id);
  }

  markCurrentLessonDone(): void {
    const block = this.selectedBlock;
    if (!block?.id) return;

    if (block.type === ContentType.VIDEO) {
      alert('Video lesson is marked complete only after video ends.');
      return;
    }

    this.completedBlockIds.add(block.id);
    this.persistProgress();
    this.goToNextLesson();
  }

  onVideoEnded(): void {
    const block = this.selectedBlock;
    if (!block?.id) return;

    this.completedBlockIds.add(block.id);
    this.persistProgress();
  }

  hasPreviousLesson(): boolean {
    return this.currentLessonLinearIndex > 0;
  }

  hasNextLesson(): boolean {
    return this.currentLessonLinearIndex >= 0 && this.currentLessonLinearIndex < this.totalLessons - 1;
  }

  goToPreviousLesson(): void {
    if (!this.hasPreviousLesson()) return;

    const prev = this.flatLessons[this.currentLessonLinearIndex - 1];
    this.selectBlock(prev.chapterIndex, prev.blockIndex);
  }

  goToNextLesson(): void {
    if (!this.hasNextLesson()) return;

    const next = this.flatLessons[this.currentLessonLinearIndex + 1];
    if (this.isBlockLocked(next.chapterIndex, next.blockIndex)) return;

    this.selectBlock(next.chapterIndex, next.blockIndex);
  }

  getLessonCounterLabel(): string {
    const idx = this.currentLessonLinearIndex;
    if (idx < 0) return '';
    return `Lesson ${idx + 1} / ${this.totalLessons}`;
  }

  getLevelLabel(level: any): string {
    return (level || '').toString();
  }

  getLevelBadgeClass(level: any): string {
    const l = (level || '').toString().toLowerCase();
    if (l.includes('beginner'))     return 'level-beginner';
    if (l.includes('intermediate')) return 'level-intermediate';
    if (l.includes('advanced'))     return 'level-advanced';
    return 'level-beginner';
  }

  getContentTypeLabel(type: ContentType): string {
    switch (type) {
      case ContentType.TEXT:
        return 'Text Lesson';
      case ContentType.IMAGE:
        return 'Image Lesson';
      case ContentType.VIDEO:
        return 'Video Lesson';
      case ContentType.PDF:
        return 'PDF Document';
      case ContentType.FILE:
        return 'Downloadable File';
      default:
        return type;
    }
  }

  getChapterDescription(chapter: Chapter | null): string {
    if (!chapter) return '';
    return chapter.description?.trim() || 'No chapter description provided.';
  }

  getBlockDescription(block: ContentBlock | null): string {
    if (!block) return '';

    if (block.type === ContentType.TEXT && block.data) {
      const plainText = block.data.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
      if (plainText.length > 160) {
        return `${plainText.slice(0, 160)}...`;
      }
      return plainText || 'No lesson details provided.';
    }

    if (block.type === ContentType.VIDEO) return 'Complete this video until the end to unlock the next lesson.';
    if (block.type === ContentType.PDF) return 'Read this document to continue your learning sequence.';
    if (block.type === ContentType.FILE) return 'Open or download this file to continue.';
    if (block.type === ContentType.IMAGE) return 'Study this visual lesson and then continue.';

    return 'No lesson details provided.';
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
        return '📦';
      default:
        return '📘';
    }
  }

  getContentUrl(block: ContentBlock): string {
    if (!block?.data) return '';
    return this.fileUrlService.getFileUrl(block.data);
  }

  getPdfViewerUrl(block: ContentBlock): SafeResourceUrl {
    const url = this.getContentUrl(block);
    if (!url) {
      return this.sanitizer.bypassSecurityTrustResourceUrl('');
    }

    const cached = this.pdfViewerUrlCache.get(url);
    if (cached) {
      return cached;
    }

    const safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    this.pdfViewerUrlCache.set(url, safeUrl);
    return safeUrl;
  }

  isPptFile(block: ContentBlock | null): boolean {
    if (!block?.data) return false;
    const lowered = block.data.toLowerCase();
    return lowered.endsWith('.ppt') || lowered.endsWith('.pptx');
  }

  formatDuration(minutes?: number): string {
    if (!minutes) return 'N/A';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`;
  }

  downloadAttachment(attachment: CourseAttachment): void {
    const attachmentId = attachment.id;
    if (!this.course?.id) return;

    if (!attachmentId && attachment.fileUrl) {
      const directUrl = this.fileUrlService.getFileUrl(attachment.fileUrl);
      const link = document.createElement('a');
      link.href = directUrl;
      link.download = attachment.fileName || 'attachment';
      link.target = '_blank';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      return;
    }

    if (!attachmentId) {
      alert('Attachment ID is missing, unable to download this file.');
      return;
    }

    this.courseService.downloadAttachment(this.course.id, attachmentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = attachment.fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Attachment download failed:', error);
        alert('Failed to download attachment.');
      }
    });
  }

  goToCourseEvaluation(): void {
    if (!this.isCourseCompleted) {
      alert('Complete all lessons first to unlock the final evaluation.');
      return;
    }

    this.evaluationLoading = true;
    this.evaluationService.getAllEvaluations().subscribe({
      next: (evaluations: any[]) => {
        this.evaluationLoading = false;
        const evaluation = evaluations.find(ev => Number(ev.courseId) === this.courseId);

        if (!evaluation?.id) {
          alert('No evaluation found for this course yet.');
          return;
        }

        const type = (evaluation.type || evaluation.typeAssessment || '').toString().toUpperCase();
        if (type === 'EXAM') {
          this.router.navigate(['/examPlayer', evaluation.id]);
        } else {
          this.router.navigate(['/quizPlayer', evaluation.id]);
        }
      },
      error: (err) => {
        this.evaluationLoading = false;
        console.error('Failed to load evaluations:', err);
        alert('Failed to open course evaluation.');
      }
    });
  }

  goBackToPreview(): void {
    this.router.navigate(['/learner/courses', this.courseId]);
  }

  private getProgressStorageKey(): string {
    return `enrolled_progress_${this.learnerId}_${this.courseId}`;
  }

  private persistProgress(): void {
    const payload = {
      selectedChapterIndex: this.selectedChapterIndex,
      selectedBlockIndex: this.selectedBlockIndex,
      completedBlockIds: Array.from(this.completedBlockIds),
    };

    localStorage.setItem(this.getProgressStorageKey(), JSON.stringify(payload));
  }

  private restoreProgress(): void {
    const raw = localStorage.getItem(this.getProgressStorageKey());
    if (!raw) return;

    try {
      const parsed = JSON.parse(raw);
      const validCompletedIds = new Set<number>();
      const existingIds = new Set(this.flatLessons.map(item => item.block.id).filter(Boolean) as number[]);

      (parsed.completedBlockIds || []).forEach((id: number) => {
        if (existingIds.has(id)) validCompletedIds.add(id);
      });

      this.completedBlockIds = validCompletedIds;

      const chapterIndex = Number(parsed.selectedChapterIndex ?? 0);
      const blockIndex = Number(parsed.selectedBlockIndex ?? 0);

      if (
        this.course?.chapters?.[chapterIndex]?.contentBlocks?.[blockIndex] &&
        !this.isBlockLocked(chapterIndex, blockIndex)
      ) {
        this.selectedChapterIndex = chapterIndex;
        this.selectedBlockIndex = blockIndex;
      }

      const chapter = this.course?.chapters?.[this.selectedChapterIndex];
      if (chapter?.id) this.expandedChapters.add(chapter.id);
    } catch {
      // ignore corrupted progress
    }
  }
}
