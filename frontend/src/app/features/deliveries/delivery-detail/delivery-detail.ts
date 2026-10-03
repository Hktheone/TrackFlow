import { DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Observable, Subscription, interval } from 'rxjs';

import { API, TrackFlowApi } from '../../../core/api';
import { AuthService } from '../../../core/auth';
import { VEHICLE_ICONS, humanize } from '../../../core/labels';
import { lastGood } from '../../../core/last-good';
import { Courier, Delivery, DeliveryStatus, LocationPoint } from '../../../core/models';
import { Notifications } from '../../../core/notifications';
import { pollResources } from '../../../core/polling';
import { MapPosition, RouteMap } from '../../../shared/route-map/route-map';
import { StatusBadge } from '../../../shared/status-badge/status-badge';

interface StatusAction {
  status: DeliveryStatus;
  label: string;
  danger?: boolean;
}

const NEXT_ACTIONS: Partial<Record<DeliveryStatus, StatusAction[]>> = {
  ASSIGNED: [
    { status: 'PICKED_UP', label: 'Mark picked up' },
    { status: 'FAILED', label: 'Mark failed', danger: true },
  ],
  PICKED_UP: [
    { status: 'IN_TRANSIT', label: 'Start transit' },
    { status: 'DELIVERED', label: 'Mark delivered' },
    { status: 'FAILED', label: 'Mark failed', danger: true },
  ],
  IN_TRANSIT: [
    { status: 'DELIVERED', label: 'Mark delivered' },
    { status: 'FAILED', label: 'Mark failed', danger: true },
  ],
};

const TRACKABLE: readonly DeliveryStatus[] = ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT'];
const FALLBACK_POSITION: MapPosition = { latitude: 51.5074, longitude: -0.1278 };
/** Roughly 150 m per simulated GPS ping. */
const STEP_DEGREES = 0.0015;
const AUTO_DRIVE_INTERVAL_MS = 2000;
/** Real GPS can fire many times a second; send at most one ping per this interval. */
const LIVE_PING_MIN_INTERVAL_MS = 5000;

