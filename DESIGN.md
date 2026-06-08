# Design — Policy Overview Dashboard

## Purpose

The Policy Overview Dashboard is a REST service for retrieving and triaging insurance
policies. It exposes four endpoints under `/api/v1/policies` — a filtered/paginated list,
get-by-id, bulk flag-for-review, and aggregated summary statistics.

The codebase follows a **hexagonal (ports & adapters) architecture**: the domain sits at
the centre with no framework dependencies, and all dependencies point inward.

---

## Architecture

```
                       HTTP
                        │
        ┌───────────────▼─────────────────────────────┐
        │  api/  (inbound adapter)                     │
        │    controller/PolicyController               │  Spring MVC, Pageable
        │    dto/request, dto/response                 │  wire-format DTOs
        │    mapper/PolicyMapper (+Impl)               │  domain → DTO
        └───────────────┬─────────────────────────────┘
                        │ depends on
        ┌───────────────▼─────────────────────────────┐
        │  service/  (application)                     │
        │    PolicyService (interface) + Impl          │  use cases / orchestration
        │    PolicySummary (app result record)         │
        └───────────────┬─────────────────────────────┘
                        │ depends on
        ┌───────────────▼─────────────────────────────┐
        │  domain/  (core — no framework deps)         │
        │    model/Policy (POJO) + enums               │
        │    port/PolicyRepositoryPort  ◄──────────┐   │  outbound PORT
        │    query/PolicyFilter, PageQuery,        │   │
        │          PageResult, SortDirection       │   │
        └──────────────────────────────────────────┼──┘
                                                    │ implements
        ┌───────────────────────────────────────────┼──┐
        │  infrastructure/  (outbound adapter)       │  │
        │    persistence/PolicyPersistenceAdapter ───┘  │  implements the port
        │    persistence/entity/PolicyEntity (@Entity)  │  JPA mapping
        │    persistence/entity/PolicyEntityMapper      │  entity ↔ domain
        │    persistence/repository/PolicyJpaRepository │  Spring Data JPA
        │    persistence/PolicySpecification            │  filter → Specification
        └───────────────────────────────────────────────┘

Cross-cutting:  common/exception/  → GlobalExceptionHandler, ErrorResponse,
                                      PolicyNotFoundException
```

**Dependency rule.** Every arrow points inward. The domain depends on nothing; the
service depends only on the domain (and its port); the api and infrastructure layers
are the outer adapters and depend inward on the domain/application. The infrastructure
adapter is wired to the service at runtime via the `PolicyRepositoryPort` interface
(dependency inversion).

---

## Package Structure

```
com.insurance.dashboard/
├── api/                                  inbound adapter (HTTP)
│   ├── controller/PolicyController
│   ├── dto/request/FlagPoliciesRequest
│   ├── dto/response/PolicySummaryResponse, FlagPoliciesResponse, PolicySummaryStats
│   └── mapper/PolicyMapper (+ PolicyMapperImpl)
├── service/                              application layer
│   ├── PolicyService (interface) + PolicyServiceImpl
│   └── PolicySummary                     application result record
├── domain/                              core — zero framework dependencies
│   ├── model/Policy                      persistence-ignorant POJO + enums
│   ├── port/PolicyRepositoryPort         outbound port (owned by domain)
│   └── query/PolicyFilter, PageQuery, PageResult, SortDirection
├── infrastructure/                       outbound adapter (persistence)
│   └── persistence/
│       ├── PolicyPersistenceAdapter      implements PolicyRepositoryPort
│       ├── PolicySpecification           PolicyFilter → Specification<PolicyEntity>
│       ├── entity/PolicyEntity           the JPA @Entity
│       ├── entity/PolicyEntityMapper     entity ↔ domain
│       └── repository/PolicyJpaRepository Spring Data JPA + JpaSpecificationExecutor
├── common/exception/                     GlobalExceptionHandler, ErrorResponse, exceptions
└── PolicyOverviewDashboardApplication.java
```

---

## Why hexagonal — verified isolation

Two properties are enforced (and checked by grepping imports):

1. **The domain is persistence-ignorant.** `domain/` contains no `jakarta.persistence`,
   `org.hibernate`, or `org.springframework` imports. `Policy` is a plain POJO; the JPA
   `@Entity` (`PolicyEntity`) lives in infrastructure, with `PolicyEntityMapper` translating
   between them.

