# Design — Policy Overview Dashboard

## Purpose

The Policy Overview Dashboard is a read-only REST service that exposes a single endpoint,
`GET /api/policies`, allowing front-end clients to retrieve a paginated, filterable list of
insurance policies with computed expiry indicators.

---

## Architecture

The application follows a layered package structure that enforces explicit dependency direction:
outer layers depend inward; inner layers know nothing of the layers above them.

```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│  api/                           │  ← HTTP boundary
│    controller/PolicyController  │    receives request, returns ResponseEntity
│    mapper/PolicyMapper (iface)  │    converts domain → response DTO
│    dto/response/                │    pure data holders, no logic
└────────────┬────────────────────┘
             │ depends on
             ▼
┌─────────────────────────────────┐
│  service/                       │  ← Business orchestration
│    PolicyService (interface)    │    defines the use-case contract
│    PolicyServiceImpl            │    single implementation
└────────────┬────────────────────┘
             │ depends on
             ▼
┌─────────────────────────────────┐
│  infrastructure/persistence/    │  ← Data access
│    repository/PolicyRepository  │    Spring Data JPA interface
└────────────┬────────────────────┘
             │ queries
             ▼
┌─────────────────────────────────┐
│  domain/model/                  │  ← Domain entities
│    Policy, PolicyHolder         │    JPA-annotated, no framework logic
└─────────────────────────────────┘

Cross-cutting
┌─────────────────────────────────┐
│  common/exception/              │
│    GlobalExceptionHandler       │  503 → structured ErrorResponse
│    ErrorResponse (record)       │  timestamp + status + message
└─────────────────────────────────┘
```

---

## Package Structure

```
com.insurance.dashboard/
├── api/
│   ├── controller/          HTTP layer — no business logic
│   ├── dto/
│   │   └── response/        Response DTOs (pure data, no domain imports)
│   └── mapper/              PolicyMapper interface + PolicyMapperImpl
├── domain/
│   └── model/               Policy, PolicyHolder (JPA entities = domain model)
├── service/                 PolicyService interface + PolicyServiceImpl
├── infrastructure/
│   └── persistence/
│       └── repository/      PolicyRepository, PolicyHolderRepository
├── common/
│   └── exception/           GlobalExceptionHandler, ErrorResponse
└── PolicyOverviewDashboardApplication.java
```

---

## SOLID Principles Applied

### S — Single Responsibility
| Class | Single reason to change |
|---|---|
| `PolicyController` | HTTP request/response shape changes |
| `PolicyServiceImpl` | Retrieval orchestration changes |
| `PolicyMapperImpl` | Domain-to-response mapping rules change |
| `PolicyRepository` | Data access query changes |
| `GlobalExceptionHandler` | Error response contract changes |

### O — Open/Closed
`Policy.Region` carries its own display name via `getDisplayName()`.
Adding a new region (e.g. `KOREA("Korea")`) requires no change to `PolicyMapperImpl`,
`PolicyController`, or `PolicyRepository` — only the enum constant is added.

### L — Liskov Substitution
`PolicyService` and `PolicyMapper` are interfaces. Any implementation that honours their
contracts can replace `PolicyServiceImpl` / `PolicyMapperImpl` transparently —
for example, a caching decorator or a test double.

### I — Interface Segregation
Each interface exposes exactly one method — the one its caller actually needs.

| Interface | Method | Caller |
|---|---|---|
| `PolicyService` | `getPaginatedPolicies(...)` | `PolicyController` |
| `PolicyMapper` | `toResponse(Policy)` | `PolicyServiceImpl` |
| `PolicyRepository` | `findAllWithFilters(...)` | `PolicyServiceImpl` |

### D — Dependency Inversion
Every dependency crosses a layer boundary via an interface, never a concrete class:

```
PolicyController     →  PolicyService (interface)
PolicyServiceImpl    →  PolicyMapper (interface)
PolicyServiceImpl    →  PolicyRepository (interface — Spring Data)
GlobalExceptionHandler  →  Spring exception abstractions
```

No layer instantiates another layer's concrete class with `new`.

