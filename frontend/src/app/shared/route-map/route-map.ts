import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';

import { LocationPoint } from '../../core/models';

export interface MapPosition {
  latitude: number;
  longitude: number;
}

/** Where to centre the map before any GPS data exists (the seeded couriers are around here). */
const DEFAULT_CENTER: L.LatLngTuple = [51.5074, -0.1278];

/** Draws a delivery's GPS trail and the courier's latest position on an OpenStreetMap map. */
@Component({
  selector: 'app-route-map',
  templateUrl: './route-map.html',
  styleUrl: './route-map.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteMap {
  readonly trail = input<LocationPoint[]>([]);
  readonly courier = input<MapPosition | null>(null);

  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('map');
  private readonly map = signal<L.Map | null>(null);
  private readonly layers = L.layerGroup();
  private fittedPoints = -1;

  constructor() {
    afterNextRender(() => {
      const map = L.map(this.container().nativeElement, { zoomControl: true }).setView(DEFAULT_CENTER, 13);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors',
      }).addTo(map);
      this.layers.addTo(map);
      this.map.set(map);
    });

    inject(DestroyRef).onDestroy(() => this.map()?.remove());

    effect(() => {
      const map = this.map();
      if (map) {
        this.draw(map, this.trail(), this.courier());
      }
    });
  }

  private draw(map: L.Map, trail: LocationPoint[], courier: MapPosition | null): void {
    this.layers.clearLayers();
    const points: L.LatLngTuple[] = trail.map((p) => [p.latitude, p.longitude]);

    if (points.length > 0) {
      L.polyline(points, { color: '#4f46e5', weight: 4, opacity: 0.8 }).addTo(this.layers);
      L.circleMarker(points[0], { radius: 6, color: '#127a4a', fillOpacity: 1 })
        .bindTooltip('First ping')
        .addTo(this.layers);
    }

    const head: L.LatLngTuple | null = courier
      ? [courier.latitude, courier.longitude]
      : (points.at(-1) ?? null);
    if (head) {
      L.circleMarker(head, { radius: 9, color: '#fff', weight: 3, fillColor: '#4f46e5', fillOpacity: 1 })
        .bindTooltip('Courier')
        .addTo(this.layers);
    }

    // Re-fit only when new points arrive, so the user's own panning/zooming isn't undone on every poll.
    if (points.length !== this.fittedPoints) {
      this.fittedPoints = points.length;
      const all = head ? [...points, head] : points;
      if (all.length > 1) {
        map.fitBounds(L.latLngBounds(all), { padding: [32, 32], maxZoom: 16 });
      } else if (all.length === 1) {
        map.setView(all[0], 15);
      }
    }
  }
}
