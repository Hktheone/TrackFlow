-- Links a courier profile to the RIDER login that carries it (the token's username).
alter table couriers add column username varchar(50);
alter table couriers add constraint uq_couriers_username unique (username);

-- The seeded fleet matches the demo rider accounts the gateway creates.
update couriers set username = 'amara' where name = 'Amara Okafor';
update couriers set username = 'bilal' where name = 'Bilal Hussain';
update couriers set username = 'chen'  where name = 'Chen Wei';
update couriers set username = 'dana'  where name = 'Dana Kowalski';

-- Who placed the order, copied from the ORDER_CONFIRMED event, so that customer can see and
-- manage their delivery. Deliveries created before auth existed have no owner (admin-only).
alter table deliveries add column customer_username varchar(50);

create index idx_deliveries_customer_username on deliveries (customer_username);
