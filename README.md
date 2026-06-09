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

The API contract lives in [`openapi.yaml`](src/main/resources/static/openapi.yaml) (the
single source of truth); SpringDoc serves it via Swagger UI at `/swagger-ui/index.html`
when the app is running.

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL · Caffeine cache · SpringDoc/OpenAPI · Docker · Gatling · Maven

## Quick start

```bash
# 1. Ensure PostgreSQL is running and the insuranceDB database exists
# 2. Run — Flyway creates the schema and seeds 220 sample policies on startup
mvn spring-boot:run        # http://localhost:8081
```

The schema and sample data are managed by **Flyway** (`src/main/resources/db/migration`),
applied automatically on startup — no manual seeding step.

Full setup, configuration, and API reference: see **[docs/ProjectOverview.md](docs/ProjectOverview.md)**.

## Run with Docker

```bash
# App + PostgreSQL together (Flyway migrates + seeds automatically)
docker compose up --build
```

`Dockerfile` is multi-stage (builds the jar inside the container) — use it in CI and
proxy-free environments. **Behind a TLS-intercepting proxy** (where the in-container Maven
download fails with a PKIX error), build the jar on the host first and use
[`Dockerfile.local`](Dockerfile.local):

```bash
mvn clean package -DskipTests
docker build -f Dockerfile.local -t insurancepolicylistdemo:local .
docker run -p 8081:8081 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/insuranceDB insurancepolicylistdemo:local
```

A published image is built and pushed to **GitHub Container Registry** on every push to
`master` or `dev` by [`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml):

```bash
docker pull ghcr.io/meetravig-collab/insurancepolicylistdemo:latest
docker run -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/insuranceDB \
  ghcr.io/meetravig-collab/insurancepolicylistdemo:latest
```

## Documentation

| Document | What's inside |
|---|---|
| **[docs/ProjectOverview.md](docs/ProjectOverview.md)** | What the service does, endpoints, features, quick start |
| **[docs/TechStack.md](docs/TechStack.md)** | Technologies used and why; build, container & CI tooling |
| **[docs/Architecture.md](docs/Architecture.md)** | Hexagonal layering, dependency rule, package structure, SOLID |
| **[docs/Design.md](docs/Design.md)** | Domain schema, API contract, caching & key design decisions |
| **[docs/Testing.md](docs/Testing.md)** | Test strategy, layers, coverage, and performance results |
| **[docs/AI-WORKING-JOURNAL.md](docs/AI-WORKING-JOURNAL.md)** | Process log — what the AI accepted, challenged, overrode, with reasoning |

## Project layout

```
src/main/java/com/insurance/dashboard/
├── api/              inbound adapter — controller, DTOs, mapper, exception handler
├── service/          application layer — use cases (depends only on the domain port)
├── domain/           core — POJO model, port, query value objects, domain exceptions
└── infrastructure/   outbound adapter — JPA entity, Spring Data repo, persistence adapter
```

Dependencies point strictly inward (`api → service → domain`, `infrastructure → domain`).
See [docs/Architecture.md](docs/Architecture.md) for the full diagram and rationale.

## Tooling

- **API explorer:** Swagger UI at `http://localhost:8081/swagger-ui/index.html` (serves the committed [`openapi.yaml`](src/main/resources/static/openapi.yaml)) — try every endpoint live.
- **Load test:** `mvn gatling:test` (app must be running).
