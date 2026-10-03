import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth';
import { Role } from './models';

/** Only for logged-in users; everyone else goes to the login page. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.token() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Login/register pages: an already logged-in user is sent to their home screen instead. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? inject(Router).parseUrl(auth.homeUrl()) : true;
};

/** Restricts a route to some roles; anyone else is sent to their own home screen. */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (!auth.token()) {
      return inject(Router).createUrlTree(['/login']);
    }
    return auth.hasRole(...roles) ? true : inject(Router).parseUrl(auth.homeUrl());
  };
}
