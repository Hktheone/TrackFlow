import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Courier,
  CreateCourierRequest,
  CreateOrderRequest,
  CreateUserRequest,
  Delivery,
  DeliveryStatus,
  LocationPoint,
  Order,
  Role,
  UserAccount,
} from './models';

/** Set on a request to stop the global error interceptor from toasting its failure (e.g. an expected 404). */
export const SILENT_ERRORS = new HttpContextToken<boolean>(() => false);
export const silentErrors = (): HttpContext => new HttpContext().set(SILENT_ERRORS, true);

// Everything goes through the API gateway; in dev the Angular proxy forwards /api, in Docker Nginx does.
export const API = {
  orders: '/api/orders',
  deliveries: '/api/deliveries',
  couriers: '/api/couriers',
  users: '/api/auth/users',
} as const;

/** Write operations. Reads are done with httpResource in the components so they stay signal-based. */
@Injectable({ providedIn: 'root' })
export class TrackFlowApi {
  private readonly http = inject(HttpClient);

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(API.orders, request);
  }

  confirmOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${API.orders}/${id}/confirm`, null);
  }

  cancelOrder(id: string, reason: string | null): Observable<Order> {
    return this.http.post<Order>(`${API.orders}/${id}/cancel`, { reason });
  }

  assignCourier(deliveryId: string, courierId: string): Observable<Delivery> {
    return this.http.post<Delivery>(`${API.deliveries}/${deliveryId}/assign`, { courierId });
  }

  updateDeliveryStatus(deliveryId: string, status: DeliveryStatus, note: string | null): Observable<Delivery> {
    return this.http.patch<Delivery>(`${API.deliveries}/${deliveryId}/status`, { status, note });
  }

  recordLocation(deliveryId: string, latitude: number, longitude: number): Observable<LocationPoint> {
    return this.http.post<LocationPoint>(`${API.deliveries}/${deliveryId}/location`, { latitude, longitude });
  }

  createCourier(request: CreateCourierRequest): Observable<Courier> {
    return this.http.post<Courier>(API.couriers, request);
  }

  createUser(request: CreateUserRequest): Observable<UserAccount> {
    return this.http.post<UserAccount>(API.users, request);
  }

  updateUser(id: string, changes: { role?: Role; enabled?: boolean }): Observable<UserAccount> {
    return this.http.patch<UserAccount>(`${API.users}/${id}`, changes);
  }
}
