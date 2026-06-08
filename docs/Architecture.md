# Architecture — Policy Overview Dashboard

Hexagonal (ports & adapters). The domain sits at the centre with **no framework
dependencies**; every dependency points inward.

## Layer diagram

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
        │    PolicySummary (application result)        │
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

Cross-cutting:  config/ (caching)   api/exception/ (HTTP error handling)   domain/exception/ (domain errors)
```

## The dependency rule

`api → service → domain` and `infrastructure → domain`. The domain depends on nothing;
the infrastructure adapter is wired to the service at runtime through the
`PolicyRepositoryPort` interface (dependency inversion).

**Verified isolation (enforced, checked by import scans):**
- The **domain** has zero `jakarta.persistence`, `org.hibernate`, or `org.springframework` imports — `Policy` is a plain POJO; the JPA `@Entity` (`PolicyEntity`) lives in infrastructure.
- The **service** has zero `infrastructure.*` and `org.springframework.data.*` imports — it depends only on the domain port and domain value objects. Spring Data JPA, `Specification`, and `Pageable` are confined to the adapter and the controller.

## Package structure

```
com.insurance.dashboard/
├── api/                                  inbound adapter (HTTP)
│   ├── controller/PolicyController
│   ├── dto/request/FlagPoliciesRequest
│   ├── dto/response/PolicySummaryResponse, FlagPoliciesResponse, PolicySummaryStats
│   ├── mapper/PolicyMapper (+ PolicyMapperImpl)
│   └── exception/GlobalExceptionHandler, ErrorResponse
├── service/            PolicyService (interface) + PolicyServiceImpl, PolicySummary
├── domain/             core — no framework deps
│   ├── model/Policy    persistence-ignorant POJO + enums
│   ├── port/PolicyRepositoryPort
│   ├── query/PolicyFilter, PageQuery, PageResult, SortDirection
│   └── exception/PolicyNotFoundException
├── infrastructure/persistence/
│   ├── PolicyPersistenceAdapter          implements PolicyRepositoryPort
│   ├── PolicySpecification               PolicyFilter → Specification<PolicyEntity>
│   ├── entity/PolicyEntity (+ PolicyEntityMapper)
│   └── repository/PolicyJpaRepository    Spring Data JPA + JpaSpecificationExecutor
├── config/             CacheConfig, CacheNames
└── PolicyOverviewDashboardApplication.java
```

## How pagination crosses the boundary

- The **controller** accepts a Spring `Pageable`, converts it to a domain `PageQuery`, and builds a domain `PolicyFilter` from the query parameters.
- The **service** deals only in `PolicyFilter` / `PageQuery` / `PageResult<Policy>`.
- The **adapter** converts `PageQuery` → Spring `PageRequest`, runs the query, maps `PolicyEntity` → `Policy`, and returns a domain `PageResult<Policy>`.
- The controller maps `PageResult<Policy>` → a Spring `Page<PolicySummaryResponse>` so the JSON shape is unchanged.

## SOLID

| Principle | How it shows up |
|---|---|
| **S**ingle Responsibility | controller = HTTP; service = use cases; adapter = persistence; mappers = translation |
| **O**pen/Closed | `Region`/`LineOfBusiness` carry display names; adding a value touches only the enum |
| **L**iskov | `PolicyService`, `PolicyMapper`, `PolicyRepositoryPort` are interfaces; any conforming impl substitutes (e.g. an in-memory adapter in tests) |
| **I**nterface Segregation | the port exposes only the operations the application needs |
| **D**ependency Inversion | the service depends on the domain-owned port; the Spring Data adapter is injected at runtime |
