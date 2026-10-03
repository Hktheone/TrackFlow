import { inject } from '@angular/core';
import { Router, Routes } from '@angular/router';

import { AuthService } from './core/auth';
import { authGuard, guestGuard, roleGuard } from './core/auth.guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    // Each role starts somewhere different: riders straight at their deliveries.
    redirectTo: () => inject(Router).parseUrl(inject(AuthService).homeUrl()),
  },
  {
    path: 'login',
    title: 'Log in · TrackFlow',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    title: 'Sign up · TrackFlow',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'dashboard',
    title: 'Dashboard · TrackFlow',
    canActivate: [roleGuard('ADMIN', 'USER')],
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'orders',
    title: 'Orders · TrackFlow',
    canActivate: [roleGuard('ADMIN', 'USER')],
    loadComponent: () => import('./features/orders/order-list/order-list').then((m) => m.OrderList),
  },
  {
    path: 'orders/new',
    title: 'Checkout · TrackFlow',
    canActivate: [roleGuard('ADMIN', 'USER')],
    loadComponent: () => import('./features/orders/order-create/order-create').then((m) => m.OrderCreate),
  },
  {
    path: 'orders/:id',
    title: 'Order · TrackFlow',
    canActivate: [roleGuard('ADMIN', 'USER')],
    loadComponent: () => import('./features/orders/order-detail/order-detail').then((m) => m.OrderDetail),
  },
  {
    path: 'deliveries',
    title: 'Deliveries · TrackFlow',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/deliveries/delivery-list/delivery-list').then((m) => m.DeliveryList),
  },
  {
    path: 'deliveries/:id',
    title: 'Delivery · TrackFlow',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/deliveries/delivery-detail/delivery-detail').then((m) => m.DeliveryDetail),
  },
  {
    path: 'riders',
    title: 'Riders · TrackFlow',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/couriers/courier-list/courier-list').then((m) => m.CourierList),
  },
  {
    path: 'users',
    title: 'Users · TrackFlow',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/users/user-list/user-list').then((m) => m.UserList),
  },
  { path: 'couriers', redirectTo: 'riders' },
  { path: '**', redirectTo: '' },
];
