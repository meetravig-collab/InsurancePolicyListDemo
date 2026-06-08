# Testing — Policy Overview Dashboard

The hexagonal design makes each layer independently testable: the service is tested against
a mocked **port**, the HTTP layer against the **real mapper**, and the full stack against
**real PostgreSQL**. A Gatling load test gates performance.

```
┌──────────────────────────────────────────────┐
│  Performance (Gatling)   p95 < 300ms @ 50 sessions (list + detail)
├──────────────────────────────────────────────┤
│  Acceptance (SpringBootTest + PostgreSQL)     full stack, real DB, auto-rollback
├──────────────────────────────────────────────┤
│  Controller slice (WebMvcTest + real mapper)  HTTP contract, mapping, errors
├──────────────────────────────────────────────┤
│  Service unit (Mockito on the port)           use-case logic, no Spring, no DB
│  Caching (SpringBootTest, port mocked)        cache hits + invalidation
└──────────────────────────────────────────────┘
```

## Running

```bash
mvn test                      # all 34 tests (needs PostgreSQL running)
mvn test -Dtest=PolicyServiceTest
mvn spring-boot:run && mvn gatling:test    # performance (app must be running)
```
> Windows behind a TLS proxy: `set MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`.

## 1. Service unit — `PolicyServiceTest` (5)
JUnit 5 + Mockito; `PolicyServiceImpl` built directly with the **`PolicyRepositoryPort`
mocked** — no Spring, no DB. Asserts on domain types (`Policy`, `PageResult`, `PolicySummary`).

| Test | Verifies |
|---|---|
| `getPolicies_delegatesToPortAndReturnsResult` | filter + page forwarded; result returned |
| `getPolicyById_returnsPolicy_whenFound` | domain `Policy` returned |
| `getPolicyById_throwsNotFound_whenMissing` | `PolicyNotFoundException` on empty `Optional` |
| `flagPoliciesForReview_returnsUpdatedCount` | delegates to `port.flagForReview` |
| `getSummary_formatsAggregatesFromPort` | title-case status, display-name LOB (`A&H`), expiring count |

## 2. Caching — `PolicyCachingTest` (3)
`@SpringBootTest` (cache proxies active) with the port mocked; asserts port call counts.

| Test | Verifies |
|---|---|
| `listings_areCached_soSecondCallSkipsThePort` | 2nd identical list call served from cache |
| `summary_isCached_soSecondCallSkipsThePort` | 2nd summary call served from cache |
| `flagging_evictsListingsCache_soNextListHitsThePortAgain` | flag evicts listings cache |

## 3. Controller slice — `PolicyControllerTest` (7)
`@WebMvcTest` + `@Import(PolicyMapperImpl)` + `@MockBean PolicyService`. The **real mapper**
verifies domain→DTO + JSON; filter binding checked with an `ArgumentCaptor<PolicyFilter>`.
Covers: list payload & fields, status/region filter capture, get-by-id, flag (200),
flag empty → 400, summary.

## 4. Acceptance (full stack) — real PostgreSQL, `@Transactional` rollback
Run controller → service → port → adapter → JPA → PostgreSQL and back. Each test seeds its
own rows via `PolicyJpaRepository`/`PolicyEntity` and rolls back.

**`PolicyListAcceptanceTest` (17)** — pagination & metadata, sort, all schema fields,
status formatting (`Active`/`Cancelled`), LOB display name, region & effective-date filters,
search on name & underwriter, get-by-id, 404, expiring-soon, bulk flag + verify, empty-flag 400, summary.

**`PolicyDatabaseFailureAcceptanceTest` (2)** — `@WebMvcTest` with the service stubbed to throw
`CannotCreateTransactionException`: returns `503` with a readable message and **no** stack trace.

## 5. Performance — `PolicyEndpointSimulation` (Gatling)
Each session lists policies, captures a real UUID, then fetches that policy's detail — so
**both** read endpoints are measured.

| Profile | Value |
|---|---|
| Load | 50 concurrent sessions (closed model), 30s |
| Endpoints | `GET /api/v1/policies` and `GET /api/v1/policies/{id}` |
| Hard gates | per-endpoint p95 < 300ms; success > 99% |

**Observed (local PostgreSQL, ~17k requests, 0 errors):** list p95 158ms, detail p95 83ms — both well under target.

## Summary

| Test class | Tests | Type | Database |
|---|---|---|---|
| `PolicyServiceTest` | 5 | Service unit (port mocked) | None |
| `PolicyCachingTest` | 3 | Caching (Spring ctx, port mocked) | None |
| `PolicyControllerTest` | 7 | Controller slice (real mapper) | None |
| `PolicyListAcceptanceTest` | 17 | Acceptance (full stack) | Real PostgreSQL |
| `PolicyDatabaseFailureAcceptanceTest` | 2 | Acceptance (mocked failure) | None |
| **Total** | **34** | | |
| `PolicyEndpointSimulation` | 1 sim | Performance | Real PostgreSQL |

## Test data isolation
Acceptance tests are `@Transactional`, so each method's inserts roll back automatically — no
cleanup scripts, no `@DirtiesContext`. Tests target their own rows with `ACC-*` prefixes and
JSONPath filters, and scope magnitude-sensitive assertions (e.g. the premium-sort test
searches `ACC-0`) so the 220-row seed can't interfere.
