# Event-Driven Order Platform

Order processing built as three Spring Boot microservices that coordinate through Kafka using a
**choreographed saga**, the **transactional outbox** pattern and **idempotent consumers**.

**Stack:** Java 21, Spring Boot 3.4.1, Spring Kafka, Spring Data JPA, Flyway, PostgreSQL 16, Kafka 3.8 (KRaft), Maven.

## Architecture

```
  POST /orders                                         GET /stock
       │                                                    │
┌──────▼───────┐   orders.events    ┌─────────────────────▼──┐
│ order-service│───────────────────►│    inventory-service   │
│    :8081     │                    │         :8082          │
│  db: orders  │◄───────────────────│    db: inventory       │
└──────▲───────┘  inventory.events  └───────────┬────────────┘
       │          (StockRejected)               │ inventory.events (StockReserved)
       │                                ┌───────▼────────┐
       └────────────────────────────────│ payment-service│
         payments.events                │     :8083      │
   (PaymentCompleted / PaymentFailed)   │ db: payments   │
                                        └────────────────┘
```

| Module | Responsibility |
|---|---|
| `common` | Shared library: event contracts (`Events`), envelope and codec, transactional outbox (`OutboxService`, `OutboxRelay`), `IdempotencyService`, topic definitions |
| `order-service` | REST API; creates orders and settles them as `CONFIRMED`, `REJECTED` or `CANCELLED` |
| `inventory-service` | Reserves stock; releases it when payment fails (compensation) |
| `payment-service` | Mock payment provider; declines amounts above `payment.max-amount` (default 1000) |
| `infra` | `docker-compose.yml` (Postgres + Kafka) and database bootstrap script |

Each service owns its own database; services never share tables.

## Order saga

1. `POST /orders` saves the order and an `OrderCreated` outbox row in **one transaction**.
2. `OutboxRelay` polls the outbox every 500 ms and publishes to Kafka (at-least-once, keyed by order id).
3. Inventory reserves stock and emits `StockReserved`, or `StockRejected` (unknown SKU / insufficient stock) and the order becomes `REJECTED`.
4. Payment charges the order and emits `PaymentCompleted` (order `CONFIRMED`) or `PaymentFailed`.
5. On `PaymentFailed` the order becomes `CANCELLED` and inventory releases the reservation.

| Topic | Producer | Consumers |
|---|---|---|
| `orders.events` | order-service | inventory-service |
| `inventory.events` | inventory-service | order-service, payment-service |
| `payments.events` | payment-service | order-service, inventory-service |

Topics are created on startup with 3 partitions and replication factor 1.

## Reliability

- **Transactional outbox**: the business change and its event commit atomically, so events are never lost if Kafka is down.
- **Idempotent consumers**: each event id is recorded in `processed_message` in the same transaction as its effects, so redelivery is harmless.
- **Ordering**: events are keyed by order id, so all events of one order land on the same partition.

## Running locally

Prerequisites: JDK 21, Maven, Docker.

```bash
# 1. Start Postgres (host port 5433) and Kafka (port 9092)
docker compose -f infra/docker-compose.yml up -d

# 2. Build everything and run the tests
mvn clean verify

# 3. Start the services (each in its own terminal)
mvn -pl order-service spring-boot:run       # :8081
mvn -pl inventory-service spring-boot:run   # :8082
mvn -pl payment-service spring-boot:run     # :8083
```

## Try it

Seeded stock: `LAPTOP` (10), `PHONE` (50), `HEADSET` (100).

```bash
# Happy path -> CONFIRMED
curl -X POST localhost:8081/orders -H 'Content-Type: application/json' \
  -d '{"sku":"PHONE","quantity":2,"amount":200.00}'

# Check the result (use the returned id)
curl localhost:8081/orders/<id>

# Payment declined (amount > 1000) -> CANCELLED, stock released
curl -X POST localhost:8081/orders -H 'Content-Type: application/json' \
  -d '{"sku":"LAPTOP","quantity":1,"amount":5000.00}'

# Unknown SKU or too much quantity -> REJECTED
curl -X POST localhost:8081/orders -H 'Content-Type: application/json' \
  -d '{"sku":"NOPE","quantity":1,"amount":10.00}'

# Current stock levels
curl localhost:8082/stock
```

`POST /orders` returns `202 Accepted`: the order is settled asynchronously by the saga.

## Configuration

| Property | Default | Where |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | each service's `application.yml` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/<db>` | each service's `application.yml` |
| `outbox.poll-interval-ms` | `500` | any service |
| `payment.max-amount` | `1000` | payment-service |

Actuator endpoints `health`, `info` and `prometheus` are exposed on every service.

## Limitations

This is a demo, not a production deployment: single Kafka broker with replication factor 1, no
authentication, no dead-letter topic or custom retry policy, no cleanup of published outbox rows,
and no Dockerfiles for the services themselves.