2. **The service never sees infrastructure.** `service/` imports no `infrastructure.*` and
   no `org.springframework.data.*`. It depends only on the domain `PolicyRepositoryPort`
   abstraction and domain value objects (`PolicyFilter`, `PageQuery`, `PageResult`).
   Spring Data JPA, `Specification`, and `Pageable` are confined to the infrastructure
   adapter and the api controller.

### How pagination crosses the boundary
- The **controller** (api adapter) accepts a Spring `Pageable`, converts it to a domain
  `PageQuery`, and builds a domain `PolicyFilter` from the query parameters.
- The **service** deals only in `PolicyFilter` / `PageQuery` / `PageResult<Policy>`.
- The **adapter** converts `PageQuery` → Spring `PageRequest`, runs the query, and maps
  `PolicyEntity` → `Policy`, returning a domain `PageResult<Policy>`.
- The controller maps `PageResult<Policy>` → `Page<PolicySummaryResponse>` (a Spring
  `PageImpl`) so the JSON wire format is unchanged.

---

## SOLID

| Principle | How it shows up |
|---|---|
| **S**ingle Responsibility | controller = HTTP; service = use cases; adapter = persistence; mapper = domain↔DTO; entity mapper = entity↔domain |
| **O**pen/Closed | `Region`/`LineOfBusiness` carry their own display names; adding a value touches only the enum |
| **L**iskov | `PolicyService`, `PolicyMapper`, `PolicyRepositoryPort` are interfaces; any conforming impl substitutes cleanly (e.g. an in-memory adapter for tests) |
| **I**nterface Segregation | the port exposes only the operations the application needs |
| **D**ependency Inversion | the service depends on the domain-owned `PolicyRepositoryPort`; the Spring Data adapter is injected at runtime — the high-level policy does not depend on the persistence detail |

---

## Domain Model

`domain.model.Policy` (POJO) — the canonical schema:

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | primary key |
| `policyNumber` | String | unique, `POL-XXXXXX` |
| `policyholderName` | String | |
| `lineOfBusiness` | enum | `PROPERTY`, `CASUALTY`, `ACCIDENT_AND_HEALTH` (A&H), `MARINE` |
| `status` | enum | `ACTIVE`, `EXPIRED`, `PENDING`, `CANCELLED` |
| `premiumAmount` | BigDecimal | range 1,000 – 5,000,000 |
| `currency` | String | `USD`, `SGD`, `HKD`, `AUD`, `JPY`, `THB` |
| `effectiveDate` | LocalDate | |
| `expiryDate` | LocalDate | |
| `region` | enum | Singapore, Hong Kong, Australia, Japan, Thailand, Indonesia, Malaysia, Philippines |
| `underwriter` | String | |
| `flaggedForReview` | boolean | default false |
| `createdAt` / `updatedAt` | Instant | set by the JPA entity (`@CreationTimestamp`/`@UpdateTimestamp`) |

### Computed field — `isExpiringSoon`
Not stored. Computed by `PolicyMapperImpl` at response time:
```
isExpiringSoon = expiryDate ≥ today AND expiryDate < today + POLICY_EXPIRY_WARNING_DAYS
```
The warning window defaults to 30 days, configurable via `POLICY_EXPIRY_WARNING_DAYS`.

---

## API Contract

