# Microservices Assessment

Small Spring Boot commerce system with separate order and notification services.

## Modules

- `shared`: order event contracts and AMQP naming.
- `order-module`: order API, order persistence, and outbox publisher.
- `notification-module`: RabbitMQ consumer, notification persistence, and notification API.

Order changes are written to an outbox table in the same transaction as the order update. The outbox publisher sends those events to RabbitMQ, and the notification service consumes them.

## Local Dependencies

- Java 21
- Maven
- Docker
- PostgreSQL, RabbitMQ, and Aspire from `docker-compose.yaml`

Static tenants are hard-coded behind API keys. There is no tenant CRUD or tenant registration flow.

| API key | Tenant ID |
| --- | --- |
| `company-1-key` | `11111111-1111-1111-1111-111111111111` |
| `company-2-key` | `22222222-2222-2222-2222-222222222222` |

## Run

Start infrastructure:

```powershell
docker compose up -d postgres rabbitmq aspire
```

Run services:

```powershell
mvn -pl order-module spring-boot:run
mvn -pl notification-module spring-boot:run
```

Run published images:

```powershell
docker compose -f docker-compose.images.yaml up -d
```

Service URLs:

- Order API: `http://localhost:8080`
- Notification API: `http://localhost:8081`
- RabbitMQ UI: `http://localhost:15672`
- Aspire: `http://localhost:18888`

Manual request examples for orders, notifications, and actuator endpoints are in `resources/api/*.http`.

## Tests And Formatting

```powershell
mvn -pl order-module -am test
mvn spotless:check
mvn spotless:apply
```

Current tests cover the order state machine, basic order service behavior, tenant resolution, tenant isolation through the order API, Micrometer trace helper behavior, and the successful outbox publish path.

Additional coverage worth adding:

- controller validation and HTTP error mappings
- failed outbox publish retry behavior
- notification consumer idempotency

## Observability

The services use Spring Boot Actuator, Micrometer Tracing, and OTLP export. The default local trace endpoint points at Aspire:

```text
http://localhost:18890/v1/traces
```

Override with `OTLP_TRACING_ENDPOINT`.

## Tradeoffs

- Tenant IDs and API keys are static.
- Services own separate databases.
- Notification sending is mocked.
- Outbox publishing starts a new Micrometer span rather than continuing a propagated `traceparent`.
