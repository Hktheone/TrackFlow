# TrackFlow

An order and delivery management system built as event-driven microservices, with login and role-based access. Every part runs in its own container: Spring Boot services, a Spring Cloud Gateway that also handles authentication, Eureka discovery, Kafka in KRaft mode, one PostgreSQL database per service, and an Angular frontend served by Nginx.

> **Developers:** see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full technical design: request and event flows, auth internals, state machines, data model, configuration, and how to extend the system safely.

```
                         ┌───────────────────────────────┐
  Browser ──► Nginx ───► │  API Gateway :8080            │ ◄── Eureka :8761 (service discovery)
  (Angular)  :3000       │  routing + login, users,      │
                         │  roles, JWT issuing           │──── auth-db (PG): users, roles, keys
                         └─────┬──────────────────┬──────┘
                    Bearer JWT │ lb://            │ lb://   Bearer JWT
                 ┌─────────────▼──┐            ┌──▼───────────────┐
                 │ order-service  │            │ delivery-service │   both verify the token
                 │  (internal)    │            │   (internal)     │   via the gateway's JWKS
                 └──┬─────────▲───┘            └───▲──────────┬───┘
                    │         │                    │          │
                    │   order-events ──────────────┘          │
                    │         └──────── delivery-status-events │
                    ▼                   (Kafka, KRaft)         ▼
               order-db (PG)                           delivery-db (PG)
```

## Roles

Roles live in the gateway's `roles` table; each user has one.

| Role | Can do |
|---|---|
| **ADMIN** | Everything: every order and delivery, manage riders, manage user accounts and their roles |
| **USER** | Check out orders, confirm or cancel **their own** orders, pick a rider for **their own** deliveries, track them live |
| **RIDER** | See only deliveries **assigned to them**, move them through *picked up → in transit → delivered/failed*, share live location while delivering |

Someone else's order or delivery is reported as **404 Not Found**, not 403, so ids can't be probed. A rider has no access to the order APIs at all.

### Demo accounts

Created on first start (password `trackflow123`, configurable with `DEMO_PASSWORD`):

| Username | Role |
|---|---|
| `admin` | ADMIN |
| `grace`, `alan` | USER |
| `amara`, `bilal`, `chen`, `dana` | RIDER (linked to the seeded rider profiles) |

Anyone can also sign up; self-registration always creates a USER.

## How authentication works

1. **Login**: `POST /api/auth/login` checks the BCrypt password hash in `auth-db` and returns a signed **RS256 JWT** (2 h) with the username (`sub`) and role (`roles`).
2. **Every request** carries `Authorization: Bearer <token>`. The gateway rejects missing, invalid, expired or revoked tokens (401) and applies coarse role rules (403), e.g. riders can't touch `/api/orders`.
3. **The services verify the token again** using the gateway's public key from `/.well-known/jwks.json` (defence in depth; their ports aren't even published). They enforce ownership: *is this your order? are you the rider on this delivery?*
4. **Logout**: `POST /api/auth/logout` records the token's id in `revoked_tokens`; the gateway refuses it until it would have expired anyway.

The RSA signing key is generated once and stored in `auth-db`, so tokens survive gateway restarts and every gateway replica signs with the same key.

## The event flow

1. **Checkout**: a USER calls `POST /api/orders` → order-service saves it as `PENDING` (owned by that user) and publishes `ORDER_CREATED`.
2. **Confirm**: `POST /api/orders/{id}/confirm` → `CONFIRMED`, publishes `ORDER_CONFIRMED` to `order-events`.
3. **Delivery created**: delivery-service consumes `ORDER_CONFIRMED` and creates a delivery from the event (no call back to order-service). It waits in `PENDING_ASSIGNMENT`.
4. **Assign a rider**: the customer (or an admin) picks a free rider → `ASSIGNED`. Set `TRACKFLOW_DELIVERY_AUTO_ASSIGN=true` to have the longest-idle rider assigned automatically instead.
5. **Ride**: the assigned rider marks it picked up, shares their location (real browser GPS, or *Simulate drive* for demos), and marks it delivered.
6. **Status flows back**: every delivery change is published to `delivery-status-events`. order-service consumes these and moves the order through `COURIER_ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED`, recording each step in its status history.
7. **Cancellation**: cancelling an order before pickup publishes `ORDER_CANCELLED`; delivery-service cancels the delivery and frees the rider.

### Reliability choices

| Concern | How it's handled |
|---|---|
| Publishing an event for a change that rolled back | Events are raised inside the transaction but sent to Kafka from a `@TransactionalEventListener(AFTER_COMMIT)` |
| Per-order ordering | Both topics are keyed by `orderId`, so all events for one order share a partition |
| Duplicate / redelivered events | Consumers are idempotent: one delivery per `orderId` (unique constraint + check), and order status only moves forward through an explicit state machine, so stale events are skipped |
| Poison messages | `DefaultErrorHandler` retries 3× then routes to a dead-letter topic; malformed JSON goes straight to the DLT |
| Concurrent updates | `@Version` optimistic locking on orders, deliveries and riders |
| Service coupling | Each service owns its database and its own copy of the event contract; no shared library, no synchronous service-to-service calls |
| A service restarting | The gateway answers 503 instead of hanging; the UI keeps showing the last good data and catches up on the next poll |

