-- Who placed the order (the token's username). Orders created before auth existed have no owner
-- and are visible to admins only.
alter table orders add column customer_username varchar(50);

create index idx_orders_customer_username on orders (customer_username, created_at desc);
