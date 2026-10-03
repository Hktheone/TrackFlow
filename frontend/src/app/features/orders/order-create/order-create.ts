import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { TrackFlowApi } from '../../../core/api';
import { AuthService } from '../../../core/auth';
import { CreateOrderRequest } from '../../../core/models';
import { Notifications } from '../../../core/notifications';

type ItemForm = FormGroup<{
  productName: FormControl<string>;
  quantity: FormControl<number>;
  unitPrice: FormControl<number>;
}>;

@Component({
  selector: 'app-order-create',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './order-create.html',
  styleUrl: './order-create.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderCreate {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(TrackFlowApi);
  private readonly router = inject(Router);
  private readonly notifications = inject(Notifications);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthService);

  protected readonly form = this.fb.group({
    customerName: [this.auth.user()?.displayName ?? '', [Validators.required, Validators.maxLength(120)]],
    customerPhone: ['', Validators.maxLength(30)],
    deliveryAddress: ['', [Validators.required, Validators.maxLength(300)]],
    items: this.fb.array<ItemForm>([this.newItem()]),
  });

  protected readonly submitting = signal(false);

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly total = computed(() =>
    (this.formValue().items ?? []).reduce(
      (sum, item) => sum + (Number(item?.quantity) || 0) * (Number(item?.unitPrice) || 0),
      0,
    ),
  );

  protected get items(): FormArray<ItemForm> {
    return this.form.controls.items;
  }

  protected addItem(): void {
    this.items.push(this.newItem());
  }

  protected removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  protected fillSample(): void {
    this.items.clear();
    this.items.push(this.newItem('Wireless headphones', 1, 79.99));
    this.items.push(this.newItem('USB-C cable', 2, 9.5));
    this.form.patchValue({
      customerName: 'Grace Hopper',
      customerPhone: '+44 7700 900200',
      deliveryAddress: '221B Baker Street, London NW1 6XE',
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: CreateOrderRequest = {
      customerName: value.customerName.trim(),
      customerPhone: value.customerPhone.trim() || null,
      deliveryAddress: value.deliveryAddress.trim(),
      items: value.items.map((i) => ({
        productName: i.productName.trim(),
        quantity: Number(i.quantity),
        unitPrice: Number(i.unitPrice),
      })),
    };

    this.submitting.set(true);
    this.api
      .createOrder(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.notifications.success(`Order ${order.orderNumber} created`);
          void this.router.navigate(['/orders', order.id]);
        },
        error: () => this.submitting.set(false),
      });
  }

  private newItem(productName = '', quantity = 1, unitPrice = 0): ItemForm {
    return this.fb.group({
      productName: [productName, [Validators.required, Validators.maxLength(120)]],
      quantity: [quantity, [Validators.required, Validators.min(1), Validators.max(1000)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0.01)]],
    });
  }
}
