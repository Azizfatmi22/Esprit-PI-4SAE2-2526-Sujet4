import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, AppNotification } from '../../services/notification.service';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss']
})
export class NotificationBellComponent implements OnInit {
  @Input() learnerId: string | null = null;
  showPanel = false;
  notifications: AppNotification[] = [];
  currentUser: User | null = null;

  unreadCount$: Observable<number>;

  constructor(
    private notifService: NotificationService,
    private userService: UserService
  ) {
    this.unreadCount$ = this.notifService.unreadCount$;
  }

  async ngOnInit(): Promise<void> {
    this.currentUser = this.userService.getUser() || null;
    if (!this.currentUser) {
      this.currentUser = (await this.userService.loadUser()) || null;
    }

    if (!this.learnerId) {
      this.learnerId = this.currentUser?.id ?? null;
    }

    if (!this.learnerId) {
      return;
    }

    this.loadNotifications();
    this.notifService.startPolling(this.learnerId);
  }

  loadNotifications(): void {
    if (!this.learnerId) {
      return;
    }

    this.notifService.getAll(this.learnerId).subscribe(
      (n: AppNotification[]) => this.notifications = n
    );
  }

  togglePanel(): void {
    this.showPanel = !this.showPanel;
    if (this.showPanel) this.loadNotifications();
  }

  markRead(n: AppNotification): void {
    if (!this.learnerId) {
      return;
    }
    const learnerId = this.learnerId;

    if (!n.isRead) {
      this.notifService.markAsRead(n.id).subscribe(() => {
        n.isRead = true;
        this.notifService.getUnreadCount(learnerId).subscribe();
      });
    }
  }

  markAllRead(): void {
    if (!this.learnerId) {
      return;
    }
    const learnerId = this.learnerId;

    this.notifService.markAllAsRead(learnerId).subscribe(() => {
      this.notifications.forEach(n => n.isRead = true);
      this.notifService.getUnreadCount(learnerId).subscribe();
    });
  }

  getIcon(type: string): string {
    const icons: { [key: string]: string } = {
      PAYMENT_SUCCESS:      '✅',
      CART_ABANDONED:       '🛒',
      INSTALLMENT_REMINDER: '📅',
      INSTALLMENT_OVERDUE:  '🚨',
      COURSE_ADDED_TO_CART: '📚'
    };
    return icons[type] || '🔔';
  }
}
