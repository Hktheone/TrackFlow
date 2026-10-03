import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { API, TrackFlowApi } from '../../../core/api';
import { AuthService } from '../../../core/auth';
import { humanize } from '../../../core/labels';
import { lastGood } from '../../../core/last-good';
import { ROLES, Role, UserAccount } from '../../../core/models';
import { Notifications } from '../../../core/notifications';

/** ADMIN: every account, its role (from the gateway's roles table) and whether it may log in. */
@Component({
  selector: 'app-user-list',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserList {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(TrackFlowApi);
  private readonly notifications = inject(Notifications);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  private readonly usersResource = httpResource<UserAccount[]>(() => API.users, { defaultValue: [] });
  protected readonly users = lastGood<UserAccount[]>(this.usersResource, []);
  protected readonly roles = ROLES;
  protected readonly humanize = humanize;
  protected readonly showForm = signal(false);
  protected readonly busy = signal(false);

  protected readonly counts = computed(() => {
    const all = this.users();
    return ROLES.map((role) => ({ role, count: all.filter((u) => u.role === role).length }));
  });

  protected readonly form = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._-]{3,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: this.fb.control<Role>('USER', Validators.required),
  });

  protected isSelf(user: UserAccount): boolean {
    return user.username === this.auth.user()?.username;
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.busy.set(true);
    this.api
      .createUser({ ...value, username: value.username.trim(), displayName: value.displayName.trim() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => {
          const riderHint = user.role === 'RIDER' ? ' Add their rider profile on the Riders page.' : '';
          this.notifications.success(`Created ${humanize(user.role).toLowerCase()} ${user.username}.${riderHint}`);
          this.form.reset();
          this.showForm.set(false);
          this.busy.set(false);
          this.usersResource.reload();
        },
        error: () => this.busy.set(false),
      });
  }

  protected changeRole(user: UserAccount, event: Event): void {
    const role = (event.target as HTMLSelectElement).value as Role;
    if (role !== user.role) {
      this.update(user, { role }, `${user.username} is now ${humanize(role).toLowerCase()}`);
    }
  }

  protected toggleEnabled(user: UserAccount): void {
    const enabled = !user.enabled;
    this.update(user, { enabled }, `${user.username} ${enabled ? 'can log in again' : 'is disabled'}`);
  }

  private update(user: UserAccount, changes: { role?: Role; enabled?: boolean }, message: string): void {
    this.busy.set(true);
    this.api
      .updateUser(user.id, changes)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notifications.success(message);
          this.busy.set(false);
          this.usersResource.reload();
        },
        error: () => {
          this.busy.set(false);
          this.usersResource.reload(); // put the select back to the stored role
        },
      });
  }
}
