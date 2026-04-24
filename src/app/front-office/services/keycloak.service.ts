import { Injectable } from '@angular/core';
import Keycloak, { KeycloakInstance, KeycloakInitOptions } from 'keycloak-js';

@Injectable({
  providedIn: 'root',
})
export class KeycloakService {
  private keycloak?: Keycloak;

  /**
   * Initialize Keycloak
   */
  init(): Promise<boolean> {
    return new Promise((resolve, reject) => {
      if (typeof window === 'undefined') {
        // SSR : do not execute
        resolve(false);
        return;
      }

      this.keycloak = new Keycloak({
        url: 'http://localhost:8180',
        realm: 'myrealm',
        clientId: 'angular-frontend',
      });
      const options: KeycloakInitOptions = {
        onLoad: 'login-required',
        checkLoginIframe: false, // Disable the hidden iframe which causes loops in local dev
        pkceMethod: 'S256',
        enableLogging: true,
        silentCheckSsoRedirectUri:
          window.location.origin + '/assets/silent-check-sso.html',
      };

      this.keycloak
        .init(options)
        .then((authenticated) => {
          console.log('Keycloak authenticated:', authenticated);
          resolve(authenticated);
        })
        .catch((err) => {
          console.error('Keycloak init error:', err);
          reject(err);
        });
    });
  }

  /**
   * Get current token
   */
  getToken(): string {
    return this.keycloak?.token || '';
  }

  /**
   * Trigger login
   */
  login(): void {
    this.keycloak?.login();
  }

  /**
   * Trigger logout
   */
  logout(): void {
    if (!this.keycloak) return;

    const redirectUri = window.location.origin;

    this.keycloak
      .logout({ redirectUri })
      .then(() => {
        console.log('Logged out from Keycloak');
      })
      .catch((err) => {
        console.error('Logout error:', err);
      });
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn(): boolean {
    return !!this.keycloak?.token;
  }

  /**
   * Get username from token
   */
  getUsername(): string {
    return this.keycloak?.tokenParsed?.['preferred_username'] ?? '';
  }

  /**
   * Get roles from token
   */
  getRoles(): string[] {
    return this.keycloak?.tokenParsed?.realm_access?.roles ?? [];
  }
}
