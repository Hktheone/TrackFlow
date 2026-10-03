create table couriers (
    id                uuid primary key,
    name              varchar(100) not null,
    phone             varchar(30)  not null,
    vehicle_type      varchar(20)  not null,
    available         boolean      not null default true,
    current_latitude  double precision,
    current_longitude double precision,
    last_location_at  timestamptz,
    last_assigned_at  timestamptz,
    version           bigint       not null default 0,
    created_at        timestamptz  not null default now()
);

create table deliveries (
    id               uuid primary key,
    order_id         uuid         not null unique,
    order_number     varchar(20)  not null,
    customer_name    varchar(120) not null,
    customer_phone   varchar(30),
    delivery_address varchar(300) not null,
    status           varchar(30)  not null,
    courier_id       uuid references couriers (id),
    note             varchar(300),
    version          bigint       not null default 0,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    assigned_at      timestamptz,
    picked_up_at     timestamptz,
    delivered_at     timestamptz
);

create index idx_deliveries_status on deliveries (status);
create index idx_deliveries_courier on deliveries (courier_id);

create table delivery_location_updates (
    id          bigserial primary key,
    delivery_id uuid             not null references deliveries (id) on delete cascade,
    courier_id  uuid references couriers (id),
    latitude    double precision not null,
    longitude   double precision not null,
    recorded_at timestamptz      not null
);

create index idx_location_updates_delivery on delivery_location_updates (delivery_id, recorded_at);
