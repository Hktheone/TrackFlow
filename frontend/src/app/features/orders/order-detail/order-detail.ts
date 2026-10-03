import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { RouterLink } from '@angular/router';

import { API, TrackFlowApi, silentErrors } from '../../../core/api';
import { VEHICLE_ICONS, humanize } from '../../../core/labels';
import { lastGood } from '../../../core/last-good';
import { Delivery, Order, OrderHistoryEntry, OrderStatus } from '../../../core/models';
import { Notifications } from '../../../core/notifications';
import { pollResources } from '../../../core/polling';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

/** The happy path, used to draw the progress stepper. */
const LIFECYCLE: readonly OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'COURIER_ASSIGNED',
  'PICKED_UP',
  'IN_TRANSIT',
  'DELIVERED',
];

const CANCELLABLE: readonly OrderStatus[] = ['PENDING', 'CONFIRMED', 'COURIER_ASSIGNED'];

type StepState = 'done' | 'current' | 'upcoming';

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink, StatusBadge, CurrencyPipe, DatePipe],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderDetail {
  private readonly api = inject(TrackFlowApi);
  private readonly notifications = inject(Notifications);
  private readonly destroyRef = inject(DestroyRef);

  /** Bound from the :id route parameter. */
  readonly id = input.required<string>();

  protected readonly orderResource = httpResource<Order>(() => `${API.orders}/${this.id()}`);
  protected readonly order = lastGood<Order | undefined>(this.orderResource, undefined);
  private readonly historyResource = httpResource<OrderHistoryEntry[]>(() => `${API.orders}/${this.id()}/history`, {
    defaultValue: [],
  });
  protected readonly history = lastGood<OrderHistoryEntry[]>(this.historyResource, []);
  // A delivery only exists once ORDER_CONFIRMED has been consumed, so skip the lookup for pending
  // orders and don't toast the 404 that's expected in the moment between confirming and consuming.
  private readonly deliveryResource = httpResource<Delivery>(() => {
    const status = this.order()?.status;
    return status && status !== 'PENDING'
      ? { url: `${API.deliveries}/order/${this.id()}`, context: silentErrors() }
      : undefined;
  });
  protected readonly delivery = lastGood<Delivery | undefined>(this.deliveryResource, undefined);

  protected readonly busy = signal(false);
  protected readonly cancelReason = signal('');
  protected readonly vehicleIcons = VEHICLE_ICONS;

  protected readonly canConfirm = computed(() => this.order()?.status === 'PENDING');
  protected readonly canCancel = computed(() => {
    const status = this.order()?.status;
    return !!status && CANCELLABLE.includes(status);
  });

  protected readonly steps = computed(() => {
    const status = this.order()?.status;
    const current = status ? LIFECYCLE.indexOf(status) : -1;
    // Cancelled / failed orders are off the happy path: show how far they got before stopping.
    const stopped = !!status && current < 0;
    const reached = stopped ? this.furthestFromHistory() : current;
    return LIFECYCLE.map((step, i) => {
      const finished = i < reached || (i === reached && (stopped || step === 'DELIVERED'));
      const state: StepState = finished ? 'done' : i === reached ? 'current' : 'upcoming';
      return { status: step, label: humanize(step), state };
    });
  });

  protected readonly stoppedReason = computed(() => {
    const status = this.order()?.status;
    return status === 'CANCELLED' || status === 'DELIVERY_FAILED' ? status : null;
  });

  constructor() {
    pollResources(this.orderResource, this.historyResource, this.deliveryResource);
  }

  protected confirm(): void {
    this.run(this.api.confirmOrder(this.id()), (o) => `Order ${o.orderNumber} confirmed → ORDER_CONFIRMED published`);
  }

  protected cancel(): void {
    const reason = this.cancelReason().trim() || null;
    this.run(this.api.cancelOrder(this.id(), reason), (o) => `Order ${o.orderNumber} cancelled`);
  }

  protected onReasonInput(event: Event): void {
    this.cancelReason.set((event.target as HTMLInputElement).value);
  }

  protected sourceLabel(entry: OrderHistoryEntry): string {
    return entry.source === 'DELIVERY_EVENT' ? 'Kafka · delivery-status-events' : 'REST API';
  }

  private run(action: Observable<Order>, message: (order: Order) => string): void {
    this.busy.set(true);
    action.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (order) => {
        this.orderResource.set(order);
        this.historyResource.reload();
        this.notifications.success(message(order));
        this.busy.set(false);
      },
      error: () => this.busy.set(false),
    });
  }

  private furthestFromHistory(): number {
    return this.history()
      .reduce((max, entry) => Math.max(max, LIFECYCLE.indexOf(entry.status)), -1);
  }
}
