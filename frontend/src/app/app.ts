import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth';
import { humanize } from './core/labels';
import { Role } from './core/models';
import { ToastHost } from './shared/toast-host/toast-host';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  roles: readonly Role[];
}

const NAV: readonly NavItem[] = [
  { path: '/dashboard', label: 'Dashboard', icon: '◧', roles: ['ADMIN', 'USER'] },
  { path: '/orders', label: 'Orders', icon: '▤', roles: ['ADMIN', 'USER'] },
  { path: '/deliveries', label: 'Deliveries', icon: '➜', roles: ['ADMIN', 'USER'] },
  { path: '/deliveries', label: 'My deliveries', icon: '➜', roles: ['RIDER'] },
  { path: '/riders', label: 'Riders', icon: '☺', roles: ['ADMIN'] },
  { path: '/users', label: 'Users', icon: '⚿', roles: ['ADMIN'] },
];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastHost],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly auth = inject(AuthService);

  protected readonly nav = computed(() => {
    const role = this.auth.role();
    return role ? NAV.filter((item) => item.roles.includes(role)) : [];
  });

  protected readonly roleLabel = computed(() => {
    const role = this.auth.role();
    return role ? humanize(role) : '';
  });

  protected readonly initials = computed(() =>
    (this.auth.user()?.displayName ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join(''),
  );
}
