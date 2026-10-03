import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { silentErrors } from './api';
import { AuthResponse, Role, UserAccount } from './models';

const STORAGE_KEY = 'trackflow.session';

interface Session {
  token: string;
  expiresAt: string;
  user: UserAccount;
}

/** Where each role lands after logging in. */
export const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: '/dashboard',
  USER: '/dashboard',
  RIDER: '/deliveries',
};

/**
 * Holds the logged-in session. The access token is a JWT issued by the gateway; it is kept in
 * localStorage so a page reload doesn't log the user out, and dropped as soon as it expires.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly role = computed<Role | null>(() => this.session()?.user.role ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  token(): string | null {
    const session = this.session();
    if (session && new Date(session.expiresAt).getTime() <= Date.now()) {
      this.clear();
      return null;
    }
    return session?.token ?? null;
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  homeUrl(): string {
    const role = this.role();
    return role ? HOME_BY_ROLE[role] : '/login';
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { username, password }, { context: silentErrors() })
      .pipe(tap((response) => this.start(response)));
  }

  register(username: string, password: string, displayName: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { username, password, displayName })
      .pipe(tap((response) => this.start(response)));
  }

  /** Revokes the token on the gateway (best effort) and forgets the session locally either way. */
  logout(): void {
    if (this.token()) {
      this.http.post<void>('/api/auth/logout', null, { context: silentErrors() }).subscribe({ error: () => undefined });
    }
    this.clear();
    void this.router.navigate(['/login']);
  }

  /** Called when the API rejects our token (expired, revoked or the account was disabled). */
  sessionExpired(): void {
    this.clear();
    void this.router.navigate(['/login'], { queryParams: { expired: 1 } });
  }

  private start(response: AuthResponse): void {
    const session: Session = { token: response.accessToken, expiresAt: response.expiresAt, user: response.user };
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Storage unavailable (private mode etc.): the session just won't survive a reload.
    }
  }

  private clear(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // nothing to clean up
    }
  }
}

function restore(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
  } catch {
    return null;
  }
}
