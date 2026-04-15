import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { KeycloakService } from './keycloak.service';
import { jwtDecode } from 'jwt-decode';
import { User } from '../../user';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly STORAGE_KEY = 'currentUser';
  private isBrowser: boolean;

  constructor(
    private keycloakService: KeycloakService,
    @Inject(PLATFORM_ID) platformId: Object,
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  /**
   * Load user info from Keycloak token and save in sessionStorage
   */
  async loadUser(): Promise<User | undefined> {
    const token = this.keycloakService.getToken();
    if (!token) return undefined;

    const decoded: any = jwtDecode(token);

    const rawRoles: string[] = this.keycloakService.getRoles() || decoded.realm_access?.roles || decoded.roles || [];
    const roles: string[] =
      rawRoles.filter(
        (r: string) =>
          !r.startsWith('default-') &&
          r !== 'offline_access' &&
          r !== 'uma_authorization',
      );

    const user: User = {
      id: decoded.sub,
      username: decoded.preferred_username || decoded.username || '',
      email: decoded.email || '',
      fullName:
        decoded.name ||
        `${decoded.given_name || ''} ${decoded.family_name || ''}`.trim(),
      roles: roles,
    };

    // FIX: Only access sessionStorage if in browser
    if (this.isBrowser) {
      sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(user));
    }

    return user;
  }

  /**
   * Get user from sessionStorage
   */
  getUser(): User | null {
    // FIX: Guard against SSR access
    if (this.isBrowser) {
      const data = sessionStorage.getItem(this.STORAGE_KEY);
      return data ? (JSON.parse(data) as User) : null;
    }
    return null;
  }

  /**
   * Clear user data from sessionStorage
   */
  clearUser(): void {
    // FIX: Guard against SSR access
    if (this.isBrowser) {
      sessionStorage.removeItem(this.STORAGE_KEY);
    }
  }

  /**
   * Check if current user has a specific role
   */
  hasRole(role: string): boolean {
    const target = this.normalizeRole(role);
    const tokenRoles = this.keycloakService ? this.keycloakService.getRoles() : [];
    if (tokenRoles.some(r => this.normalizeRole(r) === target)) return true;
    const user = this.getUser();
    return user ? user.roles.some(r => this.normalizeRole(r) === target) : false;
  }

  /**
   * Check if current user is a Trainer
   */
  isTrainer(): boolean {
    return this.hasRole('TRAINER');
  }

  private normalizeRole(role: string): string {
    const normalized = (role || '').trim().toUpperCase();
    return normalized.startsWith('ROLE_') ? normalized.substring(5) : normalized;
  }
}
