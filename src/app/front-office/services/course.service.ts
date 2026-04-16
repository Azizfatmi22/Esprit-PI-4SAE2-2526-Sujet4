// src/app/front-office/services/course.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../front-office/environment/envirement';
import {
  Course,
  CourseSummary,
  Chapter,
  ContentBlock,
  CourseAttachment,
  CreateChapterRequest,
  CreateContentBlockRequest,
  CourseStatistics,
} from '../courses/modules/course.model';

@Injectable({
  providedIn: 'root',
})
export class CourseService {
  private baseUrl = `${environment.apiGatewayUrl}${environment.courseServiceBasePath}`;

  constructor(private http: HttpClient) {}

  // ==================== COURSE ENDPOINTS ====================

  createCourse(formData: FormData): Observable<Course> {
    // URL: http://localhost:8082/ms-course/api/courses
    return this.http.post<Course>(this.baseUrl, formData);
  }

  // Remove this if your backend doesn't have it - it's causing 404
  // createCourseWithDTO(courseData: CreateCourseRequest): Observable<Course> {
  //   return this.http.post<Course>(`${this.baseUrl}/dto`, courseData);
  // }

  getCourseTitle(id: number): Observable<string> {
    return this.http.get(`${this.baseUrl}/${id}/title`, {
      responseType: 'text',
    });
  }

