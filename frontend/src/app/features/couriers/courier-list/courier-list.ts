import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, of, switchMap } from 'rxjs';

import { API, TrackFlowApi } from '../../../core/api';
import { VEHICLE_ICONS, humanize } from '../../../core/labels';
import { lastGood } from '../../../core/last-good';
import { Courier, UserAccount, VEHICLE_TYPES, VehicleType } from '../../../core/models';
import { Notifications } from '../../../core/notifications';
import { pollResources } from '../../../core/polling';

@Component({
  selector: 'app-courier-list',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './courier-list.html',
  styleUrl: './courier-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourierList {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(TrackFlowApi);
  private readonly notifications = inject(Notifications);
  private readonly destroyRef = inject(DestroyRef);

  private readonly couriersResource = httpResource<Courier[]>(() => API.couriers, { defaultValue: [] });
  protected readonly couriers = lastGood<Courier[]>(this.couriersResource, []);
  protected readonly availableCount = computed(() => this.couriers().filter((c) => c.available).length);

  protected readonly vehicleTypes = VEHICLE_TYPES;
  protected readonly vehicleIcons = VEHICLE_ICONS;
  protected readonly humanize = humanize;
  protected readonly showForm = signal(false);
  protected readonly saving = signal(false);

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.maxLength(30)]],
    vehicleType: this.fb.control<VehicleType>('SCOOTER', Validators.required),
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._-]{3,50}$/)]],
    // Blank means "link an existing RIDER account"; filled in creates the login too.
    password: ['', Validators.minLength(8)],
  });

  constructor() {
    pollResources(this.couriersResource);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    const username = value.username.trim().toLowerCase();
    const profile = { name: value.name.trim(), phone: value.phone.trim(), vehicleType: value.vehicleType, username };
    // A rider is two records owned by two services: the login (gateway) and the courier profile (delivery-service).
    const account$: Observable<UserAccount | null> = value.password
      ? this.api.createUser({ username, password: value.password, displayName: profile.name, role: 'RIDER' })
      : of(null);
    account$
      .pipe(
        switchMap(() => this.api.createCourier(profile)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (courier) => {
          this.notifications.success(`${courier.name} joined the fleet and can log in as ${courier.username}`);
          this.form.reset();
          this.showForm.set(false);
          this.saving.set(false);
          this.couriersResource.reload();
        },
        error: () => this.saving.set(false),
      });
  }
}
