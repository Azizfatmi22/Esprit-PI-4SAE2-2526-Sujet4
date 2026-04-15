import {
  Component,
  OnInit,
  HostListener,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { KeycloakService } from '../../front-office/services/keycloak.service';

interface MenuItem {
  id: string;
  icon: string;
  label: string;
  route: string;
  badge?: string; // Changé de number à string pour correspondre aux données
}

@Component({
  selector: 'app-side-bar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './side-bar.component.html',
  styleUrl: './side-bar.component.scss',
})
export class SideBarComponent implements OnInit {
  isCollapsed = false;
  activeItem = 'statistics';
  hoveredItem: string | null = null;
  isMobile = false;
  isDarkMode = false;
  isBrowser: boolean;
  admin = {
    name: 'Aguil Omar',
    role: 'Admin',
    email: 'omar@formini.com',
    initials: 'AO', // Corrigé de 'OE' à 'AO' pour correspondre au nom
  };

  menuItems: MenuItem[] = [
    {
      id: 'statistics',
      icon: '',
      label: 'Statistics',
      route: '/admin',
    },
    {
      id: 'evaluations',
      icon: '',
      label: 'Evaluations',
      route: '/admin/evaluations',
      badge: '12',
    },
    {
      id: 'badges',
      icon: '',
      label: 'badges',
      route: '/admin/badges',
    },
    {
      id: 'courses',
      icon: '',
      label: 'Courses',
      route: '/admin/courses',
      badge: '4',
    },
    {
      id: 'session-planning',
      icon: '',
      label: 'Session Planning',
      route: '/admin/dashboardSession',
    },
    {
      id: 'payments',
      icon: '',
      label: 'Payments',
      route: '/admin/payments',
      badge: '3',
    },
    {
      id: 'enrollments',
      icon: '',
      label: 'Enrollments',
      route: '/admin/enrollments',
    },
    {
      id: 'installments',
      icon: '',
      label: 'Installments',
      route: '/admin/installments',
      badge: '5',
    },
    {
      id: 'forum',
      icon: '',
      label: 'Forum',
      route: '/admin/forum',
      badge: '8',
    },
    {
      id: 'events',
      icon: '',
      label: 'Events',
      route: '/admin/events',
    },
    {
      id: 'partners',
      icon: '',
      label: 'Partners',
      route: '/admin/partners',
    },
    {
      id: 'trainers',
      icon: '',
      label: 'Trainers',
      route: '/admin/trainers',
    },
    {
      id: 'hr-analysis',
      icon: '',
      label: 'HR Analysis',
      route: '/admin/hr-analysis',
    },
    {
      id: 'jobs',
      icon: '',
      label: 'Jobs',
      route: '/admin/jobs',
    },
  ];

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
    private keycloakService: KeycloakService,
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      this.checkMobile();
    } else {
      this.isMobile = false;
      this.isCollapsed = false;
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    if (!this.isBrowser) {
      return;
    }
    this.checkMobile();
  }

  checkMobile(): void {
    if (!this.isBrowser) {
      return;
    }
    this.isMobile = window.innerWidth < 768;
    if (this.isMobile) {
      this.isCollapsed = true;
    }
  }

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  setActiveItem(itemId: string): void {
    this.activeItem = itemId;
  }

  onItemHover(itemId: string | null): void {
    this.hoveredItem = itemId;
  }

  navigateTo(route: string, itemId: string): void {
    this.activeItem = itemId;
    this.router.navigate([route]);
  }

  // Méthodes utilitaires pour le template
  hasBadge(badge: string | undefined): boolean {
    return badge !== undefined && badge !== null && Number(badge) > 0;
  }

  isActive(itemId: string): boolean {
    return this.activeItem === itemId;
  }

  logout() {
    this.keycloakService.logout();
    if (this.isBrowser) {
      localStorage.clear();
    }
    console.log('User logged out from app');
  }
}
