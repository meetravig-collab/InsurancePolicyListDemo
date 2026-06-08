# Testing — Policy Overview Dashboard

The hexagonal design makes each layer independently testable: the service against a mocked
**port**, the HTTP layer against the **real mapper**, and the full stack against a
**Testcontainers PostgreSQL** (a throwaway container — no manually-provisioned DB). A
Gatling load test gates performance, and CI runs the whole suite with coverage.

```
┌──────────────────────────────────────────────┐
│  Performance (Gatling)   p95 < 300ms @ 50 sessions (list + detail)
├──────────────────────────────────────────────┤
│  Acceptance (SpringBootTest + Testcontainers) full stack, real Postgres, auto-rollback
├──────────────────────────────────────────────┤
│  Controller slice (WebMvcTest + real mapper)  HTTP contract, mapping, errors
├──────────────────────────────────────────────┤
│  Service unit (Mockito on the port)           use-case logic, no Spring, no DB
│  Caching (SpringBootTest, port mocked)        cache hits + invalidation
└──────────────────────────────────────────────┘
```

## Running

```bash
mvn verify                    # all 38 tests + JaCoCo coverage report (target/site/jacoco)
mvn test -Dtest=PolicyServiceTest
mvn spring-boot:run && mvn gatling:test    # performance (app must be running)
```
- **Docker must be running** — the acceptance/caching tests start a PostgreSQL container via
  Testcontainers (no local DB setup needed). On CI (Linux) this is auto-detected.
- Windows + Docker Desktop: Testcontainers needs `DOCKER_HOST` pointed at the Docker Desktop
  pipe (`npipe:////./pipe/dockerDesktopLinuxEngine`) or a `~/.testcontainers.properties` with
  `docker.host=...`. CI needs none of this.
- Windows behind a TLS proxy: `set MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`.

## CI

`.github/workflows/docker-publish.yml` runs on every push and PR to `master`/`dev`:
1. **test** job — `mvn verify` on an Ubuntu runner (Testcontainers spins up PostgreSQL),
   uploads the JaCoCo report as an artifact.
2. **build-and-push** job — `needs: test`, runs only on push: the GHCR image is **published
   only when the suite passes**, so a broken commit can't ship an image.

## 1. Service unit — `PolicyServiceTest` (5)
JUnit 5 + Mockito; `PolicyServiceImpl` built directly with the **`PolicyRepositoryPort`
mocked** — no Spring, no DB. Asserts on domain types (`Policy`, `PageResult`, `PolicySummary`).

| Test | Verifies |
|---|---|
| `getPolicies_delegatesToPortAndReturnsResult` | filter + page forwarded; result returned |
| `getPolicyById_returnsPolicy_whenFound` | domain `Policy` returned |
| `getPolicyById_throwsNotFound_whenMissing` | `PolicyNotFoundException` on empty `Optional` |
| `flagPoliciesForReview_returnsUpdatedCount` | delegates to `port.flagForReview` |
| `getSummary_passesThroughEnumKeyedAggregatesFromPort` | service returns enum-keyed aggregates unchanged (formatting is the mapper's job) |

## 2. Caching — `PolicyCachingTest` (3)
`@SpringBootTest` (cache proxies active, Testcontainers Postgres) with the port mocked;
asserts port call counts.

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

## 4. Acceptance (full stack) — Testcontainers PostgreSQL, `@Transactional` rollback
Run controller → service → port → adapter → JPA → PostgreSQL and back, against a container
started by `AbstractPostgresIT` (Flyway builds + seeds it). Each test seeds its own rows via
`PolicyJpaRepository`/`PolicyEntity` and rolls back.

**`PolicyListAcceptanceTest` (21)** — pagination & metadata, sort, all schema fields, status
formatting (`Active`/`Cancelled`), LOB display name, region & effective-date filters, search
on name & underwriter, get-by-id, 404, expiring-soon, bulk flag + verify, empty-flag 400,
summary. **Edge/negative cases:** invalid enum param → 400, malformed UUID → 400 (not 500),
no-match filters → empty page (200), flag unknown id → no-op (`flaggedCount` 0).

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

**Observed (~15k requests, 0 errors):** list p95 ~170ms, detail p95 ~90ms — both well under target.

## Summary

| Test class | Tests | Type | Database |
|---|---|---|---|
| `PolicyServiceTest` | 5 | Service unit (port mocked) | None |
| `PolicyCachingTest` | 3 | Caching (Spring ctx, port mocked) | Testcontainers |
| `PolicyControllerTest` | 7 | Controller slice (real mapper) | None |
| `PolicyListAcceptanceTest` | 21 | Acceptance (full stack) | Testcontainers |
| `PolicyDatabaseFailureAcceptanceTest` | 2 | Acceptance (mocked failure) | None |
| **Total** | **38** | | |
| `PolicyEndpointSimulation` | 1 sim | Performance | Real PostgreSQL |

Coverage: JaCoCo runs on `mvn verify` → `target/site/jacoco/index.html` (uploaded as a CI
artifact). **Actual: 92.5% line, 93.9% instruction** (Lombok-generated code and the bootstrap
class excluded). **Gated** at `mvn verify`: bundle **line ≥ 80%** and **instruction ≥ 85%** —
the build fails below either.

## Test data isolation
Acceptance tests are `@Transactional`, so each method's inserts roll back automatically — no
cleanup scripts, no `@DirtiesContext`. Tests target their own rows with `ACC-*` prefixes and
JSONPath filters, and scope magnitude-sensitive assertions (e.g. the premium-sort test
searches `ACC-0`) so the Flyway-seeded 220 rows can't interfere.
