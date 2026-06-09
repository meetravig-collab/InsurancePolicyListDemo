# AI Working Journal

Running notes on how this codebase was built with AI assistance. Captures what I (the AI)
**accepted**, **challenged**, **overrode**, and **corrected**, with brief reasoning. Not
polished — a process log committed alongside the code. Newest themes roughly last.

Legend: ✅ accepted · ⚠️ challenged / pushed back · ↪️ overrode the literal request · ✏️ corrected myself.

---

## Build-out & requirements

- ✅ Built the Spring Boot policy service and the acceptance criteria as given (paginated
  list, get-by-id, bulk flag, summary; status title-cased "Active" not "ACTIVE"; region/LOB
  display names; `isExpiringSoon`; ISO dates; 503 with no stack trace; p95 gate).
- ↪️ **`effectiveDate` mapping** — the contract named an `effectiveDate` range filter, but the
  response contract already exposed `startDate`/`endDate`. Rather than guess, I asked the user
  (it changes a locked contract). Decision: keep `startDate`/`endDate` in the response, map the
  `effectiveDateFrom/To` filter onto the start (effective) date.
- ↪️ **Schema replacement** — when the canonical schema arrived (UUID id, `policyholderName`,
  new enums, 8 regions, `underwriter`, timestamps), the old seed/DB rows used old enum values
  that would break enum mapping. Confirmed with the user, then reset + reseeded rather than
  silently leaving broken data.

## SOLID / Clean Architecture

- ⚠️ I proactively flagged three layering concerns instead of rubber-stamping a "looks SOLID":
  1. service depended on the `api` layer (DTOs + mapper) — an outward dependency. Fixed: service
     deals in domain types; mapping moved to the controller/mapper.
  2. service depended on infrastructure / Spring Data — fixed with a domain-owned
     `PolicyRepositoryPort` + an infrastructure adapter (hexagonal).
  3. domain model was a JPA entity — split into a POJO `Policy` + `PolicyEntity` in infra.
- ✅ User flagged the `static toPageQuery` in the controller (SRP / "fundamental red flag").
  Agreed it was wrong; it was moved to the injected `PolicyMapper` (no static, no second
  responsibility in the controller).
- ✅ User flagged `GlobalExceptionHandler` sitting in `common` ("it's API-related"). Agreed and
  moved it to `api/exception`; `ErrorResponse` to `api/exception`; `PolicyNotFoundException` to
  `domain/exception`; deleted the `common` package.
- ✅ Moved `PolicySpecification` into a `specification` sub-package and refactored the filter
  if-chain into one composable `Specification` per criterion (`Specification.allOf`) for OCP.
- ⚠️ **DRY/SRP self-review** — when asked to "confirm SOLID etc.", I did NOT just confirm. A grep
  found `toTitleCase` duplicated in the service and the mapper, and the service was doing
  presentation formatting (a layer smell). Fixed: `PolicySummary` is enum-keyed, the service
  passes aggregates through, the mapper does all display formatting (single `toTitleCase`).

## Contract-first

- ↪️ "Put openapi.yaml in the resources folder so Swagger picks it up" — put it in
  `resources/static/` (not just `resources/`), because only `static/` is web-served, and wired
  `springdoc.swagger-ui.url=/openapi.yaml`. Explained the distinction.
