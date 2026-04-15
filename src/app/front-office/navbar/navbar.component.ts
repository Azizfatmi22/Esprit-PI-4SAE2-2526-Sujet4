import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { KeycloakService } from '../services/keycloak.service';
import { UserService } from '../services/user.service';
import { User } from '../../user';
import { NotificationBellComponent } from '../notification/notification-bell/notification-bell.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, NotificationBellComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  showSessionDropdown = false;
  showUserDropdown = false;
  showTrainingDropdown = false;
  user: User | null = null;
  avatarUrl: string = 'images/avatar.jpg';
  constructor(
    private router: Router,
    private keycloakService: KeycloakService,
    private userService: UserService,
  ) {}

  async ngOnInit(): Promise<void> {
    const loadedUser = await this.userService.loadUser();
    this.user = loadedUser ?? null; // Converts undefined to null

    if (!this.user) {
      this.router.navigate(['/login']);
    }
  }

  isSessionRouteActive(): boolean {
    const url = this.router.url;
    return url.includes('/sessions') || url.includes('/planning');
  }
  isTrainingRouteActive(): boolean {
    const url = this.router.url;
    return (
      url.includes('/sessions') ||
      url.includes('/planning') ||
      url.includes('/learning-paths')
    );
  }
  handleAvatarError(event: any): void {
    event.target.style.display = 'none';
  }
  isTrainer(): boolean {
    return !!this.user && this.user.roles.includes('TRAINER');
  }

  isLearner(): boolean {
    return (
      !!this.user &&
      this.user.roles.length === 1 &&
      this.user.roles[0] === 'LEARNER'
    );
  }

  logout() {
    this.keycloakService.logout();
    // Optionally clear local state
    localStorage.clear();
    console.log('User logged out from app');
  }
  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToMyInvoices(): void {
    this.router.navigate(['/cart/my-invoices']);
  }

  goToMyCourses(): void {
    this.router.navigate(['/learner/mycourses']);
  }
  goToRefund(): void {
    this.router.navigate(['/refunds']);
  }
  goToMyCertifications() {
    this.router.navigate(['/certifications']);
  }
}
