import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../front-office/services/keycloak.service';
import { UserService } from '../front-office/services/user.service';

export const roleRedirectGuard: CanActivateFn = (route, state) => {
  const keycloakService = inject(KeycloakService);
  const userService = inject(UserService);
  const router = inject(Router);

  if (keycloakService.isLoggedIn()) {
    const roles = userService.getUser()?.roles || [];

    // Si l'utilisateur est ADMIN et qu'il essaie d'accéder à la racine
    if (roles.includes('ADMIN')) {
      router.navigate(['/admin']);
      return false;
    }
  }

  return true;
};
