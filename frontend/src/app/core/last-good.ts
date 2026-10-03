import { Resource, Signal, computed } from '@angular/core';

/**
 * The resource's value, but holding on to the last successful one while a reload is failing.
 *
 * When a reload fails, a resource either throws from {@code value()} or falls back to its
 * {@code defaultValue}, so a single failed poll (e.g. a 503 while a service restarts or
 * re-registers with Eureka) would blank or break the page until the next success. Screens poll
 * every few seconds, so showing slightly stale data is the better failure mode.
 */
export function lastGood<T>(resource: Resource<T>, fallback: T): Signal<T> {
  let last = fallback;
  return computed(() => {
    const status = resource.status();
    // Only these mean "fresh data". 'reloading' after a failure carries the fallback, not real data,
    // and 'error' / 'loading' / 'idle' have none, so keep showing what we had.
    if (status === 'resolved' || status === 'local') {
      last = resource.value();
    }
    return last;
  });
}