- ✅ Removed the Postman collection once Swagger served the committed contract (user said it
  wouldn't be needed).

## Schema management

- ⚠️ When asked about deployment safety, I flagged `ddl-auto=update` as a real risk (silent
  schema drift). When the user clarified it was for a demo, I scaled back ("fine for the demo")
  rather than over-engineering — then later, on request, added **Flyway** (V1 schema captured
  from the Hibernate DDL so `ddl-auto=validate` matches; V2 seed) properly.

## Testing & coverage

- ⚠️ "Have to write load tests" — pointed out load testing was already implemented (Gatling),
  and that a **stale k6 `load-test.js`** existed with the old path and `p95<500`. Recommended
  removing the stale duplicate rather than keeping two conflicting load tests.
- ✅ When asked for production-grade testing, I assessed honestly and named the real gaps:
  CI wasn't running tests, tests weren't hermetic (manual local PostgreSQL), no coverage.
  Implemented Testcontainers + a CI test gate (publish only if tests pass) + JaCoCo + edge tests.
- ↪️ **Coverage gate sequencing** — the headline JaCoCo number was 46% (instruction), but that
  was Lombok-generated boilerplate. Rather than claim 46% or gate something I couldn't verify,
  I (a) excluded Lombok via `@Generated`, (b) gated **line ≥ 80%** first (safe, ~92%), then
  (c) added an **instruction ≥ 85%** gate only after CI *measured* it at 93.9%.
- ↪️ **Did not gate branch coverage (65.9%)** — it's dominated by Java `record` equals/hashCode
  that JaCoCo can't auto-exclude; gating it would chase a metric artifact, not real risk. Stated
  this openly as a deliberate trade-off.
- ✏️ **Corrected my own caveat.** I had written "JPA enums live on the domain POJO." On
  validation that was false — the domain has zero JPA annotations/imports; JPA mapping lives only
  on `PolicyEntity`. Lombok is compile-time only. So no real loophole there; I retracted the claim.

## Performance

- ✅ Tightened the gate from p95<500 (list only) to **p95<300 for list AND detail**, asserted
  per-endpoint in Gatling so the build fails on regression. Verified live (~150ms / ~80ms).

## Infrastructure / CI / supply chain

- ↪️ **Docker local build** — the committed multi-stage Dockerfile failed locally because the
  in-container Maven hit this machine's TLS-intercepting proxy. I deliberately did NOT bake a
  corporate cert into the repo Dockerfile (would pollute CI). Added `Dockerfile.local` that copies
  a host-built jar instead, and kept the multi-stage Dockerfile for CI/GHCR.
- ↪️ **Docker image "push to GitHub"** — clarified that images go to GHCR (not the git repo) and
  set up a GitHub Actions workflow to build+publish, since a local push needs auth/Docker that
  the environment lacked. Later installed Docker locally on request and proved the local build
  via the host-jar path.
- ⚠️ **Dependabot major-version PRs** — Dependabot opened Spring Boot 3→4 and JDK 17→25/26 base
  image bumps. I explicitly recommended *not* auto-merging these (breaking) and only merging the
  low-risk ones after green checks.
- ✅ Hardened CI: Node-24 action versions, then **pinned all actions to commit SHAs**, added
  Dependabot config, and enabled Dependabot alerts + automated security fixes.

## Branching

- ✅ Created `dev`, made it the default branch, and updated the workflow to publish on
  `master`/`dev`. Kept `master` fast-forwarded to `dev` so they stay identical.

## k6 (re-added on request)

- ✅ Re-added `k6/load-test.js` (v1 endpoints, p95<300 thresholds, 50 VUs) as a lightweight
  alternative to Gatling.
- ⚠️ **Honest verification limit** — verified the script is correct at 1 VU (list+detail 200,
  ~18ms, checks pass), but the full 50-VU run is blocked on this Windows machine by a local
  proxy/firewall quirk that throttles many simultaneous loopback connections (the app + Gatling
  do 50 VUs fine here). Did not claim a 50-VU pass I couldn't produce; noted Gatling remains the
  in-build/CI-runnable gate.

## Recurring stances

- I verified claims with `grep`/import scans / CI runs rather than asserting (e.g. "domain has
  zero framework imports", "service has zero infrastructure imports", coverage numbers).
- I kept `dev` and `master` in sync and confirmed remote state after each change.
- Where the environment (TLS proxy, Windows Docker/Testcontainers, k6 concurrency) blocked local
  verification, I said so plainly and used CI as the authoritative check instead of overstating.
