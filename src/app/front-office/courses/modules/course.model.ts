// src/app/front-office/course/models/course.model.ts
export enum Level {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
}

export enum ContentType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  VIDEO = 'VIDEO',
  FILE = 'FILE',
  PDF = 'PDF',
  QUIZ = 'QUIZ',
  ASSIGNMENT = 'ASSIGNMENT',
}

export enum AttachmentCategory {
  SYLLABUS = 'SYLLABUS',
  PREREQUISITES = 'PREREQUISITES',
  RESOURCES = 'RESOURCES',
}

export interface Course {
  id?: number;
  title: string;
  description: string;
  level: Level;
  price: number;
  durationMinutes?: number;
  status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  trainerId: String;
  enrolledStudents?: number;
  rating?: number;
  thumbnailUrl?: string;
  createdAt?: Date;
  updatedAt?: Date;
  chapters?: Chapter[];
  attachments?: CourseAttachment[];
  totalChapters?: number;
}

export interface CourseSummary {
  id: number;
  title: string;
  description: string;
  level: Level;
  price: number;
  status: string;
  trainerId: number;
  enrolledStudents: number;
  rating: number;
  thumbnailUrl?: string;
  totalChapters: number;
  totalDurationMinutes?: number;
}

export interface Chapter {
  id?: number;
  title: string;
  description?: string;
  orderIndex: number;
  courseId?: number;
  contentBlocks?: ContentBlock[];
  totalContentBlocks?: number;
}

export interface ContentBlock {
  id?: number;
  type: ContentType;
  orderIndex: number;
  data: string;
  title?: string;
  chapterId?: number;
}

export interface CourseAttachment {
  id?: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  fileUrl: string;
  category: AttachmentCategory;
  description?: string;
  courseId?: number;
  createdAt?: Date;
}

export interface CreateCourseRequest {
  title: string;
  description: string;
  level: Level;
  price: number;
  durationMinutes?: number;
  trainerId: number;
  thumbnailUrl?: string;
}

export interface UpdateCourseRequest {
  title?: string;
  description?: string;
  level?: Level;
  price?: number;
  durationMinutes?: number;
  status?: string;
  thumbnailUrl?: string;
}

export interface CreateChapterRequest {
  title: string;
  description?: string;
  orderIndex?: number;
}

export interface CreateContentBlockRequest {
  type: ContentType;
  orderIndex?: number;
  data: string;
  title?: string;
}

export interface CourseStatistics {
  trainerId: number;
  totalCourses: number;
  publishedCourses: number;
  draftCourses: number;
  archivedCourses: number;
  totalEnrollments: number;
  averageRating: number;
  coursesByLevel: { [key in Level]?: number };
  coursesByStatus: { [key: string]: number };
  totalChapters: number;
  totalContentBlocks: number;
  totalAttachments: number;
  totalAttachmentsSize: number;
}
