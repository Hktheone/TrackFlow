import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth';
import { Role } from '../../../core/models';

interface DemoAccount {
  username: string;
  role: Role;
  description: string;
}

/** Seeded by the gateway on first start (see DemoUserSeeder). */
const DEMO_PASSWORD = 'trackflow123';
const DEMO_ACCOUNTS: readonly DemoAccount[] = [
  { username: 'admin', role: 'ADMIN', description: 'Everything: all orders, deliveries, riders and users' },
  { username: 'grace', role: 'USER', description: 'Checks out orders and assigns riders to them' },
  { username: 'amara', role: 'RIDER', description: 'Carries assigned deliveries and shares location' },
];

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: '../auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Query params, bound by the router. */
  readonly returnUrl = input<string>();
  readonly expired = input<string>();

  protected readonly demoAccounts = DEMO_ACCOUNTS;
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected useDemo(account: DemoAccount): void {
    this.form.setValue({ username: account.username, password: DEMO_PASSWORD });
    this.submit();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.auth
      .login(username, password)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          const target = this.returnUrl();
          void this.router.navigateByUrl(target && target.startsWith('/') ? target : this.auth.homeUrl());
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          this.error.set(
            err instanceof HttpErrorResponse && err.status === 401
              ? 'Wrong username or password.'
              : 'Could not reach the server. Is the backend running?',
          );
        },
      });
  }
}
