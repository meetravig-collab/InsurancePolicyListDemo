# Development Guide — Policy Overview Dashboard

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java | 17+ | Required by Spring Boot 3.3 |
| Maven | 3.9+ | Bundled wrapper not included; use local install |
| PostgreSQL | 16 | Local instance; database `insuranceDB` must exist |

---

## Initial Setup

### 1. Create the database

```sql
CREATE DATABASE "insuranceDB";
```

Connect as the `postgres` user (default password: `postgres`).
The schema is created automatically by Hibernate on first run (`ddl-auto=update`).

### 2. Seed data (optional)

`src/main/resources/data.sql` is loaded on startup and inserts sample policy holders
and policies. Remove or modify this file to start with an empty dataset.

### 3. Configure the environment (if overriding defaults)

The application reads the following environment variables.
Defaults work for a standard local PostgreSQL install — only set these if your setup differs.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/insuranceDB` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `PORT` | `8081` | HTTP server port |
| `POLICY_EXPIRY_WARNING_DAYS` | `30` | Days before end date to flag `isExpiringSoon` |

Set them in your shell before running:

```bash
export DB_PASSWORD=mysecretpassword
export POLICY_EXPIRY_WARNING_DAYS=45
```

---

## Build

```bash
# Compile and run all tests
mvn test

# Package as a JAR (skip tests)
mvn package -DskipTests

# Package and run all tests
mvn package
```

The built JAR is at `target/insurance-policy-holders-dashboard-0.0.1-SNAPSHOT.jar`.

---

## Run

### Option A — Maven plugin (development)

```bash
mvn spring-boot:run
```

Spring DevTools is included — the application restarts automatically on class changes.

### Option B — JAR (production-like)

```bash
java -jar target/insurance-policy-holders-dashboard-0.0.1-SNAPSHOT.jar
```

Override environment variables inline:

```bash
DB_PASSWORD=secret PORT=9090 java -jar target/*.jar
```

### Verify startup

```
Tomcat started on port 8081 (http) with context path '/'
Started PolicyOverviewDashboardApplication in X.XXX seconds
```

---

## API Reference

### `GET /api/policies`

Returns a paginated, optionally filtered list of insurance policies.

**Base URL:** `http://localhost:8081`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `status` | string | No | all | Filter by policy status. Allowed: `ACTIVE`, `INACTIVE`, `EXPIRED`, `PENDING`, `LAPSED` |
| `region` | string | No | all | Filter by region. Allowed: `SINGAPORE`, `HONG_KONG`, `AUSTRALIA`, `INDIA`, `JAPAN` |
| `page` | integer | No | `0` | Zero-based page index |
| `size` | integer | No | `10` | Records per page |
| `sort` | string | No | `startDate,desc` | Sort field and direction (e.g. `policyNumber,asc`) |

#### Example Requests

```bash
# All policies, default pagination
curl http://localhost:8081/api/policies

# Active policies only
curl "http://localhost:8081/api/policies?status=ACTIVE"

# Active policies in Singapore, page 2
curl "http://localhost:8081/api/policies?status=ACTIVE&region=SINGAPORE&page=1&size=10"

# Policies expiring soonest first
curl "http://localhost:8081/api/policies?status=ACTIVE&sort=endDate,asc"
```

#### Response — 200 OK

```json
{
  "content": [
    {
      "id": 11,
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
  "number": 0,
  "first": true,
  "last": true
}
```

#### Response field reference

| Field | Type | Description |
|---|---|---|
| `id` | Long | Internal policy identifier |
| `policyNumber` | String | Business policy number |
| `holderName` | String | Full name: `firstName + " " + lastName` |
| `region` | String | Human-readable region name (e.g. `"Hong Kong"`) |
| `status` | String | Title-cased status (e.g. `"Active"`, `"Lapsed"`) |
| `premium.amount` | Decimal | Monthly premium amount |
| `premium.currency` | String | ISO currency code (e.g. `"SGD"`, `"USD"`) |
| `startDate` | String | Policy start date (ISO-8601: `yyyy-MM-dd`) |
| `endDate` | String | Policy end date (ISO-8601: `yyyy-MM-dd`) |
| `isExpiringSoon` | Boolean | `true` if end date is within the configured warning window |

#### Response — 503 Service Unavailable (database unreachable)

```json
{
  "timestamp": "2026-06-07T10:30:00",
  "status": 503,
  "message": "Policy service is temporarily unavailable. Please try again later."
}
```

No stack traces or internal exception class names are included in error responses.

---

## Postman Collection

Import `PolicyOverviewDashboard.postman_collection.json` from the project root into Postman.

The collection includes 8 pre-built requests covering all filter and pagination combinations.
The `{{baseUrl}}` variable is set to `http://localhost:8081` — update it once to target
any environment.

---

## Logging

Log levels follow SLF4J via Logback (Spring Boot default).

| Logger | Default level | What it logs |
|---|---|---|
| `PolicyController` | `DEBUG` | Incoming request parameters |
| `PolicyServiceImpl` | `DEBUG` | Filter + page applied |
| `PolicyMapperImpl` | `DEBUG` / `WARN` | Per-record mapping; warns on null status |
| `GlobalExceptionHandler` | `ERROR` | Data access failures with full stack trace |

**Enable debug logging for this application only:**

```bash
java -jar target/*.jar --logging.level.com.insurance.dashboard=DEBUG
```

**Or in `application.properties`:**

```properties
logging.level.com.insurance.dashboard=DEBUG
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/insurance/dashboard/
│   │   ├── api/
│   │   │   ├── controller/     PolicyController
│   │   │   ├── dto/response/   PolicySummaryResponse
│   │   │   └── mapper/         PolicyMapper (interface), PolicyMapperImpl
│   │   ├── common/exception/   GlobalExceptionHandler, ErrorResponse
│   │   ├── domain/model/       Policy, PolicyHolder
│   │   ├── infrastructure/
│   │   │   └── persistence/
│   │   │       └── repository/ PolicyRepository, PolicyHolderRepository
│   │   ├── service/            PolicyService (interface), PolicyServiceImpl
│   │   └── PolicyOverviewDashboardApplication.java
│   └── resources/
│       ├── application.properties
│       └── data.sql            (sample data, loaded on startup)
├── test/
│   └── java/com/insurance/dashboard/
│       ├── acceptance/         PolicyListAcceptanceTest, PolicyDatabaseFailureAcceptanceTest
│       ├── controller/         PolicyControllerTest
│       ├── performance/        PolicyEndpointSimulation (Gatling)
│       └── service/            PolicyServiceTest
└── gatling/ (reports generated here after mvn gatling:test)
```

---

## Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API, embedded Tomcat |
| `spring-boot-starter-data-jpa` | JPA / Hibernate ORM |
| `spring-boot-starter-validation` | Bean Validation (JSR-380) |
| `postgresql` | PostgreSQL JDBC driver |
| `spring-boot-devtools` | Auto-restart in development |
| `lombok` | `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`, `@Data` |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc, Spring Test |
| `gatling-charts-highcharts` | Load testing (test scope) |
