import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { API } from '../../../core/api';
import { AuthService } from '../../../core/auth';
import { humanize } from '../../../core/labels';
import { lastGood } from '../../../core/last-good';
import { Order, OrderStatus } from '../../../core/models';
import { pollResources } from '../../../core/polling';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

const FILTERS: readonly OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'COURIER_ASSIGNED',
  'PICKED_UP',
  'IN_TRANSIT',
  'DELIVERED',
  'DELIVERY_FAILED',
  'CANCELLED',
];

@Component({
  selector: 'app-order-list',
  imports: [RouterLink, StatusBadge, CurrencyPipe, DatePipe],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderList {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly ordersResource = httpResource<Order[]>(() => API.orders, { defaultValue: [] });
  protected readonly orders = lastGood<Order[]>(this.ordersResource, []);
  protected readonly filter = signal<OrderStatus | null>(null);

  protected readonly filters = computed(() => {
    const all = this.orders();
    return FILTERS.map((status) => ({
      status,
      label: humanize(status),
      count: all.filter((o) => o.status === status).length,
    })).filter((f) => f.count > 0);
  });

  protected readonly visible = computed(() => {
    const status = this.filter();
    const all = this.orders();
    return status ? all.filter((o) => o.status === status) : all;
  });

  constructor() {
    pollResources(this.ordersResource);
  }

  protected open(order: Order): void {
    void this.router.navigate(['/orders', order.id]);
  }

  protected itemCount(order: Order): number {
    return order.items.reduce((sum, item) => sum + item.quantity, 0);
  }
}
