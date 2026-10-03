-- A starting fleet so confirmed orders get auto-assigned out of the box.
insert into couriers (id, name, phone, vehicle_type, current_latitude, current_longitude, last_location_at)
values (gen_random_uuid(), 'Amara Okafor',   '+44 7700 900101', 'SCOOTER', 51.5079, -0.0877, now()),
       (gen_random_uuid(), 'Bilal Hussain',  '+44 7700 900102', 'CAR',     51.5155, -0.1419, now()),
       (gen_random_uuid(), 'Chen Wei',       '+44 7700 900103', 'BICYCLE', 51.5033, -0.1196, now()),
       (gen_random_uuid(), 'Dana Kowalski',  '+44 7700 900104', 'VAN',     51.5226, -0.1050, now());