  getCourseById(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.baseUrl}/${id}`);
  }

  getCourseWithChapters(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.baseUrl}/${id}/with-chapters`);
  }

  getAllCourses(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(this.baseUrl, { params });
  }

  getCoursesByTrainer(trainerId: string): Observable<CourseSummary[]> {
    const token = localStorage.getItem('token');
    return this.http.get<CourseSummary[]>(
      `${this.baseUrl}/trainer/${trainerId}`,
    );
  }
  

  searchCourses(keyword: string): Observable<CourseSummary[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<CourseSummary[]>(`${this.baseUrl}/search`, { params });
  }

  updateCourse(id: number, courseData: FormData): Observable<Course> {
    return this.http.put<Course>(`${this.baseUrl}/${id}`, courseData);
  }

  updateCourseStatus(id: number, status: string): Observable<Course> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Course>(`${this.baseUrl}/${id}/status`, null, {
      params,
    });
  }

  updateCourseRating(id: number, rating: number): Observable<Course> {
    const params = new HttpParams().set('rating', rating.toString());
    return this.http.patch<Course>(`${this.baseUrl}/${id}/rating`, null, {
      params,
    });
  }

  deleteCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getCourseStatistics(trainerId: number): Observable<CourseStatistics> {
    return this.http.get<CourseStatistics>(
      `${this.baseUrl}/trainer/${trainerId}/statistics`,
    );
  }

  checkCourseExists(title: string, trainerId: number): Observable<boolean> {
    const params = new HttpParams()
      .set('title', title)
      .set('trainerId', trainerId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/exists`, { params });
  }

  // ==================== CHAPTER ENDPOINTS ====================

  createChapter(
    courseId: number,
    chapterData: CreateChapterRequest,
  ): Observable<Chapter> {
    return this.http.post<Chapter>(
      `${this.baseUrl}/${courseId}/chapters`,
      chapterData,
    );
  }

  getChaptersByCourse(courseId: number): Observable<Chapter[]> {
    return this.http.get<Chapter[]>(`${this.baseUrl}/${courseId}/chapters`);
  }

  getChapterById(courseId: number, chapterId: number): Observable<Chapter> {
    // Fixed: Added courseId parameter
    return this.http.get<Chapter>(
      `${this.baseUrl}/${courseId}/chapters/${chapterId}`,
    );
  }

  getChapterWithContent(
    courseId: number,
    chapterId: number,
  ): Observable<Chapter> {
    // Fixed: Added courseId parameter
    return this.http.get<Chapter>(
      `${this.baseUrl}/${courseId}/chapters/${chapterId}/with-content`,
    );
  }

  updateChapter(
    courseId: number,
    chapterId: number,
    chapterData: any,
  ): Observable<Chapter> {
    // Fixed: Added courseId parameter
    return this.http.put<Chapter>(
      `${this.baseUrl}/${courseId}/chapters/${chapterId}`,
      chapterData,
    );
  }

  reorderChapter(
    courseId: number,
    chapterId: number,
    newOrderIndex: number,
  ): Observable<Chapter> {
    // Fixed: Added courseId parameter
    const params = new HttpParams().set(
      'newOrderIndex',
      newOrderIndex.toString(),
    );
    return this.http.patch<Chapter>(
      `${this.baseUrl}/${courseId}/chapters/${chapterId}/reorder`,
      null,
      { params },
    );
  }

  deleteChapter(courseId: number, chapterId: number): Observable<void> {
    // Fixed: Added courseId parameter
    return this.http.delete<void>(
      `${this.baseUrl}/${courseId}/chapters/${chapterId}`,
    );
  }

  deleteAllChapters(courseId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${courseId}/chapters`);
  }

  // ==================== CONTENT BLOCK ENDPOINTS ====================

  createContentBlock(
    chapterId: number,
    blockData: CreateContentBlockRequest,
  ): Observable<ContentBlock> {
    // Backend path: /api/courses/chapters/{chapterId}/content-blocks
    return this.http.post<ContentBlock>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks`,
      blockData,
    );
  }

  getContentBlocksByChapter(chapterId: number): Observable<ContentBlock[]> {
    return this.http.get<ContentBlock[]>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks`,
    );
  }

  getContentBlockById(
    chapterId: number,
    blockId: number,
  ): Observable<ContentBlock> {
    return this.http.get<ContentBlock>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks/${blockId}`,
    );
  }

  updateContentBlock(
    chapterId: number,
    blockId: number,
    blockData: any,
  ): Observable<ContentBlock> {
    return this.http.put<ContentBlock>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks/${blockId}`,
      blockData,
    );
  }

  reorderContentBlock(
    chapterId: number,
    blockId: number,
    newOrderIndex: number,
  ): Observable<ContentBlock> {
    const params = new HttpParams().set(
      'newOrderIndex',
      newOrderIndex.toString(),
    );
    return this.http.patch<ContentBlock>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks/${blockId}/reorder`,
      null,
      { params },
    );
  }

  deleteContentBlock(chapterId: number, blockId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks/${blockId}`,
    );
  }

  deleteAllContentBlocks(chapterId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/chapters/${chapterId}/content-blocks`,
    );
  }

  searchContentInCourse(
    courseId: number,
    keyword: string,
  ): Observable<ContentBlock[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<ContentBlock[]>(
      `${this.baseUrl}/chapters/course/${courseId}/search`,
      { params },
    );
  }

  // ==================== ATTACHMENT ENDPOINTS ====================

  uploadAttachment(
    courseId: number,
    file: File,
    category: string,
    description?: string,
  ): Observable<CourseAttachment> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', category);
    if (description) formData.append('description', description);

    // URL: http://localhost:8082/ms-course/api/courses/{courseId}/attachments
    return this.http.post<CourseAttachment>(
      `${this.baseUrl}/${courseId}/attachments`,
      formData,
    );
  }

  updateAttachment(
    courseId: number,
    attachmentId: number,
    updateData: any,
  ): Observable<CourseAttachment> {
    return this.http.put<CourseAttachment>(
      `${this.baseUrl}/${courseId}/attachments/${attachmentId}`,
      updateData,
    );
  }

  updateAttachmentFile(
    courseId: number,
    attachmentId: number,
    file: File,
    category: string,
    description?: string,
  ): Observable<CourseAttachment> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', category);
    if (description) formData.append('description', description);

    return this.http.put<CourseAttachment>(
      `${this.baseUrl}/${courseId}/attachments/${attachmentId}/file`,
      formData,
    );
  }

  getAttachmentsByCourse(courseId: number): Observable<CourseAttachment[]> {
    return this.http.get<CourseAttachment[]>(
      `${this.baseUrl}/${courseId}/attachments`,
    );
  }

  getAttachmentsByCategory(
    courseId: number,
    category: string,
  ): Observable<CourseAttachment[]> {
    return this.http.get<CourseAttachment[]>(
      `${this.baseUrl}/${courseId}/attachments/category/${category}`,
    );
  }

  downloadAttachment(courseId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/${courseId}/attachments/${attachmentId}/download`,
      {
        responseType: 'blob',
      },
    );
  }

  deleteAttachment(courseId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${courseId}/attachments/${attachmentId}`,
    );
  }

  deleteAllAttachments(courseId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${courseId}/attachments`);
  }

  getAttachmentStatistics(courseId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.baseUrl}/${courseId}/attachments/statistics`,
    );
  }

  getTotalAttachmentsSize(courseId: number): Observable<number> {
    return this.http.get<number>(
      `${this.baseUrl}/${courseId}/attachments/total-size`,
    );
  }
}
