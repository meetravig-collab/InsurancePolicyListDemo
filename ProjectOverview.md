# Project Overview — Policy Overview Dashboard

A Spring Boot REST service (BFF) for retrieving and triaging insurance policies, built
**contract-first** with a **hexagonal (ports & adapters)** architecture.

## What it does

Exposes four endpoints under `/api/v1/policies` over a PostgreSQL-backed catalogue of
insurance policies, with filtering, free-text search, pagination, aggregated statistics,
and a bulk review-flagging action.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/policies` | List — pagination, sorting, filtering, free-text search |
| `GET` | `/api/v1/policies/{id}` | Get a single policy by UUID |
| `PATCH` | `/api/v1/policies/flag` | Bulk flag policies for review |
| `GET` | `/api/v1/policies/summary` | Counts by status, premium by line of business, expiring-soon count |

## Key features

- **Filtering** by status, region, line of business, and effective-date range.
- **Free-text search** across policy number, policyholder name, and underwriter.
- **Pagination & sorting** (e.g. `sort=premiumAmount,desc`).
- **Computed `isExpiringSoon`** flag (configurable warning window).
- **Aggregated summary** — counts by status, total premium by line of business, expiring-soon count.
- **Caching** of hot reads (Caffeine) with TTL + event-based invalidation.
- **Contract-first API** — [`openapi.yaml`](openapi.yaml) is the source of truth; Swagger UI at `/swagger-ui/index.html`.
- **Safe errors** — structured 404 / 400 / 503 responses, never leaking stack traces.
- **Performance** — list & detail p95 < 300ms under 50 concurrent sessions (Gatling-gated).

## Quick start

```bash
# Prerequisites: Java 17+, Maven 3.9+, PostgreSQL 13+ with a database named insuranceDB
mvn spring-boot:run                                             # http://localhost:8081
psql -U postgres -d insuranceDB -f src/main/resources/data.sql  # seed 220 sample policies
```

Then browse `http://localhost:8081/swagger-ui/index.html` or:
```bash
curl "http://localhost:8081/api/v1/policies?status=ACTIVE&region=JAPAN"
```

> On a machine behind a TLS-intercepting proxy, prefix Maven with
> `set MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`.

## Run with Docker

```bash
docker compose up --build
docker compose exec -T db psql -U postgres -d insuranceDB < src/main/resources/data.sql
```
A published image is pushed to GHCR on every push to `master`/`dev`:
`ghcr.io/meetravig-collab/insurancepolicylistdemo:latest`. See [TechStack.md](TechStack.md)
for the Docker/CI details.

## Documentation

| Document | Contents |
|---|---|
| [TechStack.md](TechStack.md) | Technologies used and why; build, container & CI tooling |
| [Architecture.md](Architecture.md) | Hexagonal layering, dependency rule, package structure, SOLID |
| [Design.md](Design.md) | Domain schema, API contract, caching & key design decisions |
| [Testing.md](Testing.md) | Test strategy, layers, coverage, performance results |

## Sample data

220 realistic policies spanning all 4 statuses, 4 lines of business, 8 APAC regions,
6 currencies, premiums 1,000–5,000,000, and a realistic date spread.
