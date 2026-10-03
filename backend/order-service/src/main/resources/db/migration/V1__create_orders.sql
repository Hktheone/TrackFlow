create table orders (
    id               uuid primary key,
    order_number     varchar(20)    not null unique,
    customer_name    varchar(120)   not null,
    customer_phone   varchar(30),
    delivery_address varchar(300)   not null,
    total_amount     numeric(12, 2) not null,
    status           varchar(30)    not null,
    version          bigint         not null default 0,
    created_at       timestamptz    not null,
    updated_at       timestamptz    not null
);

create index idx_orders_status on orders (status);
create index idx_orders_created_at on orders (created_at desc);

create table order_items (
    order_id     uuid           not null references orders (id) on delete cascade,
    line_no      integer        not null,
    product_name varchar(120)   not null,
    quantity     integer        not null check (quantity > 0),
    unit_price   numeric(12, 2) not null check (unit_price > 0),
    primary key (order_id, line_no)
);

create table order_status_history (
    id         bigserial primary key,
    order_id   uuid        not null references orders (id) on delete cascade,
    status     varchar(30) not null,
    source     varchar(20) not null,
    note       varchar(300),
    changed_at timestamptz not null
);

create index idx_order_status_history_order on order_status_history (order_id, changed_at);
