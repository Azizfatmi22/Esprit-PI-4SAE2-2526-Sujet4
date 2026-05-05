import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CourseService } from '../../services/course.service';
import { FileUrlService } from '../../services/file-url.service';
import { Level, ContentType, AttachmentCategory } from '../modules/course.model';
import { UserService } from '../../services/user.service';

interface ChapterForm {
  id?: number; // Existing chapter ID (for edit mode)
  title: string;
  description: string;
  contentBlocks: ContentBlockForm[];
  isExpanded?: boolean;
}

interface ContentBlockForm {
  type: ContentType;
  title: string;
  data: string;
  file?: File;
  filePreview?: string;
  orderIndex?: number;
  id?: number; // Existing block ID (for edit mode)
  isNew?: boolean; // Track if this is a newly added block
}

@Component({
  selector: 'app-course-create',
  templateUrl: './course-create.component.html',
  styleUrls: ['./course-create.component.scss']
})
export class CourseCreateComponent implements OnInit {
  currentStep = 1;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  
  // Edit Mode
  isEditMode = false;
  editingCourseId: number | null = null;
  
  // Step 1: Course Info
  courseForm!: FormGroup;
  thumbnailFile: File | null = null;
  thumbnailPreview: string | null = null;
  existingThumbnailUrl: string | null = null;
  
  // Step 2: Chapters and Content
  chapters: ChapterForm[] = [];
  
  // Step 3: Attachments
  attachments: Array<{
    id?: number; // Existing attachment ID
    file?: File; 
    category: AttachmentCategory; 
    description: string; 
    preview?: string;
    isNew?: boolean; // Track if newly added
  }> = [];
  
  // Created course ID (after step 1)
  createdCourseId: number | null = null;
  
  // Pending content block creation (for file types)
  pendingContentBlockType: ContentType | null = null;
  pendingChapterIndex: number | null = null;
  
  // Pending attachment creation
  pendingAttachmentCategory: AttachmentCategory | null = null;
  pendingAttachmentIndex: number | null = null;
  
  levels = Object.values(Level);
  contentTypes = [ContentType.TEXT, ContentType.IMAGE, ContentType.VIDEO, ContentType.FILE, ContentType.PDF];
  attachmentCategories = Object.values(AttachmentCategory);
  
  private readonly MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
  private readonly ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg'];

