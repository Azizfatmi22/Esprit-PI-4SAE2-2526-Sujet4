import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../front-office/environment/envirement';
import { Course } from '../../front-office/courses/modules/course.model';

export interface AdminCourseStats {
  totalCourses: number;
  publishedCourses: number;
  draftCourses: number;
  totalEnrollments: number;
}

@Injectable({
  providedIn: 'root',
})
export class AdminCourseService {
  private baseUrl = `${environment.apiGatewayUrl}${environment.courseServiceBasePath}`;

  constructor(private http: HttpClient) {}

  /** Get all courses (paginated) */
  getAllCourses(page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(this.baseUrl, { params });
  }

  /** Get a single course with chapters */
  getCourseById(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.baseUrl}/${id}/with-chapters`);
  }

  /** Update course status (DRAFT | PUBLISHED | ARCHIVED) */
  updateCourseStatus(id: number, status: string): Observable<Course> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Course>(`${this.baseUrl}/${id}/status`, null, { params });
  }

  /** Delete a course */
  deleteCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Search courses by keyword */
  searchCourses(keyword: string): Observable<Course[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<Course[]>(`${this.baseUrl}/search`, { params });
  }
}
