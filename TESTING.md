# Testing Guide — Policy Overview Dashboard

## Test Strategy

The suite is layered to give fast feedback on logic while validating full-stack behaviour
and the performance NFR. The hexagonal architecture makes the layers independently
testable: the service is tested against a mocked **port**, and the persistence adapter is
exercised through the real Spring context.

```
┌──────────────────────────────────────────────┐
│  Performance (Gatling)                        │  NFR gate: p95 < 300ms @ 50 sessions (list + detail)
├──────────────────────────────────────────────┤
│  Acceptance (SpringBootTest + PostgreSQL)     │  full stack, real DB, auto-rollback
├──────────────────────────────────────────────┤
│  Controller slice (WebMvcTest + real mapper)  │  HTTP contract, mapping, errors
├──────────────────────────────────────────────┤
│  Service unit (Mockito on the port)           │  use-case logic, no Spring, no DB
└──────────────────────────────────────────────┘
```

---

## Running the Tests

```bash
# All unit + slice + acceptance tests
mvn test

# A single class
mvn test -Dtest=PolicyServiceTest

# Performance (app must be running)
mvn spring-boot:run            # terminal 1
mvn gatling:test               # terminal 2  (reports in target/gatling/)
```

> On this Windows dev machine the TLS-intercepting proxy requires
> `set MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` before `mvn` so dependency
> downloads verify against the Windows certificate store.

---

## 1. Service Unit Tests

**`src/test/java/.../service/PolicyServiceTest.java`** — JUnit 5 + Mockito.

`PolicyServiceImpl` is built directly (`new PolicyServiceImpl(port, 30)`) with the
**`PolicyRepositoryPort` mocked**. No Spring context, no database — the fastest loop.
Because the service now deals in domain types, the tests assert on `Policy` / `PageResult`
/ `PolicySummary`, not DTOs.

| Test | Asserts |
|---|---|
| `getPolicies_delegatesToPortAndReturnsResult` | filter + page forwarded to the port; `PageResult<Policy>` returned |
| `getPolicyById_returnsPolicy_whenFound` | domain `Policy` returned |
| `getPolicyById_throwsNotFound_whenMissing` | `PolicyNotFoundException` on empty `Optional` |
| `flagPoliciesForReview_returnsUpdatedCount` | delegates to `port.flagForReview`, returns count |
| `getSummary_formatsAggregatesFromPort` | enum→title-case status, enum→display-name LOB (e.g. `A&H`), expiring-soon count |

### Caching tests — PolicyCachingTest (3 tests)

**`src/test/java/.../service/PolicyCachingTest.java`** — `@SpringBootTest` (so the
`@Cacheable`/`@CacheEvict` proxies are active) with the persistence **port mocked**, then
asserts how often the port is actually hit.

| Test | Asserts |
|---|---|
| `listings_areCached_soSecondCallSkipsThePort` | two identical `getPolicies` calls → port hit once |
| `summary_isCached_soSecondCallSkipsThePort` | two `getSummary` calls → port hit once |
| `flagging_evictsListingsCache_soNextListHitsThePortAgain` | after `flagPoliciesForReview`, the listings cache is evicted → port hit again |

---

## 2. Controller Slice Tests

**`src/test/java/.../controller/PolicyControllerTest.java`** — `@WebMvcTest` +
`@Import(PolicyMapperImpl.class)` + `@MockBean PolicyService`.

The **real mapper** is imported so JSON serialization and domain→DTO mapping are actually
verified; the service is mocked and stubbed with **domain objects** (`Policy`,
`PageResult<Policy>`, `PolicySummary`). Filter binding is checked with an
`ArgumentCaptor<PolicyFilter>`.

| Test | Asserts |
|---|---|
| `getPolicies_returnsPaginatedList` | 200; all DTO fields incl. `policyholderName`, `lineOfBusiness`, `premiumAmount`, `currency`, `flaggedForReview` |
| `getPolicies_withStatusFilter_passesFilterToService` | captured `PolicyFilter.status() == ACTIVE` |
| `getPolicies_withRegionFilter_passesFilterToService` | captured `PolicyFilter.region() == JAPAN` |
| `getPolicyById_returnsPolicy` | mapper output serialized for one policy |
| `flagPoliciesForReview_returnsFlaggedCount` | `flaggedCount` + `policyIds` echoed |
| `flagPoliciesForReview_returns400_whenPolicyIdsEmpty` | `@NotEmpty` → 400 |
| `getSummaryStats_returnsAggregatedStats` | `countsByStatus`, `expiringSoonCount` |

---

## 3. Acceptance Tests (full stack, real PostgreSQL)

**`src/test/java/.../acceptance/`** — `@SpringBootTest` + `@AutoConfigureMockMvc` +
`@Transactional`.

These run through the **entire hexagon**: controller → service → port →
`PolicyPersistenceAdapter` → JPA → PostgreSQL, and back through `PolicyEntityMapper` and
`PolicyMapper`. Each method seeds its own data in `@BeforeEach` via the
`PolicyJpaRepository` (building `PolicyEntity` rows) and is rolled back by `@Transactional`.