@Component({
  selector: 'app-delivery-detail',
  imports: [RouterLink, StatusBadge, RouteMap, DatePipe, DecimalPipe],
  templateUrl: './delivery-detail.html',
  styleUrl: './delivery-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryDetail {
  private readonly api = inject(TrackFlowApi);
  private readonly notifications = inject(Notifications);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  /** Bound from the :id route parameter. */
  readonly id = input.required<string>();

  protected readonly deliveryResource = httpResource<Delivery>(() => `${API.deliveries}/${this.id()}`);
  protected readonly delivery = lastGood<Delivery | undefined>(this.deliveryResource, undefined);
  private readonly trailResource = httpResource<LocationPoint[]>(() => `${API.deliveries}/${this.id()}/track`, {
    defaultValue: [],
  });
  protected readonly trail = lastGood<LocationPoint[]>(this.trailResource, []);
  /** Only customers and admins pick riders (riders aren't allowed to list them). */
  private readonly couriersResource = httpResource<Courier[]>(() => (this.canAssign() ? API.couriers : undefined), {
    defaultValue: [],
  });
  protected readonly couriers = lastGood<Courier[]>(this.couriersResource, []);

  protected readonly busy = signal(false);
  protected readonly note = signal('');
  protected readonly selectedCourier = signal('');
  protected readonly autoDriving = signal(false);
  protected readonly sharingLocation = signal(false);
  protected readonly vehicleIcons = VEHICLE_ICONS;
  protected readonly humanize = humanize;

  /** The customer who placed the order (or an admin) chooses the rider, until pickup. */
  protected readonly canAssign = computed(() => {
    const d = this.delivery();
    if (!d || (d.status !== 'PENDING_ASSIGNMENT' && d.status !== 'ASSIGNED')) {
      return false;
    }
    return this.auth.isAdmin() || (this.auth.hasRole('USER') && d.customerUsername === this.auth.user()?.username);
  });

  /** The rider carrying the delivery (or an admin) moves it forward and reports location. */
  protected readonly canDrive = computed(() => {
    const d = this.delivery();
    if (!d) {
      return false;
    }
    return this.auth.isAdmin() || (this.auth.hasRole('RIDER') && d.courier?.username === this.auth.user()?.username);
  });

  protected readonly isMyJob = computed(() => this.auth.hasRole('RIDER') && this.canDrive());

  protected readonly actions = computed(() => {
    const status = this.delivery()?.status;
    return status && this.canDrive() ? (NEXT_ACTIONS[status] ?? []) : [];
  });

  protected readonly canTrack = computed(() => {
    const status = this.delivery()?.status;
    return this.canDrive() && !!status && TRACKABLE.includes(status);
  });

  protected readonly availableCouriers = computed(() => this.couriers().filter((c) => c.available));

  protected readonly courierPosition = computed<MapPosition | null>(() => {
    const courier = this.delivery()?.courier;
    return courier?.latitude != null && courier.longitude != null
      ? { latitude: courier.latitude, longitude: courier.longitude }
      : null;
  });

  protected readonly geolocationSupported = typeof navigator !== 'undefined' && 'geolocation' in navigator;

  private heading = Math.random() * 2 * Math.PI;
  private autoDrive: Subscription | null = null;
  private watchId: number | null = null;
  private lastLivePing = 0;

  constructor() {
    pollResources(this.deliveryResource, this.trailResource, this.couriersResource);

    // Stop sending locations once the delivery is finished (or this user can no longer drive it).
    effect(() => {
      if (!this.canTrack()) {
        if (this.autoDriving()) {
          this.stopAutoDrive();
        }
        if (this.sharingLocation()) {
          this.stopSharing();
        }
      }
    });
    this.destroyRef.onDestroy(() => this.stopSharing());
  }

  protected assign(): void {
    const courierId = this.selectedCourier();
    if (!courierId) {
      return;
    }
    this.run(this.api.assignCourier(this.id(), courierId), (d) => `Assigned to ${d.courier?.name ?? 'rider'}`);
    this.selectedCourier.set('');
  }

  protected setStatus(action: StatusAction): void {
    const note = this.note().trim() || null;
    this.run(
      this.api.updateDeliveryStatus(this.id(), action.status, note),
      (d) => `${humanize(d.status)} → published to delivery-status-events`,
    );
    this.note.set('');
  }

  /** Real tracking: streams the device's GPS position while the rider is on the job. */
  protected toggleSharing(): void {
    if (this.sharingLocation()) {
      this.stopSharing();
      return;
    }
    if (!this.geolocationSupported) {
      this.notifications.error('This browser cannot share location.');
      return;
    }
    this.stopAutoDrive();
    this.sharingLocation.set(true);
    this.watchId = navigator.geolocation.watchPosition(
      (position) => {
        const now = Date.now();
        if (now - this.lastLivePing >= LIVE_PING_MIN_INTERVAL_MS) {
          this.lastLivePing = now;
          this.send(position.coords.latitude, position.coords.longitude);
        }
      },
      (error) => {
        this.notifications.error(
          error.code === error.PERMISSION_DENIED
            ? 'Location permission was denied. Allow it in the browser to share your position.'
            : 'Could not read your location.',
        );
        this.stopSharing();
      },
      { enableHighAccuracy: true, maximumAge: 5000 },
    );
  }

  /** Demo stand-in for a moving phone: one simulated GPS reading a short hop from the last one. */
  protected ping(): void {
    const from = this.lastPosition();
    this.heading += (Math.random() - 0.5) * 0.8;
    this.send(
      from.latitude + Math.cos(this.heading) * STEP_DEGREES,
      from.longitude + Math.sin(this.heading) * STEP_DEGREES * 1.6,
    );
  }

  protected toggleAutoDrive(): void {
    if (this.autoDrive) {
      this.stopAutoDrive();
      return;
    }
    this.stopSharing();
    this.autoDriving.set(true);
    this.ping();
    this.autoDrive = interval(AUTO_DRIVE_INTERVAL_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.ping());
  }

  protected onNoteInput(event: Event): void {
    this.note.set((event.target as HTMLInputElement).value);
  }

  protected onCourierChange(event: Event): void {
    this.selectedCourier.set((event.target as HTMLSelectElement).value);
  }

  private send(latitude: number, longitude: number): void {
    this.api
      .recordLocation(this.id(), latitude, longitude)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.trailResource.reload();
          this.deliveryResource.reload();
        },
        error: () => {
          this.stopAutoDrive();
          this.stopSharing();
        },
      });
  }

  private stopAutoDrive(): void {
    this.autoDrive?.unsubscribe();
    this.autoDrive = null;
    this.autoDriving.set(false);
  }

  private stopSharing(): void {
    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }
    this.sharingLocation.set(false);
  }

  private lastPosition(): MapPosition {
    const last = this.trail().at(-1);
    return last ?? this.courierPosition() ?? FALLBACK_POSITION;
  }

  private run(action: Observable<Delivery>, message: (delivery: Delivery) => string): void {
    this.busy.set(true);
    action.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (delivery) => {
        this.deliveryResource.set(delivery);
        this.couriersResource.reload();
        this.notifications.success(message(delivery));
        this.busy.set(false);
      },
      error: () => this.busy.set(false),
    });
  }
}
