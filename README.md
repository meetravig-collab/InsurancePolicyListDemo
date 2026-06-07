# Policy Overview Dashboard

A Spring Boot REST service for retrieving and triaging insurance policies, built
contract-first with a hexagonal (ports & adapters) architecture.

## Endpoints

Base path `/api/v1/policies`:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/policies` | List with pagination, sorting, filtering, and free-text search |
| `GET` | `/api/v1/policies/{id}` | Get a single policy by UUID |
| `PATCH` | `/api/v1/policies/flag` | Bulk flag policies for review |
| `GET` | `/api/v1/policies/summary` | Counts by status, total premium by line of business, expiring-soon count |

The API contract lives in [`openapi.yaml`](openapi.yaml) (the single source of truth);
SpringDoc serves live docs at `/swagger-ui.html` when the app is running.

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL · Gatling · Maven

## Quick start

```bash
# 1. Ensure PostgreSQL is running and the insuranceDB database exists
# 2. Seed sample data (220 records)
psql -U postgres -d insuranceDB -f src/main/resources/data.sql
# 3. Run
mvn spring-boot:run        # http://localhost:8081
```

Full setup, configuration, and API reference: see **[DEVELOPMENT.md](DEVELOPMENT.md)**.

## Documentation

| Document | What's inside |
|---|---|
| **[DESIGN.md](DESIGN.md)** | Architecture (ports & adapters), layering & dependency rule, domain model, SOLID, API contract, key design decisions |
| **[DEVELOPMENT.md](DEVELOPMENT.md)** | Prerequisites, build & run, environment variables, full API reference, logging, project structure |
| **[TESTING.md](TESTING.md)** | Test strategy, the four test layers, how to run them, coverage, and performance results |

## Project layout

```
src/main/java/com/insurance/dashboard/
├── api/              inbound adapter — controller, DTOs, mapper
├── service/          application layer — use cases (depends only on the domain port)
├── domain/           core — POJO model, port, query value objects (no framework deps)
├── infrastructure/   outbound adapter — JPA entity, Spring Data repo, persistence adapter
└── common/           cross-cutting — exception handling
```

Dependencies point strictly inward (`api → service → domain`, `infrastructure → domain`).
See [DESIGN.md](DESIGN.md) for the full diagram and rationale.

## Tooling

- **Postman:** import [`PolicyOverviewDashboard.postman_collection.json`](PolicyOverviewDashboard.postman_collection.json) — a self-contained collection with test assertions (runnable headless via `newman`).
- **Load test:** `mvn gatling:test` (app must be running).
