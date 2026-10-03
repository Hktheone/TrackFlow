create table roles (
    name        varchar(20) primary key,
    description varchar(200) not null
);

insert into roles (name, description)
values ('ADMIN', 'Full access: every order, delivery, rider and user account'),
       ('USER',  'Places and manages their own orders and assigns riders to their deliveries'),
       ('RIDER', 'Carries the deliveries assigned to them and shares live location while delivering');

create table users (
    id            uuid primary key,
    username      varchar(50)  not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(100) not null,
    role          varchar(20)  not null references roles (name),
    enabled       boolean      not null default true,
    created_at    timestamptz  not null
);

create index idx_users_role on users (role);

-- RSA key pair that signs access tokens. Persisted so tokens survive a gateway restart
-- and every gateway replica signs with (and publishes) the same key.
create table signing_keys (
    kid         varchar(64) primary key,
    public_key  text        not null,
    private_key text        not null,
    created_at  timestamptz not null
);

-- Tokens are stateless JWTs; logging out records the token id here until it would have expired anyway.
create table revoked_tokens (
    jti        varchar(64) primary key,
    expires_at timestamptz not null
);

create index idx_revoked_tokens_expires_at on revoked_tokens (expires_at);
