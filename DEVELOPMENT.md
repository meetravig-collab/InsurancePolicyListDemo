# Development Guide — Policy Overview Dashboard

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java | 17+ | Required by Spring Boot 3.3 |
| Maven | 3.9+ | No wrapper bundled; use a local install |
| PostgreSQL | 13+ | `data.sql` uses `gen_random_uuid()` (core since PG 13); database `insuranceDB` must exist |
| Docker | optional | Only to run via container / compose |
| Node.js | optional | Only to run the Postman collection via `newman` |

---

## Initial Setup

### 1. Create the database

```sql
CREATE DATABASE "insuranceDB";
```

The schema (tables, columns, enum check constraints) is created automatically by Hibernate
on first run (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Seed sample data (220 records)

`data.sql` is **not** auto-run against PostgreSQL (Spring's `sql.init.mode` is `embedded`
by default). Load it manually **after the app has started once** so the tables exist:

```bash
psql -U postgres -d insuranceDB -f src/main/resources/data.sql
```

It truncates first, then inserts 220 realistic policies covering all statuses, lines of
business, APAC regions, currencies, and a spread of dates and premiums.

### 3. Environment variables (only if overriding defaults)

Every value has a local default, so a standard local PostgreSQL needs none of these.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/insuranceDB` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `PORT` | `8081` | HTTP server port |
| `POLICY_EXPIRY_WARNING_DAYS` | `30` | Window for the `isExpiringSoon` flag |
| `CACHE_TTL_SECONDS` | `60` | Caffeine cache entry TTL |
| `CACHE_MAX_SIZE` | `1000` | Caffeine max entries per cache (LRU) |

> **Windows note (this machine):** behind the TLS-intercepting proxy, prefix Maven commands
> with `set MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` so dependency downloads
> verify against the Windows certificate store.

---

## Build

```bash
mvn test                  # compile + run all 34 tests (needs PostgreSQL running)
mvn package -DskipTests   # package the jar, skip tests
mvn package               # package + run tests
```

Built jar: `target/insurance-policy-holders-dashboard-0.0.1-SNAPSHOT.jar`

---

## Run

### Maven plugin (development)
```bash
mvn spring-boot:run        # http://localhost:8081 ; DevTools auto-restart on class changes
```

### Jar
```bash
java -jar target/insurance-policy-holders-dashboard-0.0.1-SNAPSHOT.jar
# inline overrides:
DB_PASSWORD=secret PORT=9090 java -jar target/*.jar
```

### Docker
See [README.md](README.md#run-with-docker) — `docker compose up --build`, or `Dockerfile`
(CI / multi-stage) vs `Dockerfile.local` (host-built jar, for behind a TLS proxy).

### Verify startup
```
Tomcat started on port 8081 (http) with context path '/'
Started PolicyOverviewDashboardApplication in X.XXX seconds
```

> If startup fails with `NoClassDefFoundError: CaffeineCacheManager`, a **stale**
> `spring-boot:run` process (started before a dependency was added) is still holding the
> port — DevTools can't load new JARs into a running JVM. Kill it and restart:
> `Get-Process java | Stop-Process -Force`.

---

## API Reference

Base URL `http://localhost:8081`. Live docs (Swagger UI): `/swagger-ui/index.html`;
OpenAPI spec: `/v3/api-docs` and the canonical [`openapi.yaml`](openapi.yaml).

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/policies` | List — pagination, sorting, filtering, free-text search |
| `GET` | `/api/v1/policies/{id}` | Single policy by UUID |
| `PATCH` | `/api/v1/policies/flag` | Bulk flag policies for review |
| `GET` | `/api/v1/policies/summary` | Counts by status, premium by line of business, expiring-soon count |

### `GET /api/v1/policies` — query parameters

| Parameter | Default | Allowed / format |
|---|---|---|
| `status` | all | `ACTIVE` `EXPIRED` `PENDING` `CANCELLED` |
| `region` | all | `SINGAPORE` `HONG_KONG` `AUSTRALIA` `JAPAN` `THAILAND` `INDONESIA` `MALAYSIA` `PHILIPPINES` |
| `lineOfBusiness` | all | `PROPERTY` `CASUALTY` `ACCIDENT_AND_HEALTH` `MARINE` |
| `effectiveDateFrom` / `effectiveDateTo` | — | `yyyy-MM-dd` (range on effective date) |
| `search` | — | free-text across `policyNumber`, `policyholderName`, `underwriter` |
| `page` / `size` | `0` / `10` | |
| `sort` | `effectiveDate,desc` | e.g. `premiumAmount,desc` |

#### Examples
```bash
curl "http://localhost:8081/api/v1/policies?status=ACTIVE&region=JAPAN"
curl "http://localhost:8081/api/v1/policies?lineOfBusiness=MARINE&sort=premiumAmount,desc"
curl "http://localhost:8081/api/v1/policies?search=Acme&page=0&size=20"
curl "http://localhost:8081/api/v1/policies/{id}"
curl "http://localhost:8081/api/v1/policies/summary"
curl -X PATCH http://localhost:8081/api/v1/policies/flag \
     -H "Content-Type: application/json" -d '{"policyIds":["<uuid>"]}'
```

#### Response — `200 OK` (list item / detail)
```json
{
  "id": "ca201fac-8cef-4770-a4c9-7fa7cdf2c9c5",
  "policyNumber": "POL-100099",
  "policyholderName": "Daniel Lim",
  "lineOfBusiness": "Marine",
  "status": "Active",
  "premiumAmount": 808657.36,
  "currency": "AUD",
  "effectiveDate": "2022-12-16",
  "expiryDate": "2026-06-12",
  "region": "Japan",
  "underwriter": "Orient Assurance",
  "flaggedForReview": false,
  "isExpiringSoon": true,
  "createdAt": "2026-06-07T11:42:38.252629Z",
  "updatedAt": "2026-06-07T11:42:38.252629Z"
}
```
(The list endpoint wraps items in a Spring `Page`: `content[]`, `totalElements`,
`totalPages`, `size`, `number`, `first`, `last`.)

#### Response field reference
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | primary key |
| `policyNumber` | String | unique, `POL-XXXXXX` |
| `policyholderName` | String | |
| `lineOfBusiness` | String | display name, e.g. `A&H` (never the raw enum) |
| `status` | String | title-cased, e.g. `Active` (never `ACTIVE`) |
| `premiumAmount` / `currency` | Decimal / String | 1,000–5,000,000; `USD SGD HKD AUD JPY THB` |
| `effectiveDate` / `expiryDate` | Date | ISO-8601 `yyyy-MM-dd` |
| `region` | String | display name, e.g. `Hong Kong` |
| `underwriter` | String | |
| `flaggedForReview` | Boolean | set via `PATCH /flag` |
| `isExpiringSoon` | Boolean | computed: `expiryDate` within the warning window |
| `createdAt` / `updatedAt` | Timestamp | set by the persistence layer |

#### Error responses
| Scenario | HTTP | Body |
|---|---|---|
| Unknown id | `404` | `{ timestamp, status: 404, message: "Policy not found with id: ..." }` |
| Empty `policyIds` on flag | `400` | `{ status: 400, errors: { policyIds: "policyIds must not be empty" } }` |
| Database unreachable | `503` | `{ timestamp, status: 503, message: "Policy service is temporarily unavailable..." }` |

No stack traces or internal class names appear in error bodies.

---

## Caching

Frequently accessed reads are cached with Caffeine (Spring Cache abstraction):
`policyListings` (list), `policyById` (detail), `policySummary` (summary). Invalidation is
**TTL-based** (`CACHE_TTL_SECONDS`, default 60s) plus **event-based** — `PATCH /flag` evicts
the listings and detail caches immediately. See [DESIGN.md](DESIGN.md) for the full strategy.

---

## Postman

Import `PolicyOverviewDashboard.postman_collection.json`. It is self-contained — the first
request captures a real policy UUID into `{{policyId}}` that the get-by-id / flag / verify
requests reuse — and every request has `pm.test` assertions. 12 requests, runnable in the
GUI (**Run collection**) or headless:

```bash
newman run PolicyOverviewDashboard.postman_collection.json
```

`{{baseUrl}}` defaults to `http://localhost:8081`.

---

## Logging (SLF4J / Logback)

| Logger | Level | What it logs |
|---|---|---|
| `PolicyController` | `DEBUG` | incoming request params |
| `PolicyServiceImpl` | `DEBUG` / `INFO` | filter+page (debug); flag operations (info) |
| `PolicyMapperImpl` | `DEBUG` / `WARN` | per-record mapping; warns on null status |
| `GlobalExceptionHandler` | `WARN` / `ERROR` | 404/400 (warn); data-access failure with stack trace (error) |

```bash
java -jar target/*.jar --logging.level.com.insurance.dashboard=DEBUG
```

---

## Project Structure (hexagonal / ports & adapters)

```
src/main/java/com/insurance/dashboard/
├── api/                                  inbound adapter (HTTP)
│   ├── controller/PolicyController
│   ├── dto/request/FlagPoliciesRequest
│   ├── dto/response/PolicySummaryResponse, FlagPoliciesResponse, PolicySummaryStats
│   └── mapper/PolicyMapper (+ PolicyMapperImpl)
├── service/            PolicyService (interface) + PolicyServiceImpl, PolicySummary
├── domain/             core — no framework deps
│   ├── model/Policy    persistence-ignorant POJO + enums
│   ├── port/PolicyRepositoryPort
│   └── query/PolicyFilter, PageQuery, PageResult, SortDirection
├── infrastructure/
│   └── persistence/    PolicyPersistenceAdapter, PolicySpecification,
│                       entity/PolicyEntity (+ PolicyEntityMapper),
│                       repository/PolicyJpaRepository
├── config/             CacheConfig, CacheNames
├── common/exception/   GlobalExceptionHandler, ErrorResponse, PolicyNotFoundException
└── PolicyOverviewDashboardApplication.java

src/test/java/com/insurance/dashboard/
├── acceptance/   PolicyListAcceptanceTest, PolicyDatabaseFailureAcceptanceTest
├── controller/   PolicyControllerTest
├── service/      PolicyServiceTest, PolicyCachingTest
└── performance/  PolicyEndpointSimulation (Gatling)

src/main/resources/   application.properties, data.sql
```

Dependencies point strictly inward (`api → service → domain`, `infrastructure → domain`).

---

## Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API, embedded Tomcat |
| `spring-boot-starter-data-jpa` | JPA / Hibernate ORM |
| `spring-boot-starter-validation` | Bean Validation (JSR-380) |
| `spring-boot-starter-cache` + `caffeine` | Caching abstraction + Caffeine provider |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI / OpenAPI docs |
| `postgresql` | PostgreSQL JDBC driver |
| `spring-boot-devtools` | Auto-restart in development |
| `lombok` | `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`, `@Data` |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc, Spring Test |
| `gatling-charts-highcharts` | Load testing (test scope) |
