# Testing Guide — Policy Overview Dashboard

## Test Strategy

The test suite is layered to give fast feedback on correctness while also validating
full-stack behaviour and non-functional requirements.

```
┌──────────────────────────────────────────────┐
│  Performance (Gatling)                        │  NFR gate: p95 < 500ms @ 50 users
├──────────────────────────────────────────────┤
│  Acceptance (SpringBootTest + PostgreSQL)     │  BDD scenarios, real DB, auto-rollback
├──────────────────────────────────────────────┤
│  Controller slice (WebMvcTest + MockBean)     │  HTTP contract, exception handling
├──────────────────────────────────────────────┤
│  Unit (MockitoExtension)                      │  Service & mapper logic, fast
└──────────────────────────────────────────────┘
```

---

## Running the Tests

### All unit + integration tests

```bash
mvn test
```

### Performance test only (requires the application to be running)

```bash
# Terminal 1 — start the application
mvn spring-boot:run

# Terminal 2 — run the load test
mvn gatling:test
```

Override the target URL if the application runs on a different host or port:

```bash
mvn gatling:test -DbaseUrl=http://localhost:8081
```

Reports are written to `target/gatling/` — open the `index.html` for the full HTML report.

---

## Test Types

### 1. Unit Tests

**Location:** `src/test/java/com/insurance/dashboard/service/`

**Tools:** JUnit 5, Mockito

**What they test:** `PolicyServiceImpl` in isolation.
`PolicyRepository` is mocked; `PolicyMapperImpl` is used as the real implementation
(its logic is simple and deterministic).

| Test | Asserts |
|---|---|
| `getPaginatedPolicies_returnsPageOfSummaries` | holderName, region display name, title-case status |
| `getPaginatedPolicies_withStatusFilter_delegatesFilterToRepository` | filter is passed through unchanged |
| `getPaginatedPolicies_withRegionFilter_delegatesFilterToRepository` | region filter is passed through |
| `getPaginatedPolicies_isExpiringSoon_trueWhenEndDateWithin30Days` | expiry flag set correctly |
| `getPaginatedPolicies_isExpiringSoon_falseWhenEndDateBeyond30Days` | expiry flag not set |

```bash
# Run unit tests only
mvn test -pl . -Dtest="PolicyServiceTest"
```

---

### 2. Controller Slice Tests

**Location:** `src/test/java/com/insurance/dashboard/controller/`

**Tools:** `@WebMvcTest`, `@MockBean`, MockMvc

**What they test:** HTTP layer — status codes, JSON shape, filter parameter binding.
`PolicyService` is mocked; no database is involved.

| Test | Asserts |
|---|---|
| `getAllPolicies_returnsPaginatedList` | 200 OK, all response fields present |
| `getAllPolicies_withStatusFilter_passesFilterToService` | status param bound and forwarded |
| `getAllPolicies_withRegionFilter_passesFilterToService` | region param bound and forwarded |
| `getAllPolicies_isExpiringSoon_trueWhenEndDateWithin30Days` | `isExpiringSoon` serialised correctly |
| `getAllPolicies_returnsEmptyPage_whenNoPolicies` | empty content array, totalElements = 0 |

```bash
mvn test -pl . -Dtest="PolicyControllerTest"
```

---

### 3. Acceptance Tests

**Location:** `src/test/java/com/insurance/dashboard/acceptance/`

**Tools:** `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Transactional`, real PostgreSQL

**What they test:** Full-stack BDD scenarios against a live database.
Each test method inserts its own data in `@BeforeEach` and rolls back automatically
via `@Transactional` — no test pollution between runs.

#### PolicyListAcceptanceTest (11 tests)

Maps directly to the acceptance criteria defined by the product team:

| BDD Scenario | Test method |
|---|---|
| `?page=0&size=10` returns at most 10 records | `givenPageSize10_thenResponseContainsAtMost10Records` |
| Response includes `totalElements` and `totalPages` | `givenPageSize10_thenResponseIncludesPaginationMetadata` |
| Each policy has all required fields | `givenPoliciesExist_thenEachPolicyHasAllRequiredFields` |
| `holderName` is first + last name | `givenPoliciesExist_thenHolderNameIsFullName` |
| `region` is display name, not enum | `givenPoliciesExist_thenRegionIsDisplayName` |
| `ACTIVE` status → `"Active"` in response | `givenPolicyWithStatusActive_thenFrontendReceivesActive` |
| `LAPSED` status → `"Lapsed"` in response | `givenPolicyWithStatusLapsed_thenFrontendReceivesLapsed` |
| `premium` has `amount` and `currency` | `givenPoliciesExist_thenPremiumHasAmountAndCurrency` |
| Dates are ISO-8601 | `givenPoliciesExist_thenDatesAreIso8601` |
| End date within 30 days → `isExpiringSoon: true` | `givenPolicyEndDateWithin30Days_thenIsExpiringSoonIsTrue` |
| End date beyond 30 days → `isExpiringSoon: false` | `givenPolicyEndDateBeyond30Days_thenIsExpiringSoonIsFalse` |

