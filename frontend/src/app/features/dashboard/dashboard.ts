import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { API } from '../../core/api';
import { AuthService } from '../../core/auth';
import { VEHICLE_ICONS } from '../../core/labels';
import { lastGood } from '../../core/last-good';
import { Courier, Delivery, Order, TERMINAL_DELIVERY_STATUSES, TERMINAL_ORDER_STATUSES } from '../../core/models';
import { pollResources } from '../../core/polling';
import { StatusBadge } from '../../shared/status-badge/status-badge';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, StatusBadge, CurrencyPipe, DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  protected readonly auth = inject(AuthService);
  private readonly ordersResource = httpResource<Order[]>(() => API.orders, { defaultValue: [] });
  private readonly deliveriesResource = httpResource<Delivery[]>(() => API.deliveries, { defaultValue: [] });
  private readonly couriersResource = httpResource<Courier[]>(() => API.couriers, { defaultValue: [] });
  protected readonly orders = lastGood<Order[]>(this.ordersResource, []);
  protected readonly deliveries = lastGood<Delivery[]>(this.deliveriesResource, []);
  protected readonly couriers = lastGood<Courier[]>(this.couriersResource, []);
  protected readonly vehicleIcons = VEHICLE_ICONS;

  protected readonly stats = computed(() => {
    const orders = this.orders();
    const deliveries = this.deliveries();
    const couriers = this.couriers();
    return {
      openOrders: orders.filter((o) => !TERMINAL_ORDER_STATUSES.includes(o.status)).length,
      awaitingConfirmation: orders.filter((o) => o.status === 'PENDING').length,
      activeDeliveries: deliveries.filter((d) => !TERMINAL_DELIVERY_STATUSES.includes(d.status)).length,
      unassigned: deliveries.filter((d) => d.status === 'PENDING_ASSIGNMENT').length,
      delivered: orders.filter((o) => o.status === 'DELIVERED').length,
      revenue: orders.filter((o) => o.status === 'DELIVERED').reduce((sum, o) => sum + o.totalAmount, 0),
      availableCouriers: couriers.filter((c) => c.available).length,
      totalCouriers: couriers.length,
    };
  });

  protected readonly recentOrders = computed(() => this.orders().slice(0, 6));

  protected readonly activeDeliveries = computed(() =>
    this.deliveries()
      .filter((d) => !TERMINAL_DELIVERY_STATUSES.includes(d.status))
      .slice(0, 6),
  );

  constructor() {
    pollResources(this.ordersResource, this.deliveriesResource, this.couriersResource);
  }
}
