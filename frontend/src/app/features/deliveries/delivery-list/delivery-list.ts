import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { API } from '../../../core/api';
import { AuthService } from '../../../core/auth';
import { VEHICLE_ICONS, humanize } from '../../../core/labels';
import { lastGood } from '../../../core/last-good';
import { Delivery, DeliveryStatus, TERMINAL_DELIVERY_STATUSES } from '../../../core/models';
import { pollResources } from '../../../core/polling';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

const FILTERS: readonly DeliveryStatus[] = [
  'PENDING_ASSIGNMENT',
  'ASSIGNED',
  'PICKED_UP',
  'IN_TRANSIT',
  'DELIVERED',
  'FAILED',
  'CANCELLED',
];

type Filter = DeliveryStatus | 'ACTIVE' | null;

@Component({
  selector: 'app-delivery-list',
  imports: [StatusBadge, DatePipe],
  templateUrl: './delivery-list.html',
  styleUrl: './delivery-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryList {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly deliveriesResource = httpResource<Delivery[]>(() => API.deliveries, { defaultValue: [] });
  protected readonly deliveries = lastGood<Delivery[]>(this.deliveriesResource, []);
  protected readonly filter = signal<Filter>('ACTIVE');
  protected readonly vehicleIcons = VEHICLE_ICONS;

  protected readonly activeCount = computed(
    () => this.deliveries().filter((d) => !TERMINAL_DELIVERY_STATUSES.includes(d.status)).length,
  );

  protected readonly filters = computed(() => {
    const all = this.deliveries();
    return FILTERS.map((status) => ({
      status,
      label: humanize(status),
      count: all.filter((d) => d.status === status).length,
    })).filter((f) => f.count > 0);
  });

  protected readonly visible = computed(() => {
    const filter = this.filter();
    const all = this.deliveries();
    if (filter === 'ACTIVE') {
      return all.filter((d) => !TERMINAL_DELIVERY_STATUSES.includes(d.status));
    }
    return filter ? all.filter((d) => d.status === filter) : all;
  });

  constructor() {
    pollResources(this.deliveriesResource);
  }

  protected open(delivery: Delivery): void {
    void this.router.navigate(['/deliveries', delivery.id]);
  }
}
