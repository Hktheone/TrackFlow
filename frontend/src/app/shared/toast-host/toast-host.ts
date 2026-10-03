import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { Notifications } from '../../core/notifications';

@Component({
  selector: 'app-toast-host',
  templateUrl: './toast-host.html',
  styleUrl: './toast-host.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastHost {
  protected readonly notifications = inject(Notifications);
}
