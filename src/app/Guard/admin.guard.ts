import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../front-office/services/user.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const userService = inject(UserService);
  const router = inject(Router);

  // Vérifie si l'utilisateur est connecté et possède le rôle 'admin'
  // Note: Assure-toi que la chaîne 'admin' correspond exactement à celle de Keycloak
  if (userService.hasRole('ADMIN')) {
    return true;
  } else {
    // Si non autorisé, redirection vers la page d'accueil ou une page d'erreur
    router.navigate(['/']);
    return false;
  }
};
