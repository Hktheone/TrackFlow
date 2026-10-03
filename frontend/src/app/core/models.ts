// Mirrors the JSON contracts of order-service and delivery-service (via the API gateway).

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'COURIER_ASSIGNED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'DELIVERY_FAILED'
  | 'CANCELLED';

export type DeliveryStatus =
  | 'PENDING_ASSIGNMENT'
  | 'ASSIGNED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'FAILED'
  | 'CANCELLED';

export type VehicleType = 'BICYCLE' | 'SCOOTER' | 'CAR' | 'VAN';

export type StatusChangeSource = 'API' | 'DELIVERY_EVENT';

export interface OrderItem {
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  customerUsername: string | null;
  customerName: string;
  customerPhone: string | null;
  deliveryAddress: string;
  items: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrderHistoryEntry {
  status: OrderStatus;
  source: StatusChangeSource;
  note: string | null;
  changedAt: string;
}

export interface CreateOrderItem {
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  customerName: string;
  customerPhone: string | null;
  deliveryAddress: string;
  items: CreateOrderItem[];
}

export interface CourierSummary {
  id: string;
  name: string;
  username: string | null;
  phone: string;
  vehicleType: VehicleType;
  latitude: number | null;
  longitude: number | null;
  lastLocationAt: string | null;
}

export interface Delivery {
  id: string;
  orderId: string;
  orderNumber: string;
  customerUsername: string | null;
  customerName: string;
  customerPhone: string | null;
  deliveryAddress: string;
  status: DeliveryStatus;
  courier: CourierSummary | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
  assignedAt: string | null;
  pickedUpAt: string | null;
  deliveredAt: string | null;
}

export interface Courier {
  id: string;
  name: string;
  username: string | null;
  phone: string;
  vehicleType: VehicleType;
  available: boolean;
  latitude: number | null;
  longitude: number | null;
  lastLocationAt: string | null;
}

export interface CreateCourierRequest {
  name: string;
  phone: string;
  vehicleType: VehicleType;
  username: string;
}

export interface LocationPoint {
  latitude: number;
  longitude: number;
  recordedAt: string;
}

// ---- authentication ----

export type Role = 'ADMIN' | 'USER' | 'RIDER';

export const ROLES: readonly Role[] = ['ADMIN', 'USER', 'RIDER'];

export interface UserAccount {
  id: string;
  username: string;
  displayName: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: UserAccount;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  displayName: string;
  role: Role;
}

/** RFC 9457 problem body returned by the services on errors. */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export const TERMINAL_ORDER_STATUSES: readonly OrderStatus[] = ['DELIVERED', 'DELIVERY_FAILED', 'CANCELLED'];
export const TERMINAL_DELIVERY_STATUSES: readonly DeliveryStatus[] = ['DELIVERED', 'FAILED', 'CANCELLED'];
export const VEHICLE_TYPES: readonly VehicleType[] = ['BICYCLE', 'SCOOTER', 'CAR', 'VAN'];
