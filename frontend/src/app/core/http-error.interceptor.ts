import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { SILENT_ERRORS } from './api';
import { AuthService } from './auth';
import { ProblemDetail } from './models';
import { Notifications } from './notifications';

/**
 * Global error handling for API calls: a rejected token ends the session and returns to the login
 * page; anything else becomes a readable toast, using the services' ProblemDetail body when present.
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(Notifications);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !req.context.get(SILENT_ERRORS)) {
        if (error.status === 401 && auth.isLoggedIn()) {
          notifications.error('Your session has ended. Please log in again.');
          auth.sessionExpired();
        } else if (error.status !== 401) {
          notifications.error(describe(error));
        }
      }
      return throwError(() => error);
    }),
  );
};

function describe(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Cannot reach the API gateway. Is the backend running?';
  }
  if (error.status === 503) {
    return 'Service unavailable. It may still be registering with Eureka.';
  }
  if (error.status === 403) {
    return "You don't have permission to do that.";
  }
  const problem = error.error as ProblemDetail | null;
  if (problem && typeof problem === 'object') {
    if (problem.errors) {
      const fields = Object.entries(problem.errors).map(([field, message]) => `${field} ${message}`);
      return `${problem.detail ?? 'Validation failed'}: ${fields.join(', ')}`;
    }
    if (problem.detail) {
      return problem.detail;
    }
  }
  return `Request failed (${error.status} ${error.statusText})`;
}