A crash in the narrow window between DB commit and Kafka send would lose that event. A transactional outbox (e.g. with Debezium) would close that gap and is the natural next step.

## Tech stack

- **Java 21**, **Spring Boot 4.0**, **Spring Cloud 2025.1** (Gateway Server MVC, Netflix Eureka)
- **Spring Security 7**: OAuth2 resource server, Nimbus JOSE for RS256 JWTs, BCrypt passwords, method security
- **Spring for Apache Kafka 4**, **Apache Kafka 4.1** in KRaft mode (no ZooKeeper)
- **PostgreSQL 17**, Spring Data JPA (Hibernate 7), **Flyway** migrations
- **Angular 22**: standalone components, signals, `httpResource`, OnPush, new control flow, functional guards and interceptors; **Leaflet** for live tracking
- **Docker Compose** with health-check-ordered startup and multi-stage image builds

## Run it

Requires Docker Desktop. Nothing else needs to be installed; every image builds from source.

```bash
docker compose up -d --build
```

The first build downloads Maven and npm dependencies, so give it a few minutes. Then open:

| What | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API gateway | http://localhost:8080 (everything needs a token except login/register) |
| Eureka dashboard | http://localhost:8761 |
| Kafka UI (optional) | `docker compose --profile tools up -d kafka-ui` → http://localhost:8090 |

**Try it:** log in as `grace` and check out an order (*Fill sample data* helps), confirm it, open its delivery and assign Amara. Log out, log in as `amara`, open the job, mark it picked up and press *Share my location* (or *Simulate drive*), then mark it delivered. Log back in as `grace`: the order timeline shows each step arriving via Kafka. Log in as `admin` to see everything and manage users.

Stop with `docker compose down` (add `-v` to wipe the databases).

### Running services from an IDE

Start the infrastructure in Docker, then run any service locally. The defaults in each `application.yml` point at `localhost`: Postgres on 15433 (orders) / 15434 (deliveries) / 15435 (auth), kept clear of the 5433–5532 range Windows/Hyper-V often reserves; Kafka on 9094; Eureka on 8761; JWKS from the gateway on 8080.

```bash
docker compose up -d kafka order-db delivery-db auth-db eureka-server
cd backend/api-gateway && ./mvnw spring-boot:run
cd backend/order-service && ./mvnw spring-boot:run
cd frontend && npm start        # dev server on :4200, proxies /api to the gateway on :8080
```

## API

All routes go through the gateway on `:8080`. Roles in brackets; "own" means the service also checks ownership.

| Method | Path | Who | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | anyone | Log in → access token |
| `POST` | `/api/auth/register` | anyone | Sign up as a USER → access token |
| `POST` | `/api/auth/logout` | logged in | Revoke the current token |
| `GET` | `/api/auth/me` | logged in | Current user |
| `GET` / `POST` | `/api/auth/users` | ADMIN | List / create accounts with any role |
| `PATCH` | `/api/auth/users/{id}` | ADMIN | Change role, enable/disable |
| `GET` | `/.well-known/jwks.json` | anyone | Public key for verifying tokens |
| `POST` | `/api/orders` | USER, ADMIN | Check out an order |
| `GET` | `/api/orders?status=` | USER (own), ADMIN | List orders |
| `GET` | `/api/orders/{id}` · `/history` | USER (own), ADMIN | Order and its status history |
| `POST` | `/api/orders/{id}/confirm` · `/cancel` | USER (own), ADMIN | Confirm → `ORDER_CONFIRMED`, cancel → `ORDER_CANCELLED` |
| `GET` | `/api/deliveries?status=` | USER (own orders), RIDER (assigned), ADMIN | List deliveries |
| `GET` | `/api/deliveries/{id}` · `/order/{orderId}` · `/{id}/track` | same | Delivery, by order, GPS trail |
| `POST` | `/api/deliveries/{id}/assign` | USER (own), ADMIN | Assign or reassign a rider (before pickup) |
| `PATCH` | `/api/deliveries/{id}/status` | RIDER (assigned), ADMIN | `PICKED_UP`, `IN_TRANSIT`, `DELIVERED` or `FAILED` |
| `POST` | `/api/deliveries/{id}/location` | RIDER (assigned), ADMIN | Record a GPS ping |
| `GET` | `/api/couriers` | USER, ADMIN | List riders |
| `POST` | `/api/couriers` | ADMIN | Add a rider profile (linked to a RIDER login by username) |

Errors are returned as RFC 9457 `application/problem+json`.

## Project layout

```
TrackFlow/
├── docker-compose.yml
├── backend/
│   ├── eureka-server/      service registry
│   ├── api-gateway/        Spring Cloud Gateway (MVC): routing via lb://, plus login, users, roles, JWT issuing
│   ├── order-service/      orders, status history; produces order-events, consumes delivery-status-events
│   └── delivery-service/   deliveries, riders, GPS trail; consumes order-events, produces delivery-status-events
└── frontend/               Angular app + Nginx config
```

Each backend service is an independent Maven project with its own wrapper and Dockerfile.

## Tests

```bash
cd backend/api-gateway && ./mvnw test
cd backend/order-service && ./mvnw test
cd backend/delivery-service && ./mvnw test
```

The unit tests cover token issuing and validation (signature, issuer, revocation), the order and delivery state machines, rider assignment, and the per-role delivery access rules.