**Test data strategy:**
- 12 ACTIVE policies (to make pagination meaningful — fills more than one page of size 10)
- 1 LAPSED policy (for status-formatting assertion)
- 1 ACTIVE policy with `endDate = today + 15 days` (for expiry indicator assertion)
- All inserted via JPA repositories in `@BeforeEach`, rolled back after each test

#### PolicyDatabaseFailureAcceptanceTest (2 tests)

| BDD Scenario | Test method |
|---|---|
| DB unreachable → 503 with readable message | `givenDatabaseUnreachable_thenReturns503WithReadableMessage` |
| DB unreachable → no stack trace in response | `givenDatabaseUnreachable_thenResponseDoesNotExposeStackTrace` |

**Approach:** `@WebMvcTest` + `@MockBean PolicyService` throwing
`CannotCreateTransactionException` — the exact exception Spring raises when the
database connection cannot be established. Validates the `GlobalExceptionHandler`
contract without actually bringing down the database.

```bash
mvn test -pl . -Dtest="PolicyListAcceptanceTest,PolicyDatabaseFailureAcceptanceTest"
```

---

### 4. Performance Test

**Location:** `src/test/java/com/insurance/dashboard/performance/PolicyEndpointSimulation.java`

**Tools:** Gatling 3.13 (Java DSL), `gatling-maven-plugin`

**What it tests:** Non-functional requirement — p95 response time under production-level load.

**Load profile:**

| Parameter | Value |
|---|---|
| Concurrent users | 50 (closed model — constant concurrency) |
| Duration | 30 seconds |
| Target endpoint | `GET /api/policies?page=0&size=10` |

**Pass/fail assertions (hard gates):**

| Assertion | Threshold |
|---|---|
| p95 response time | < 500 ms |
| Successful requests | > 99% |

**Observed result (local PostgreSQL):**

| Metric | Value |
|---|---|
| p95 | 169 ms |
| p99 | 215 ms |
| Mean | 115 ms |
| Success rate | 100% |
| Total requests in 30s | 12,589 |

The p95 is 169 ms — 3× below the 500 ms threshold — indicating significant headroom.

---

## Acceptance Criteria Coverage Matrix

| Acceptance Criterion | Test class | Test type |
|---|---|---|
| Paginated response | `PolicyListAcceptanceTest` | Acceptance |
| At most 10 records for size=10 | `PolicyListAcceptanceTest` | Acceptance |
| `totalElements` and `totalPages` present | `PolicyListAcceptanceTest` | Acceptance |
| `policyNumber` in each record | `PolicyListAcceptanceTest` | Acceptance |
| `holderName` as full name | `PolicyListAcceptanceTest` | Acceptance |
| `region` as display name | `PolicyListAcceptanceTest` | Acceptance |
| `status` in title case | `PolicyListAcceptanceTest` | Acceptance |
| `premium.amount` and `premium.currency` | `PolicyListAcceptanceTest` | Acceptance |
| Dates in ISO-8601 format | `PolicyListAcceptanceTest` | Acceptance |
| `isExpiringSoon: true` within 30 days | `PolicyListAcceptanceTest` | Acceptance |
| `isExpiringSoon: false` beyond 30 days | `PolicyListAcceptanceTest` | Acceptance |
| 503 when DB unreachable | `PolicyDatabaseFailureAcceptanceTest` | Acceptance |
| No stack trace in error response | `PolicyDatabaseFailureAcceptanceTest` | Acceptance |
| p95 < 500ms @ 50 concurrent users | `PolicyEndpointSimulation` | Performance |

---

## Test Summary

| Test class | Tests | Type | Database |
|---|---|---|---|
| `PolicyServiceTest` | 5 | Unit | None (mocked) |
| `PolicyControllerTest` | 5 | Controller slice | None (mocked) |
| `PolicyListAcceptanceTest` | 11 | Acceptance | Real PostgreSQL |
| `PolicyDatabaseFailureAcceptanceTest` | 2 | Acceptance | None (mocked exception) |
| **Total** | **23** | | |
| `PolicyEndpointSimulation` | 1 simulation | Performance | Real PostgreSQL |

---

## Test Data Isolation

Acceptance tests use `@Transactional` at the test class level.
Spring wraps each test method in a transaction that is **rolled back** after the method completes.
Data inserted in `@BeforeEach` is visible within the test but leaves no trace in the database.

No cleanup scripts, no test-specific database, no `@DirtiesContext` needed.

The existing seed data from `data.sql` persists in the database between test runs
and is present alongside the test data during acceptance tests.
Tests use unique policy number prefixes (`ACC-*`) and JSONPath filters to target
their own records without being affected by seed data.

---

## Dependency Notes

- `@MockBean` creates mocks of interfaces in Spring Test — `PolicyService` (interface) is
  mocked cleanly in `PolicyControllerTest` and `PolicyDatabaseFailureAcceptanceTest`.
- `PolicyServiceImpl` is instantiated directly with `new PolicyServiceImpl(...)` in
  `PolicyServiceTest` — no Spring context overhead, fastest feedback loop.
- `PolicyMapperImpl` is used as the real implementation in `PolicyServiceTest` so that
  mapping assertions (holderName, region display name, status casing) remain meaningful.
