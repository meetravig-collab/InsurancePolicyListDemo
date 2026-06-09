# Design — Policy Overview Dashboard

Domain model, API contract, and the key design decisions behind them.
(For structure/layering see [Architecture.md](Architecture.md).)

## Domain model

`domain.model.Policy` — a persistence-ignorant POJO. Canonical schema:

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | primary key |
| `policyNumber` | String | unique, `POL-XXXXXX` |
| `policyholderName` | String | |
| `lineOfBusiness` | enum | `PROPERTY`, `CASUALTY`, `ACCIDENT_AND_HEALTH` (A&H), `MARINE` |
| `status` | enum | `ACTIVE`, `EXPIRED`, `PENDING`, `CANCELLED` |
| `premiumAmount` | BigDecimal | 1,000 – 5,000,000 |
| `currency` | String | `USD` `SGD` `HKD` `AUD` `JPY` `THB` |
| `effectiveDate` / `expiryDate` | LocalDate | |
| `region` | enum | Singapore, Hong Kong, Australia, Japan, Thailand, Indonesia, Malaysia, Philippines |
| `underwriter` | String | |
| `flaggedForReview` | boolean | default false |
| `createdAt` / `updatedAt` | Instant | set by the persistence entity |

**Computed `isExpiringSoon`** (not stored) — set during mapping:
```
isExpiringSoon = expiryDate ≥ today AND expiryDate < today + POLICY_EXPIRY_WARNING_DAYS
```

## API contract

Contract-first: [`openapi.yaml`](../src/main/resources/static/openapi.yaml) is the source of
truth; Swagger UI at `/swagger-ui/index.html`.

### `GET /api/v1/policies` — parameters
| Parameter | Default | Allowed / format |
|---|---|---|
| `status` | all | `ACTIVE` `EXPIRED` `PENDING` `CANCELLED` |
| `region` | all | 8 APAC regions |
| `lineOfBusiness` | all | `PROPERTY` `CASUALTY` `ACCIDENT_AND_HEALTH` `MARINE` |
| `effectiveDateFrom` / `effectiveDateTo` | — | `yyyy-MM-dd` (range on effective date) |
| `search` | — | free-text across `policyNumber`, `policyholderName`, `underwriter` |
| `page` / `size` | `0` / `10` | |
| `sort` | `effectiveDate,desc` | e.g. `premiumAmount,desc` |

### Response (`PolicySummaryResponse`)
`id` (UUID), `policyNumber`, `policyholderName`, `lineOfBusiness` (display name, e.g. `A&H`),
`status` (title-case, e.g. `Active`), `premiumAmount`, `currency`, `effectiveDate`,
`expiryDate`, `region` (display name), `underwriter`, `flaggedForReview`, `isExpiringSoon`,
`createdAt`, `updatedAt`. The list endpoint wraps these in a Spring `Page`.

### Other endpoints
- `GET /api/v1/policies/{id}` → single policy, or `404` if unknown.
- `PATCH /api/v1/policies/flag` → body `{ "policyIds": ["<uuid>", ...] }`; sets `flaggedForReview=true`; returns `{ flaggedCount, policyIds }`. Empty list → `400`.
- `GET /api/v1/policies/summary` → `{ countsByStatus, totalPremiumByLineOfBusiness, expiringSoonCount }`.

### Errors
| Scenario | HTTP | Body |
|---|---|---|
| Unknown id | `404` | `{ timestamp, status, message }` |
| Empty `policyIds` | `400` | `{ status, errors: { policyIds } }` |
| Database unreachable | `503` | `{ timestamp, status, message }` |

Stack traces and internal class names are never exposed.

## Key design decisions

### Contract-first
The OpenAPI spec is authored alongside the implementation and is the single source of
truth for the API shape; SpringDoc serves it live.

### POJO domain / JPA entity split
The domain model and the persistence entity are separate classes, mapped by
`PolicyEntityMapper`. ORM concerns (annotations, lazy loading, identity) never reach
business logic, and the two can evolve independently.

### Domain-owned persistence port
`PolicyRepositoryPort` lives in the domain and is implemented by an infrastructure adapter,
so persistence technology can change without touching domain or service code.

### Specification-based filtering (isolated in infrastructure)
The service passes a domain `PolicyFilter`; `PolicySpecification`
(`infrastructure/persistence/specification`) turns it into a JPA `Specification` by
**composing one self-contained, null-safe criterion per field** via `Specification.allOf(...)`.
Adding a new filter means adding a criterion + one line — existing criteria are untouched
(Open/Closed). It also handles all-optional filtering and avoids the PostgreSQL
`lower(bytea)` null-type inference issue a hand-written JPQL `LIKE` hits.

### Schema migrations (Flyway)
The schema and sample data are version-controlled as Flyway migrations in
`src/main/resources/db/migration` — `V1__init.sql` (schema) and `V2__seed_sample_data.sql`
(220 rows), applied automatically on startup and tracked in `flyway_schema_history`.
Hibernate runs `ddl-auto=validate` — it only checks the entity mapping matches the
Flyway-built schema and never alters it. This makes the schema reproducible and removes
the risk of `ddl-auto=update` silently changing tables.

### Caching of frequently accessed reads
Caffeine-backed caches via the Spring Cache abstraction:

| Cache | Method | Key |
|---|---|---|
| `policyListings` | `getPolicies` | filter + page query |
| `policyById` | `getPolicyById` | UUID |
| `policySummary` | `getSummary` | single entry |

**Invalidation — two layers:**
1. **TTL** — `expireAfterWrite = CACHE_TTL_SECONDS` (default 60s), bounded by `CACHE_MAX_SIZE` (LRU). Caps staleness for out-of-band changes.
2. **Event-based** — `flagPoliciesForReview` `@CacheEvict`s `policyListings` + `policyById` immediately (it changes `flaggedForReview`, shown in listings/detail). Summary relies on TTL.

Caffeine is local/single-node; the `CacheManager` bean can be swapped for Redis for
multi-instance deployments without changing the service annotations.

### Read-only by default
`PolicyServiceImpl` is `@Transactional(readOnly = true)`; only `flagPoliciesForReview` is a
read-write transaction (a bulk `@Modifying` update).

### 12-Factor configuration
All environment-specific values are externalized with local defaults:

| Property | Env var | Default |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/insuranceDB` |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` |
| `server.port` | `PORT` | `8081` |
| `policy.expiry.warning-days` | `POLICY_EXPIRY_WARNING_DAYS` | `30` |
| `cache.ttl-seconds` | `CACHE_TTL_SECONDS` | `60` |
| `cache.max-size` | `CACHE_MAX_SIZE` | `1000` |

### Observability & health
SLF4J/Logback logging to stdout: `INFO` for write operations, `DEBUG` for reads,
`WARN` for client errors (404/400), `ERROR` (with stack trace) for data-access failures.
Spring Boot Actuator exposes `/actuator/health` (with the PostgreSQL component) plus
`liveness` and `readiness` probes for orchestrator health checks.