---

## Domain Model

```
PolicyHolder (1) ──────< Policy (many)
│                         │
│ id                      │ id
│ firstName               │ policyNumber
│ lastName                │ policyType     [LIFE|HEALTH|AUTO|HOME|TRAVEL]
│ email (unique)          │ premiumAmount
│ phone                   │ coverageAmount
│ dateOfBirth             │ startDate
│ address                 │ endDate
                          │ status         [ACTIVE|INACTIVE|EXPIRED|PENDING|LAPSED]
                          │ region         [SINGAPORE|HONG_KONG|AUSTRALIA|INDIA|JAPAN]
                          │ currency
                          │ policyHolder   (FK)
```

### Computed field — isExpiringSoon
`isExpiringSoon` is not stored. It is calculated at response-mapping time by `PolicyMapperImpl`:

```
isExpiringSoon = endDate ≥ today AND endDate < today + POLICY_EXPIRY_WARNING_DAYS
```

The warning window defaults to 30 days and is configurable via environment variable.

---

## API Contract

### `GET /api/policies`

**Query parameters**

| Parameter | Type | Required | Default | Allowed values |
|---|---|---|---|---|
| `status` | enum | No | — (all) | `ACTIVE` `INACTIVE` `EXPIRED` `PENDING` `LAPSED` |
| `region` | enum | No | — (all) | `SINGAPORE` `HONG_KONG` `AUSTRALIA` `INDIA` `JAPAN` |
| `page` | int | No | `0` | ≥ 0 |
| `size` | int | No | `10` | ≥ 1 |
| `sort` | string | No | `startDate,desc` | Any Policy field |

**Response shape**

```json
{
  "content": [
    {
      "id": 1,
      "policyNumber": "POL-APAC-001",
      "holderName": "John Smith",
      "region": "Singapore",
      "status": "Active",
      "premium": {
        "amount": 300.00,
        "currency": "SGD"
      },
      "startDate": "2024-01-01",
      "endDate": "2029-01-01",
      "isExpiringSoon": false
    }
  ],
  "totalElements": 6,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

**Notes**
- `status` is returned in title case (`"Active"`, not `"ACTIVE"`).
- `region` is returned as a human-readable display name (`"Hong Kong"`, not `"HONG_KONG"`).
- Dates are ISO-8601 (`yyyy-MM-dd`).
- When `status` or `region` is omitted, all values are returned (no filter applied).

**Error responses**

| Scenario | HTTP | Body |
|---|---|---|
| Database unreachable | `503 Service Unavailable` | `{ "timestamp": "...", "status": 503, "message": "Policy service is temporarily unavailable..." }` |

Stack traces are never exposed in error responses.

---

## Key Design Decisions

### Read-only service
`PolicyService` and `PolicyServiceImpl` are annotated `@Transactional(readOnly = true)`.
The application exposes one `GET` endpoint — write operations have no place here.
Marking transactions read-only allows the database to skip dirty-check overhead.

### JOIN FETCH instead of lazy loading
`findAllWithFilters` uses `JOIN FETCH p.policyHolder` to load the holder in a single SQL
statement. Without this, accessing `policy.getPolicyHolder()` inside `PolicyMapperImpl`
would trigger N+1 queries (one extra SELECT per policy).

### Configurable expiry window
The 30-day expiry window is not hardcoded. It is read from `${POLICY_EXPIRY_WARNING_DAYS:30}`,
allowing each deployment environment to override it without a code change.

### Static factory removed from DTOs
`PolicySummaryResponse` is a pure data holder. Domain-to-response mapping belongs in
`PolicyMapperImpl`, not in the DTO. This keeps the DTO free of domain imports and
makes the mapping logic independently testable and replaceable.

### 12-Factor configuration
All environment-specific values are externalised:

| Property | Environment variable | Default |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/insuranceDB` |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` |
| `server.port` | `PORT` | `8081` |
| `policy.expiry.warning-days` | `POLICY_EXPIRY_WARNING_DAYS` | `30` |

No credentials or environment-specific values are hardcoded in source files.