### PolicyListAcceptanceTest (17 tests)

| Area | Test methods |
|---|---|
| Pagination | `givenPageSize10_thenAtMost10Records`, `givenPageSize10_thenPaginationMetadataPresent` |
| Sorting | `givenSortByPremiumDesc_thenOrdered` |
| Field completeness | `givenPoliciesExist_thenAllFieldsPresent` (incl. `lineOfBusiness`, `underwriter`, `createdAt`) |
| Status formatting | `givenStatusActive_thenRendersActive`, `givenStatusCancelled_thenRendersCancelled` |
| LOB display name | `givenPolicies_thenLineOfBusinessIsDisplayName` |
| Filters | `givenRegionFilter_thenMatchingReturned`, `givenEffectiveDateRange_thenMatchingReturned` |
| Free-text search | `givenSearchOnName_thenMatchingReturned`, `givenSearchOnUnderwriter_thenMatchingReturned` |
| Get by id | `givenValidId_thenSinglePolicyReturned`, `givenUnknownId_thenReturns404` |
| Expiry indicator | `givenExpiryWithin30Days_thenExpiringSoonTrue` |
| Bulk flag | `givenIds_whenFlagged_thenFlaggedTrue`, `givenEmptyIds_thenReturns400` |
| Summary | `getSummary_returnsAggregatedData` |

**Test data:** 12 `ACTIVE`/`PROPERTY` policies (`ACC-000001…`), one `CANCELLED`/`CASUALTY`
(`ACC-CANCELLED-1`), one `ACTIVE`/`MARINE` expiring in 15 days (`ACC-EXPIRING-1`).

### PolicyDatabaseFailureAcceptanceTest (2 tests)

`@WebMvcTest` + `@Import(PolicyMapperImpl.class)` + `@MockBean PolicyService` stubbed to
throw `CannotCreateTransactionException` (what Spring raises when no DB connection can be
acquired) — validates the 503 contract without taking the database down.

| Test | Asserts |
|---|---|
| `givenDatabaseUnreachable_thenReturns503WithReadableMessage` | 503, readable message, timestamp |
| `givenDatabaseUnreachable_thenResponseDoesNotExposeStackTrace` | no `trace`/`exception`/`stackTrace`, no class names |

---

## 4. Performance Test

**`src/test/java/.../performance/PolicyEndpointSimulation.java`** — Gatling (Java DSL),
run via `mvn gatling:test` against a running app.

Each virtual session hits the **list** endpoint, captures a real policy UUID from the
page, then fetches that policy's **detail** — so both read endpoints are measured under load.

| Profile | Value |
|---|---|
| Load | 50 concurrent sessions (closed model) |
| Duration | 30 s |
| Endpoints | `GET /api/v1/policies` (list) and `GET /api/v1/policies/{id}` (detail) |

**Hard gates (per requirement):** p95 < 300 ms **for both list and detail**; success rate > 99%.
The assertions are per-endpoint (`details("GET list")` / `details("GET detail")`), not just global.

**Observed (local PostgreSQL, 50 concurrent sessions, 30s, 17,770 requests, 0 errors):**

| Endpoint | p95 | Target |
|---|---|---|
| GET list | 158 ms | < 300 ms ✅ |
| GET detail | 83 ms | < 300 ms ✅ |
| Success rate | 100% | > 99% ✅ |

---

## Test Summary

| Test class | Tests | Type | Database |
|---|---|---|---|
| `PolicyServiceTest` | 5 | Service unit (port mocked) | None |
| `PolicyCachingTest` | 3 | Caching (Spring context, port mocked) | None |
| `PolicyControllerTest` | 7 | Controller slice (real mapper, service mocked) | None |
| `PolicyListAcceptanceTest` | 17 | Acceptance (full stack) | Real PostgreSQL |
| `PolicyDatabaseFailureAcceptanceTest` | 2 | Acceptance (mocked failure) | None |
| **Total** | **34** | | |
| `PolicyEndpointSimulation` | 1 simulation | Performance | Real PostgreSQL |

---

## How the layering shapes the tests

- **Service tests mock the domain port**, not Spring Data. Swapping persistence technology
  would not touch a single service test — proof the dependency points inward.
- **Controller tests import the real `PolicyMapperImpl`** and stub the service with domain
  objects, so both the HTTP contract and the domain→DTO mapping are verified together.
- **Acceptance tests seed through `PolicyJpaRepository` / `PolicyEntity`** (the infrastructure
  adapter's own types), exercising the entity↔domain mapping end-to-end.

## Test data isolation

Acceptance tests are `@Transactional`, so each method's inserts roll back automatically — no
cleanup scripts, no `@DirtiesContext`. The 220-row `data.sql` seed (loaded manually via
`psql` for the running app) may coexist in the DB; tests target their own rows with `ACC-*`
prefixes and JSONPath filters, and scope magnitude-sensitive assertions (e.g. the
premium-sort test searches `ACC-0`) so seed data cannot interfere.
