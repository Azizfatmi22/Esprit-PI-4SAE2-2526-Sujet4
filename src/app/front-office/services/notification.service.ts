import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, BehaviorSubject } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common'; // ✅

const API_URL = 'http://localhost:8085/msenrollment/notifications';

export interface AppNotification {
  id: number;
  learnerId: string;
  type: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private unreadCount = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCount.asObservable();

  // ✅ Injecter PLATFORM_ID pour détecter SSR vs Browser
  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  startPolling(learnerId: string): void {
    // ✅ Ne démarrer le polling QUE dans le navigateur
    if (!isPlatformBrowser(this.platformId)) return;

    interval(3000).pipe(
      switchMap(() => this.getUnreadCount(learnerId))
    ).subscribe(res => this.unreadCount.next(res.count));
  }

  getAll(learnerId: string): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(`${API_URL}/learner/${learnerId}`);
  }

  getUnread(learnerId: string): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(`${API_URL}/learner/${learnerId}/unread`);
  }

  getUnreadCount(learnerId: string): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${API_URL}/learner/${learnerId}/count`).pipe(
      tap(res => this.unreadCount.next(res.count))
    );
  }

  markAsRead(notificationId: number): Observable<void> {
    return this.http.put<void>(`${API_URL}/${notificationId}/read`, {});
  }

  markAllAsRead(learnerId: string): Observable<void> {
    return this.http.put<void>(`${API_URL}/learner/${learnerId}/read-all`, {});
  }
}