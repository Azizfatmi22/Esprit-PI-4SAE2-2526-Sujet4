import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../front-office/services/user.service';

export const trainerGuard: CanActivateFn = (route, state) => {
  const userService = inject(UserService);
  const router = inject(Router);

  if (userService.hasRole('TRAINER')) {
    return true;
  } else {
    router.navigate(['/']);
    return false;
  }
};
