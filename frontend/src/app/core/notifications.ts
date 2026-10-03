import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

const TOAST_LIFETIME_MS = 5000;

@Injectable({ providedIn: 'root' })
export class Notifications {
  private nextId = 0;
  private readonly toastList = signal<Toast[]>([]);

  readonly toasts = this.toastList.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  dismiss(id: number): void {
    this.toastList.update((list) => list.filter((t) => t.id !== id));
  }

  private push(kind: ToastKind, message: string): void {
    const toast: Toast = { id: ++this.nextId, kind, message };
    // Collapse repeats (e.g. the same polling failure every few seconds) into one toast.
    this.toastList.update((list) => [...list.filter((t) => t.message !== message), toast]);
    setTimeout(() => this.dismiss(toast.id), TOAST_LIFETIME_MS);
  }
}