Contract-first: `openapi.yaml` is the single source of truth. SpringDoc serves live docs at
`/swagger-ui.html`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/policies` | list — pagination, sort, filter, free-text search |
| `GET` | `/api/v1/policies/{id}` | single policy by UUID |
| `PATCH` | `/api/v1/policies/flag` | bulk flag policies for review |
| `GET` | `/api/v1/policies/summary` | counts by status, premium by line of business, expiring-soon count |

### `GET /api/v1/policies` parameters

| Parameter | Type | Default | Notes |
|---|---|---|---|
| `status` | enum | all | `ACTIVE` `EXPIRED` `PENDING` `CANCELLED` |
| `region` | enum | all | 8 APAC regions |
| `lineOfBusiness` | enum | all | `PROPERTY` `CASUALTY` `ACCIDENT_AND_HEALTH` `MARINE` |
| `effectiveDateFrom` / `effectiveDateTo` | date | — | range on effective date (`yyyy-MM-dd`) |
| `search` | string | — | free-text across `policyNumber`, `policyholderName`, `underwriter` |
| `page` / `size` | int | `0` / `10` | |
| `sort` | string | `effectiveDate,desc` | e.g. `premiumAmount,desc` |

### Response fields (`PolicySummaryResponse`)
`id` (UUID), `policyNumber`, `policyholderName`, `lineOfBusiness` (display name, e.g. `A&H`),
`status` (title case, e.g. `Active`), `premiumAmount`, `currency`, `effectiveDate`,
`expiryDate`, `region` (display name), `underwriter`, `flaggedForReview`, `isExpiringSoon`,
`createdAt`, `updatedAt`.

### Error responses

| Scenario | HTTP | Body |
|---|---|---|
| Unknown id | `404` | `{ timestamp, status: 404, message: "Policy not found with id: ..." }` |
| Empty `policyIds` on flag | `400` | validation error body |
| Database unreachable | `503` | `{ timestamp, status: 503, message: "Policy service is temporarily unavailable..." }` |

Stack traces and internal class names are never exposed.

---

## Key Design Decisions

### Domain-owned port (Dependency Inversion)
`PolicyRepositoryPort` is declared in `domain.port` and implemented by
`PolicyPersistenceAdapter` in infrastructure. The application depends on the abstraction
it owns, not on Spring Data — so persistence technology can change without touching the
domain or service.

### POJO domain / JPA entity split
The domain model and the persistence entity are separate classes. This keeps ORM concerns
(lazy loading, identity, annotations) out of business logic and lets the two evolve
independently. `PolicyEntityMapper` is the single translation point.

### Filtering via Specification, isolated in infrastructure
Dynamic multi-criteria filtering uses `JpaSpecificationExecutor`. The service passes a
domain `PolicyFilter`; `PolicySpecification` (infrastructure) turns it into a
`Specification<PolicyEntity>`. This also avoids the PostgreSQL `lower(bytea)` null-type
inference problem that a hand-written JPQL `LIKE` with nullable params hits.

### Read-only by default
`PolicyServiceImpl` is `@Transactional(readOnly = true)`; only `flagPoliciesForReview`
is a read-write transaction (a bulk `@Modifying` update).

### Caching of frequently accessed reads
A Caffeine-backed cache (Spring Cache abstraction, `@EnableCaching`) fronts the three
hot reads, applied as annotations on the service:

| Cache | Method | Key |
|---|---|---|
| `policyListings` | `getPolicies(filter, page)` | filter + page query (records → value-based key) |
| `policyById` | `getPolicyById(id)` | the UUID |
| `policySummary` | `getSummary()` | single entry |

**Invalidation strategy — two layers:**
1. **Time-based (TTL).** Every entry expires `expireAfterWrite` `cache.ttl-seconds`
   (default 60s), with a bounded `cache.max-size` (LRU). This caps staleness even for
   out-of-band DB changes (e.g. the seed script or a future writer).
2. **Event-based.** `flagPoliciesForReview` mutates `flaggedForReview`, which is visible
   in listings and detail, so it `@CacheEvict`s `policyListings` and `policyById`
   immediately (`allEntries = true`). Summary is unaffected by flagging and relies on TTL.

Caffeine is a local single-node cache appropriate for this BFF. Because caching is wired
through the Spring Cache abstraction, the `CacheManager` bean can be swapped for a
distributed provider (e.g. Redis) for multi-instance deployments **without changing the
service-layer annotations**.

### Configurable expiry window
The expiry warning window is read from `${policy.expiry.warning-days:30}` and used by the
mapper (`isExpiringSoon`) and the summary aggregation.

### 12-Factor configuration
All environment-specific values are externalised with local defaults:

| Property | Env var | Default |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/insuranceDB` |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` |
| `server.port` | `PORT` | `8081` |
| `policy.expiry.warning-days` | `POLICY_EXPIRY_WARNING_DAYS` | `30` |
| `cache.ttl-seconds` | `CACHE_TTL_SECONDS` | `60` |
| `cache.max-size` | `CACHE_MAX_SIZE` | `1000` |
