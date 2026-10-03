import { DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

/** How often screens refresh, so Kafka-driven status changes appear without a manual reload. */
export const POLL_INTERVAL_MS = 3000;

interface Reloadable {
  reload(): boolean;
}

/**
 * Reloads the given resources every few seconds for as long as the calling component lives.
 * Must be called in an injection context (a constructor or field initializer).
 */
export function pollResources(...resources: Reloadable[]): void {
  const destroyRef = inject(DestroyRef);
  interval(POLL_INTERVAL_MS)
    .pipe(takeUntilDestroyed(destroyRef))
    .subscribe(() => resources.forEach((r) => r.reload()));
}
