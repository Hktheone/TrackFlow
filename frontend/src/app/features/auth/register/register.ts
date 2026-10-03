import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth';
import { Notifications } from '../../../core/notifications';

/** Self-service sign-up. Always creates a USER account; admins grant other roles from the Users page. */
@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: '../auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(Notifications);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._-]{3,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password, displayName } = this.form.getRawValue();
    this.submitting.set(true);
    this.auth
      .register(username.trim(), password, displayName.trim())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.notifications.success(`Welcome, ${response.user.displayName}!`);
          void this.router.navigateByUrl(this.auth.homeUrl());
        },
        error: () => this.submitting.set(false),
      });
  }
}
