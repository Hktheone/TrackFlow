import { DeliveryStatus, OrderStatus, VehicleType } from './models';

export type StatusTone = 'neutral' | 'info' | 'progress' | 'success' | 'danger';

/** Turns an enum like COURIER_ASSIGNED into "Courier assigned". */
export function humanize(value: string): string {
  const text = value.toLowerCase().replace(/_/g, ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function statusTone(status: OrderStatus | DeliveryStatus): StatusTone {
  switch (status) {
    case 'PENDING':
    case 'PENDING_ASSIGNMENT':
      return 'neutral';
    case 'CONFIRMED':
    case 'COURIER_ASSIGNED':
    case 'ASSIGNED':
      return 'info';
    case 'PICKED_UP':
    case 'IN_TRANSIT':
      return 'progress';
    case 'DELIVERED':
      return 'success';
    case 'DELIVERY_FAILED':
    case 'FAILED':
    case 'CANCELLED':
      return 'danger';
  }
}

export const VEHICLE_ICONS: Record<VehicleType, string> = {
  BICYCLE: '🚲',
  SCOOTER: '🛵',
  CAR: '🚗',
  VAN: '🚐',
};
