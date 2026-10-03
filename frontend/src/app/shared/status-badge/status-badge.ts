import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { humanize, statusTone } from '../../core/labels';
import { DeliveryStatus, OrderStatus } from '../../core/models';

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadge {
  readonly status = input.required<OrderStatus | DeliveryStatus>();

  protected readonly label = computed(() => humanize(this.status()));
  protected readonly tone = computed(() => statusTone(this.status()));
}