  constructor(
    private fb: FormBuilder,
    private courseService: CourseService,
    private fileUrlService: FileUrlService,
    private router: Router,
    private route: ActivatedRoute,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.initCourseForm();
    
    // Check if we're in edit mode
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.editingCourseId = parseInt(id, 10);
        this.loadCourseForEdit(this.editingCourseId);
      }
    });
  }

  loadCourseForEdit(courseId: number): void {
    this.courseService.getCourseWithChapters(courseId).subscribe({
      next: (course) => {
        // Set course info
        this.courseForm.patchValue({
          title: course.title,
          description: course.description,
          level: course.level,
          price: course.price,
          durationMinutes: course.durationMinutes,
          status: course.status,
          trainerId: course.trainerId
        });

        // Set existing thumbnail if available
        if (course.thumbnailUrl) {
          this.existingThumbnailUrl = this.fileUrlService.getThumbnailUrl(course.thumbnailUrl, courseId);
          this.thumbnailPreview = this.existingThumbnailUrl;
        }

        // Load chapters and content blocks
        if (course.chapters && course.chapters.length > 0) {
          this.chapters = course.chapters.map(ch => ({
            id: ch.id, // Add chapter ID for delete operations
            title: ch.title || '',
            description: ch.description || '',
            contentBlocks: (ch.contentBlocks || []) as ContentBlockForm[],
            isExpanded: false
          }));

          // Load file previews for content blocks
          this.loadContentBlockPreviews(courseId, course.chapters);
        }

        // Load attachments
        if (course.attachments && course.attachments.length > 0) {
          this.attachments = course.attachments.map(att => {
            // The fileUrl from database is already correct: /api/courses/uploads/cours_{courseId}/attachments/{filename}
            // Just add the gateway prefix via FileUrlService
            const fullFileUrl = att.fileUrl ? this.fileUrlService.getThumbnailUrl(att.fileUrl, courseId) : undefined;
            
            return {
              id: att.id,
              file: undefined, // No file object for existing attachments
              category: att.category,
              description: att.description || '',
              preview: fullFileUrl,
              isNew: false // Mark as existing
            };
          });
        }

        this.successMessage = 'Course loaded for editing';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        console.error('Error loading course:', error);
        this.errorMessage = 'Failed to load course for editing';
      }
    });
  }

  loadContentBlockPreviews(courseId: number, chapters: any[]): void {
    chapters.forEach((chapter, chapterIdx) => {
      if (chapter.contentBlocks && chapter.contentBlocks.length > 0) {
        chapter.contentBlocks.forEach((block: any, blockIdx: number) => {
          // Mark as existing block (not newly added)
          this.chapters[chapterIdx].contentBlocks[blockIdx].isNew = false;
          this.chapters[chapterIdx].contentBlocks[blockIdx].id = block.id;
          
          // Skip TEXT blocks
          if (block.type === 'TEXT') {
            return;
          }

          // Build file URL from the data field
          if (block.data) {
            let fileUrl = block.data;
            
            // Construct the correct API URL based on backend storage structure:
            // uploads/cours_{courseId}/chapitre_{chapterId}/content_block_{blockId}/{type}/{filename}
            if (!fileUrl.startsWith('http') && !fileUrl.startsWith('/api')) {
              // Get the content block ID
              const blockId = block.id || '';
              const filename = block.data.split('/').pop() || block.data;
              
              // Determine the folder type (images, videos, pdf, files)
              let folderType = 'images';
              if (block.type === 'VIDEO') {
                folderType = 'videos';
              } else if (block.type === 'PDF') {
                folderType = 'pdf';
              } else if (block.type === 'FILE') {
                folderType = 'files';
              }
              
              // Build the correct path: /api/courses/uploads/cours_X/chapitre_Y/content_block_Z/{type}/{filename}
              fileUrl = `/api/courses/uploads/cours_${courseId}/chapitre_${chapter.id}/content_block_${blockId}/${folderType}/${filename}`;
            }
            
            // Skip temporary URLs
            if (fileUrl.includes('/pending')) {
              return;
            }
            
            // Use FileUrlService to properly construct the full URL with gateway prefix
            const fullFileUrl = this.fileUrlService.getThumbnailUrl(fileUrl, courseId);
            
            // Store both for saving later
            this.chapters[chapterIdx].contentBlocks[blockIdx].data = fileUrl;
            this.chapters[chapterIdx].contentBlocks[blockIdx].filePreview = fullFileUrl;
            
            // Load preview based on type
            if (block.type === 'IMAGE') {
              // For images, use the full URL
              this.chapters[chapterIdx].contentBlocks[blockIdx].filePreview = fullFileUrl;
            } else if (block.type === 'VIDEO') {
              // For videos, use the full URL
              this.chapters[chapterIdx].contentBlocks[blockIdx].filePreview = fullFileUrl;
            } else if (block.type === 'PDF' || block.type === 'FILE') {
              // For PDFs and files, extract filename
              const filename = block.data.split('/').pop() || block.data;
              if (!this.chapters[chapterIdx].contentBlocks[blockIdx].title) {
                this.chapters[chapterIdx].contentBlocks[blockIdx].title = filename;
              }
              // Keep the full URL
              this.chapters[chapterIdx].contentBlocks[blockIdx].filePreview = fullFileUrl;
            }
          }
        });
      }
    });
  }

  initCourseForm(): void {
    this.courseForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
      level: ['', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      durationMinutes: [null, [Validators.min(1)]],
      status: ['DRAFT', Validators.required], // DRAFT or PUBLISHED
      trainerId: [1, [Validators.required]] // TODO: Get from logged-in user
    });
  }

  // ==================== STEP 1: COURSE INFO ====================
  
  onThumbnailChange(event: any): void {
    const file = event.target.files[0];
    this.errorMessage = '';

    if (file) {
      if (!this.ALLOWED_IMAGE_TYPES.includes(file.type)) {
        this.errorMessage = 'Only JPG and PNG images are allowed for thumbnail.';
        this.resetThumbnail();
        return;
      }

      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage = 'Thumbnail size exceeds 5MB limit.';
        this.resetThumbnail();
        return;
      }

      this.thumbnailFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.thumbnailPreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  resetThumbnail(): void {
    this.thumbnailFile = null;
    this.thumbnailPreview = null;
    const fileInput = document.getElementById('thumbnail') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
  }

  async saveStep1(): Promise<void> {
    if (this.courseForm.invalid) {
      this.markFormGroupTouched(this.courseForm);
      this.errorMessage = 'Please fill in all required fields correctly.';
      return;
    }

    // Don't create course yet, just validate and move to step 2
    this.successMessage = 'Course information saved! Now add chapters.';
    
    setTimeout(() => {
      this.successMessage = '';
      this.currentStep = 2;
    }, 500);
  }

  // ==================== STEP 2: CHAPTERS & CONTENT ====================
  
  addChapter(): void {
    this.chapters.push({
      id: undefined,
      title: '',
      description: '',
      contentBlocks: [],
      isExpanded: true
    });
  }

  toggleChapter(index: number): void {
    this.chapters[index].isExpanded = !this.chapters[index].isExpanded;
  }

  removeChapter(index: number): void {
    const chapter = this.chapters[index];
    
    // If this is an existing chapter in edit mode, delete it from the backend
    if (this.isEditMode && this.editingCourseId && chapter.id) {
      if (confirm(`Are you sure you want to delete chapter "${chapter.title}"? All content blocks will be deleted.`)) {
        this.courseService.deleteChapter(this.editingCourseId, chapter.id).subscribe({
          next: () => {
            console.log(`Chapter ${chapter.id} deleted successfully`);
            this.chapters.splice(index, 1);
            this.successMessage = `Chapter "${chapter.title}" deleted successfully`;
            setTimeout(() => this.successMessage = '', 3000);
          },
          error: (err) => {
            console.error('Error deleting chapter:', err);
            this.errorMessage = `Failed to delete chapter: ${err.error?.message || 'Unknown error'}`;
          }
        });
      }
    } else {
      // For new chapters, just remove from the array
      this.chapters.splice(index, 1);
    }
  }

  addContentBlock(chapterIndex: number): void {
    const currentBlocks = this.chapters[chapterIndex].contentBlocks;
    this.chapters[chapterIndex].contentBlocks.push({
      type: ContentType.TEXT,
      title: '',
      data: '',
      file: undefined,
      filePreview: undefined,
      orderIndex: currentBlocks.length
    });
  }

  addContentBlockWithType(chapterIndex: number, type: ContentType): void {
    // For TEXT type, create block immediately
    if (type === ContentType.TEXT) {
      const currentBlocks = this.chapters[chapterIndex].contentBlocks;
      this.chapters[chapterIndex].contentBlocks.push({
        type: type,
        title: '',
        data: '',
        file: undefined,
        filePreview: undefined,
        orderIndex: currentBlocks.length,
        isNew: true  // Mark as newly added
      });
    } else {
      // For IMAGE, VIDEO, FILE, PDF - trigger file picker first
      this.pendingContentBlockType = type;
      this.pendingChapterIndex = chapterIndex;
      
      // Trigger hidden file input
      const fileInput = document.getElementById(`file-picker-${chapterIndex}`) as HTMLInputElement;
      if (fileInput) {
        // Set accept attribute based on type
        fileInput.accept = this.getFileAcceptAttribute(type);
        fileInput.click();
      }
    }
  }

  onPendingFileSelected(event: any, chapterIndex: number): void {
    const file = event.target.files[0];
    if (!file || !this.pendingContentBlockType) {
      // Reset if no file selected
      this.pendingContentBlockType = null;
      this.pendingChapterIndex = null;
      return;
    }

    // Create the content block with the selected file
    const currentBlocks = this.chapters[chapterIndex].contentBlocks;
    const newBlock: ContentBlockForm = {
      type: this.pendingContentBlockType,
      title: '',
      data: '',
      file: file,
      filePreview: undefined,
      orderIndex: currentBlocks.length,
      isNew: true  // Mark as newly added
    };

    // Generate preview for images and videos
    if (file.type.startsWith('image/') || file.type.startsWith('video/')) {
      const reader = new FileReader();
      reader.onload = () => {
        newBlock.filePreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }

    this.chapters[chapterIndex].contentBlocks.push(newBlock);

    // Reset pending state
    this.pendingContentBlockType = null;
    this.pendingChapterIndex = null;

    // Clear the file input
    event.target.value = '';
  }

  removeContentBlock(chapterIndex: number, blockIndex: number): void {
    const block = this.chapters[chapterIndex].contentBlocks[blockIndex];
    const chapter = this.chapters[chapterIndex];

    // If this is an existing block in edit mode, delete it from backend
    if (this.isEditMode && this.editingCourseId && chapter.id && block.id) {
      if (confirm(`Are you sure you want to delete content block "${block.title || ''}"?`)) {
        this.courseService.deleteContentBlock(chapter.id, block.id).subscribe({
          next: () => {
            console.log(`Content block ${block.id} deleted successfully`);
            this.chapters[chapterIndex].contentBlocks.splice(blockIndex, 1);
            this.updateContentBlockOrder(chapterIndex);
          },
          error: (err) => {
            console.error('Error deleting content block:', err);
            this.errorMessage = `Failed to delete content block: ${err.error?.message || 'Unknown error'}`;
          }
        });
      }
    } else {
      // New/unsaved block - just remove from UI
      this.chapters[chapterIndex].contentBlocks.splice(blockIndex, 1);
      this.updateContentBlockOrder(chapterIndex);
    }
  }

  moveContentBlockUp(chapterIndex: number, blockIndex: number): void {
    if (blockIndex === 0) return;
    const blocks = this.chapters[chapterIndex].contentBlocks;
    [blocks[blockIndex], blocks[blockIndex - 1]] = [blocks[blockIndex - 1], blocks[blockIndex]];
    this.updateContentBlockOrder(chapterIndex);
  }

  moveContentBlockDown(chapterIndex: number, blockIndex: number): void {
    const blocks = this.chapters[chapterIndex].contentBlocks;
    if (blockIndex === blocks.length - 1) return;
    [blocks[blockIndex], blocks[blockIndex + 1]] = [blocks[blockIndex + 1], blocks[blockIndex]];
    this.updateContentBlockOrder(chapterIndex);
  }

  private updateContentBlockOrder(chapterIndex: number): void {
    this.chapters[chapterIndex].contentBlocks.forEach((block, index) => {
      block.orderIndex = index;
    });
  }

  onContentFileChange(event: any, chapterIndex: number, blockIndex: number): void {
    const file = event.target.files[0];
    if (!file) return;

    const block = this.chapters[chapterIndex].contentBlocks[blockIndex];
    block.file = file;
    // Mark as modified if it was an existing block
    block.isNew = true;  // Reset to "new" since file was changed

    // Generate preview for images
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = () => {
        block.filePreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
    // Generate preview for videos
    else if (file.type.startsWith('video/')) {
      const reader = new FileReader();
      reader.onload = () => {
        block.filePreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  triggerFileInputForBlock(chapterIndex: number, blockIndex: number): void {
    // Find the file input within this block and click it
    const blockElement = document.querySelector(
      `.chapter-item:nth-child(${chapterIndex + 1}) .block-item:nth-child(${blockIndex + 1}) input[type="file"]`
    ) as HTMLInputElement;
    if (blockElement) {
      // Set accept attribute based on block type
      const block = this.chapters[chapterIndex].contentBlocks[blockIndex];
      blockElement.accept = this.getFileAcceptAttribute(block.type);
      blockElement.click();
    }
  }

  removeContentFile(chapterIndex: number, blockIndex: number): void {
    const block = this.chapters[chapterIndex].contentBlocks[blockIndex];
    block.file = undefined;
    block.filePreview = undefined;
    // Mark as new since file was removed (will require file on save)
    block.isNew = true;
  }

  async saveStep2(): Promise<void> {
    // Validate chapters
    if (this.chapters.length === 0) {
      this.errorMessage = 'Please add at least one chapter.';
      return;
    }

    // Validate each chapter has a title and at least one content block
    for (let i = 0; i < this.chapters.length; i++) {
      const chapter = this.chapters[i];
      
      if (!chapter.title || chapter.title.trim() === '') {
        this.errorMessage = `Chapter ${i + 1} must have a title.`;
        return;
      }

      if (chapter.contentBlocks.length === 0) {
        this.errorMessage = `Chapter ${i + 1} must have at least one content block.`;
        return;
      }

      // Validate content blocks
      for (let j = 0; j < chapter.contentBlocks.length; j++) {
        const block = chapter.contentBlocks[j];
        
        if (block.type === 'TEXT') {
          if (!block.data || block.data.trim() === '') {
            this.errorMessage = `Content block ${j + 1} in chapter ${i + 1} must have text content.`;
            return;
          }
        } else {
          // For file-type blocks, check if NEW block or existing block with file
          const isNewBlock = block.isNew !== false;
          const hasFile = block.file instanceof File;
          const hasExistingFile = block.filePreview !== undefined;
          
          if (isNewBlock && !hasFile) {
            this.errorMessage = `Content block ${j + 1} in chapter ${i + 1} must have a file uploaded.`;
            return;
          }
          
          // If not new and no file, it's an existing block that wasn't modified - that's OK
          if (!isNewBlock && !hasFile && !hasExistingFile) {
            this.errorMessage = `Content block ${j + 1} in chapter ${i + 1} must have a file.`;
            return;
          }
        }
      }
    }

    this.successMessage = 'Chapters validated! Now add attachments.';
    
    setTimeout(() => {
      this.successMessage = '';
      this.currentStep = 3;
    }, 500);
  }

  // ==================== STEP 3: ATTACHMENTS ====================
  
  addAttachment(): void {
    this.attachments.push({
      id: undefined, // No ID for new attachments
      file: null as any,
      category: AttachmentCategory.RESOURCES,
      description: '',
      preview: undefined,
      isNew: true // Mark as newly added
    });
  }

  addAttachmentWithCategory(category: AttachmentCategory): void {
    this.pendingAttachmentCategory = category;
    this.pendingAttachmentIndex = null;
  }

  triggerAddAttachmentFileInput(): void {
    // This is called from the "Add Attachment" button
    // Set default category if not set
    if (!this.pendingAttachmentCategory) {
      this.pendingAttachmentCategory = AttachmentCategory.RESOURCES;
    }
    this.pendingAttachmentIndex = null;
  }

  onPendingAttachmentFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) {
      // Reset if no file selected
      this.pendingAttachmentCategory = null;
      this.pendingAttachmentIndex = null;
      return;
    }

    // Check if this is an update or new addition
    if (this.pendingAttachmentIndex !== null && this.pendingAttachmentIndex >= 0) {
      // Update existing attachment - mark that it has been modified
      this.attachments[this.pendingAttachmentIndex].file = file;
      this.attachments[this.pendingAttachmentIndex].preview = undefined;

      // Generate preview for images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = () => {
          this.attachments[this.pendingAttachmentIndex!].preview = reader.result as string;
        };
        reader.readAsDataURL(file);
      }
    } else if (this.pendingAttachmentCategory) {
      // Create new attachment
      const newAttachment: {
        id?: number;
        file: File;
        category: AttachmentCategory;
        description: string;
        preview?: string;
        isNew?: boolean;
      } = {
        file: file,
        category: this.pendingAttachmentCategory,
        description: '',
        preview: undefined,
        isNew: true // Mark as new
      };

      // Generate preview for images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = () => {
          newAttachment.preview = reader.result as string;
        };
        reader.readAsDataURL(file);
      }

      this.attachments.push(newAttachment);
    }

    // Reset pending state
    this.pendingAttachmentCategory = null;
    this.pendingAttachmentIndex = null;

    // Clear the file input
    event.target.value = '';
  }

  changeAttachmentFile(index: number): void {
    // Store the index to update existing attachment
    this.pendingAttachmentIndex = index;
    this.pendingAttachmentCategory = this.attachments[index].category;
  }

  removeAttachment(index: number): void {
    const attachment = this.attachments[index];

    // If this is an existing attachment in edit mode, delete it from backend
    if (this.isEditMode && this.editingCourseId && attachment.id) {
      if (confirm('Are you sure you want to delete this attachment?')) {
        this.courseService.deleteAttachment(this.editingCourseId, attachment.id).subscribe({
          next: () => {
            console.log(`Attachment ${attachment.id} deleted successfully`);
            this.attachments.splice(index, 1);
          },
          error: (err) => {
            console.error('Error deleting attachment:', err);
            this.errorMessage = `Failed to delete attachment: ${err.error?.message || 'Unknown error'}`;
          }
        });
      }
    } else {
      // New/unsaved attachment - just remove from UI
      this.attachments.splice(index, 1);
    }
  }

  removeAttachmentFile(index: number): void {
    this.attachments[index].file = null as any;
    this.attachments[index].preview = undefined;
  }

  onAttachmentFileChange(event: any, index: number): void {
    const file = event.target.files[0];
    if (file) {
      this.attachments[index].file = file;
      
      // Preview for images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = () => {
          this.attachments[index].preview = reader.result as string;
        };
        reader.readAsDataURL(file);
      }
    }
  }

 async saveStep3(navigateToEvaluation: boolean = true): Promise<void> {
  this.isSubmitting = true;
  this.errorMessage = '';

  try {
    const isUpdate = this.isEditMode && this.editingCourseId;
    console.log(`=== Starting course ${isUpdate ? 'update' : 'creation'} ===`);
    
    // Step 1: Create or Update the course
    const formData = new FormData();
    formData.append('title', this.courseForm.get('title')?.value);
    formData.append('description', this.courseForm.get('description')?.value);
    formData.append('level', this.courseForm.get('level')?.value);
    formData.append('price', this.courseForm.get('price')?.value.toString());
    formData.append('status', this.courseForm.get('status')?.value);
    formData.append('trainerId', this.userService.getUser()?.id.toString() || '12');
    
    const durationMinutes = this.courseForm.get('durationMinutes')?.value;
    if (durationMinutes) {
      formData.append('durationMinutes', durationMinutes.toString());
    }

    // Only append thumbnail if a new file was selected
    if (this.thumbnailFile) {
      formData.append('thumbnail', this.thumbnailFile);
    }

    let courseResponse;
    if (isUpdate && this.editingCourseId) {
      console.log('Updating course with status:', this.courseForm.get('status')?.value);
      courseResponse = await this.courseService.updateCourse(this.editingCourseId, formData).toPromise();
    } else {
      console.log('Creating course with status:', this.courseForm.get('status')?.value);
      courseResponse = await this.courseService.createCourse(formData).toPromise();
    }
    
    this.createdCourseId = courseResponse?.id || null;
    console.log(`Course ${isUpdate ? 'updated' : 'created'} with ID:`, this.createdCourseId);

    if (!this.createdCourseId) {
      throw new Error(`Failed to ${isUpdate ? 'update' : 'create'} course - no ID returned`);
    }

    // Step 2: Create or update chapters and content blocks
    console.log(`${isUpdate ? 'Updating' : 'Creating'} ${this.chapters.length} chapters...`);
    for (let i = 0; i < this.chapters.length; i++) {
      const chapter = this.chapters[i];

      const chapterData = {
        title: chapter.title,
        description: chapter.description,
        orderIndex: i
      };

      let chapterId: number | undefined;

      if (isUpdate && chapter.id) {
        console.log(`Updating chapter ${i + 1} with ID:`, chapter.id);
        const updatedChapter = await this.courseService.updateChapter(
          this.createdCourseId,
          chapter.id,
          chapterData
        ).toPromise();
        chapterId = updatedChapter?.id || chapter.id;
      } else {
        console.log(`Creating chapter ${i + 1}:`, chapter.title);
        const createdChapter = await this.courseService.createChapter(this.createdCourseId, chapterData).toPromise();
        chapterId = createdChapter?.id;
        console.log(`Chapter ${i + 1} created with ID:`, chapterId);
      }

      if (!chapterId) {
        console.error('Failed to create/update chapter - no ID returned');
        continue;
      }

      // Create or update content blocks for this chapter
      console.log(`Processing ${chapter.contentBlocks.length} content blocks for chapter ${i + 1}...`);
      for (let j = 0; j < chapter.contentBlocks.length; j++) {
        const block = chapter.contentBlocks[j];

        console.log(`Processing content block ${j + 1} (${block.type})...`);

        // TEXT blocks: create or update directly
        if (block.type === ContentType.TEXT) {
          const contentBlockData = {
            type: block.type,
            title: block.title,
            data: block.data,
            orderIndex: j
          };

          if (isUpdate && block.id) {
            console.log(`Updating TEXT content block ${j + 1} with ID:`, block.id);
            await this.courseService.updateContentBlock(chapterId, block.id, contentBlockData).toPromise();
          } else {
            console.log(`Creating TEXT content block ${j + 1}...`);
            await this.courseService.createContentBlock(chapterId, contentBlockData).toPromise();
          }
        }
        // File-type blocks (IMAGE, VIDEO, PDF, FILE)
        else {
          try {
            const isExistingBlock = !!block.id && block.isNew === false;
            const hasNewFile = block.file instanceof File;

            // Existing block without new file: just update metadata/order and keep existing file URL
            if (isUpdate && isExistingBlock && !hasNewFile) {
              console.log(`Updating existing file-type content block ${j + 1} (ID: ${block.id}) metadata only`);
              const updateData = {
                type: block.type,
                title: block.title || this.getDefaultBlockTitle(block.type, j + 1),
                data: block.data,
                orderIndex: j
              };
              await this.courseService.updateContentBlock(chapterId, block.id!, updateData).toPromise();
              continue;
            }

            // For new blocks or modified existing blocks, a file is required
            if (!hasNewFile) {
              this.errorMessage = `Content block ${j + 1} in chapter ${i + 1} must have a file uploaded.`;
              this.isSubmitting = false;
              return;
            }

            let contentBlockId = block.id;

            // If this is a brand new block, create a temp one to obtain an ID
            if (!isExistingBlock || !contentBlockId) {
              const tempContentBlockData = {
                type: block.type,
                title: block.title || this.getDefaultBlockTitle(block.type, j + 1),
                data: `/api/courses/uploads/pending`,
                orderIndex: j
              };

              console.log(`Creating ${block.type} content block ${j + 1} with temp data...`);
              const createdBlock = await this.courseService.createContentBlock(chapterId, tempContentBlockData).toPromise();
              contentBlockId = createdBlock?.id;

              console.log('Content block created with ID:', contentBlockId);

              if (!contentBlockId || contentBlockId === 0) {
                throw new Error(`Failed to get valid content block ID. Received: ${contentBlockId}`);
              }
            } else {
              console.log(`Updating file for existing content block ${contentBlockId}...`);
            }

            // Upload the file for this content block ID
            const uploadResponse = await this.fileUrlService.uploadFile(
              block.file as File,
              block.type as any,
              this.createdCourseId,
              chapterId,
              contentBlockId!
            ).toPromise();

            if (!uploadResponse?.fileUrl) {
              throw new Error(`File upload succeeded but no fileUrl returned. Response: ${JSON.stringify(uploadResponse)}`);
            }

            console.log('File uploaded successfully to:', uploadResponse.fileUrl);

            // Update the content block with the actual file URL
            const updateData = {
              type: block.type,
              title: block.title || this.getDefaultBlockTitle(block.type, j + 1),
              data: uploadResponse.fileUrl,
              orderIndex: j
            };

            console.log(`Saving file URL to content block ${contentBlockId}...`);
            await this.courseService.updateContentBlock(chapterId, contentBlockId!, updateData).toPromise();
            console.log(`Content block ${contentBlockId} updated with file URL`);
          } catch (uploadErr: any) {
            console.error('Error in file upload process:', uploadErr);
            const errMsg = (uploadErr?.error?.message) || (uploadErr?.message) || JSON.stringify(uploadErr);
            this.errorMessage = `Failed to upload file for content block ${j + 1} in chapter ${i + 1}: ${errMsg}`;
            this.isSubmitting = false;
            return;
          }
        }
      }
    }

    // Step 3: Upload/Update attachments
    console.log('Processing', this.attachments.length, 'attachments...');
    for (let i = 0; i < this.attachments.length; i++) {
      const attachment = this.attachments[i];
      
      // Check if this is a new attachment or an existing one being updated
      if (attachment.isNew) {
        // New attachment - upload it
        if (attachment.file) {
          console.log(`Uploading new attachment ${i + 1} (${attachment.category})...`);
          await this.courseService.uploadAttachment(
            this.createdCourseId,
            attachment.file,
            attachment.category,
            attachment.description || ''
          ).toPromise();
          console.log(`Attachment ${i + 1} uploaded successfully`);
        }
      } else if (attachment.id) {
        // Existing attachment
        if (attachment.file) {
          // File was changed - update with new file
          console.log(`Updating existing attachment ${attachment.id} with new file...`);
          const formData = new FormData();
          formData.append('file', attachment.file);
          formData.append('category', attachment.category);
          formData.append('description', attachment.description || '');
          
          await this.courseService.updateAttachmentFile(
            this.createdCourseId,
            attachment.id,
            attachment.file,
            attachment.category,
            attachment.description || ''
          ).toPromise();
          console.log(`Attachment ${attachment.id} updated successfully`);
        } else {
          // No new file - just update metadata if needed
          console.log(`Updating metadata for existing attachment ${attachment.id}...`);
          const updateData = {
            category: attachment.category,
            description: attachment.description
          };
          await this.courseService.updateAttachment(this.createdCourseId, attachment.id, updateData).toPromise();
          console.log(`Attachment ${attachment.id} metadata updated`);
        }
      }
    }

    console.log('=== Course creation completed successfully ===');
    this.successMessage = 'Course created successfully with all content!';
    
    // ✅ NEW: Navigate to evaluation page if user is trainer
    if (navigateToEvaluation ) {
      console.log('Navigating to evaluation page for course ID:', this.createdCourseId);
      
      // Small delay to show success message before navigation
      setTimeout(() => {
        this.successMessage = '';
        // Navigate to evaluation page with the course ID
        this.router.navigate(['/evaluation'], {
          queryParams: { courseId: this.createdCourseId }
        });
      }, 1500); // Show success message for 1.5 seconds then navigate
    } else {
      // If not navigating to evaluation, just reset and show success
      setTimeout(() => {
        this.successMessage = '';
        this.isSubmitting = false;
        // Optionally navigate to course list or stay
      }, 1500);
    }
    
  } catch (err: any) {
    console.error('=== Error creating course ===');
    console.error('Error:', err);
    console.error('Error details:', err.error);
    console.error('Error message:', err.message);
    
    // Extract the actual error message from the backend
    const backendMessage = err.error?.message || err.message;
    this.errorMessage = backendMessage || 'Failed to create course. Please try again.';
    
    this.isSubmitting = false;
  }
}
  // ==================== NAVIGATION ====================
  
  nextStep(): void {
    if (this.currentStep === 1) {
      this.saveStep1();
    } else if (this.currentStep === 2) {
      this.saveStep2();
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  canGoNext(): boolean {
    if (this.currentStep === 1) {
      return this.courseForm.valid;
    } else if (this.currentStep === 2) {
      // Must have at least 1 chapter
      if (this.chapters.length === 0) return false;
      
      // Each chapter must have a title and at least 1 content block
      return this.chapters.every(chapter => {
        if (!chapter.title || chapter.title.trim() === '') return false;
        if (chapter.contentBlocks.length === 0) return false;
        
        // Each content block must have data or file
        return chapter.contentBlocks.every(block => {
          if (block.type === 'TEXT') {
            return block.data && block.data.trim() !== '';
          } else {
            return !!block.file;
          }
        });
      });
    }
    return true;
  }

  onCancel(): void {
    if (confirm('Are you sure you want to cancel? All progress will be lost.')) {
      this.router.navigate(['/courses']);
    }
  }

  // ==================== HELPERS ====================
  
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.courseForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.courseForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) return `${fieldName} is required`;
      if (field.errors['minlength']) return `${fieldName} is too short`;
      if (field.errors['maxlength']) return `${fieldName} is too long`;
      if (field.errors['min']) return `${fieldName} must be at least ${field.errors['min'].min}`;
    }
    return '';
  }

  getContentTypeIcon(type: ContentType): string {
    const icons: { [key in ContentType]: string } = {
      [ContentType.TEXT]: '📝',
      [ContentType.IMAGE]: '🖼️',
      [ContentType.VIDEO]: '🎥',
      [ContentType.FILE]: '📁',
      [ContentType.PDF]: '📄',
      [ContentType.QUIZ]: '❓',
      [ContentType.ASSIGNMENT]: '📋'
    };
    return icons[type] || '📄';
  }

  getLevelIcon(level: Level): string {
    const icons: { [key in Level]: string } = {
      [Level.BEGINNER]: '🌱',
      [Level.INTERMEDIATE]: '🚀',
      [Level.ADVANCED]: '⭐'
    };
    return icons[level] || '📚';
  }

  getCategoryIcon(category: AttachmentCategory): string {
    const icons: { [key in AttachmentCategory]: string } = {
      [AttachmentCategory.SYLLABUS]: '📋',
      [AttachmentCategory.PREREQUISITES]: '📌',
      [AttachmentCategory.RESOURCES]: '📚'
    };
    return icons[category] || '📎';
  }

  getDefaultBlockTitle(type: ContentType, blockNumber: number): string {
    const typeNames: { [key in ContentType]: string } = {
      [ContentType.TEXT]: 'Text Content',
      [ContentType.IMAGE]: 'Image',
      [ContentType.VIDEO]: 'Video',
      [ContentType.FILE]: 'Document',
      [ContentType.PDF]: 'PDF File',
      [ContentType.QUIZ]: 'Quiz',
      [ContentType.ASSIGNMENT]: 'Assignment'
    };
    return `${typeNames[type]} ${blockNumber}`;
  }

  getFileAcceptAttribute(type: ContentType): string {
    switch (type) {
      case ContentType.IMAGE:
        return 'image/png,image/jpeg,image/jpg,image/gif,image/webp';
      case ContentType.VIDEO:
        return 'video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo';
      case ContentType.PDF:
        return 'application/pdf';
      case ContentType.FILE:
        return '.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar';
      default:
        return '*/*';
    }
  }
  async createCourseAndGoToEvaluation(): Promise<void> {
  // Call your existing saveStep3 method with true to navigate to evaluation
  await this.saveStep3(true);
}

  async saveCourseChanges(): Promise<void> {
    // Call saveStep3 with false to not navigate to evaluation, just back to catalog
    await this.saveStep3(false);
    // Navigate back to courses after successful save
    setTimeout(() => {
      if (!this.errorMessage) {
        this.router.navigate(['/courses']);
      }
    }, 1000);
  }

  goBackToCatalog(): void {
    if (confirm('Are you sure you want to discard changes and go back to the catalog?')) {
      this.router.navigate(['/courses']);
    }
  }
}
