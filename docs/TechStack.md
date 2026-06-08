# Tech Stack — Policy Overview Dashboard

## Runtime & language
| Technology | Version | Why |
|---|---|---|
| Java | 17 | LTS; baseline for Spring Boot 3.x |
| Spring Boot | 3.3.0 | Application framework, auto-config, embedded Tomcat |
| Maven | 3.9+ | Build & dependency management |

## Web & API
| Technology | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controllers, JSON (Jackson), embedded Tomcat |
| `spring-boot-starter-validation` | Request validation (JSR-380, e.g. `@NotEmpty`) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI + runtime OpenAPI docs (`/swagger-ui`, `/v3/api-docs`) |
| `openapi.yaml` | Contract-first source of truth for the API shape |

## Persistence
| Technology | Purpose |
|---|---|
| `spring-boot-starter-data-jpa` | ORM (Hibernate), repositories, `Specification` filtering |
| PostgreSQL (13+) | Relational store; UUID keys via `gen_random_uuid()` |
| `postgresql` JDBC driver | Database connectivity |

## Caching
| Technology | Purpose |
|---|---|
| `spring-boot-starter-cache` | Spring Cache abstraction (`@Cacheable` / `@CacheEvict`) |
| Caffeine | In-memory cache provider (TTL + max-size LRU); swappable for Redis |

## Productivity
| Technology | Purpose |
|---|---|
| Lombok | `@Slf4j`, `@Builder`, `@Data`, `@RequiredArgsConstructor` |
| `spring-boot-devtools` | Auto-restart during development |

## Testing
| Technology | Purpose |
|---|---|
| JUnit 5 + Mockito | Unit tests (service against a mocked port) |
| Spring Test / MockMvc | Controller slice & full-stack acceptance tests |
| Gatling (Java DSL) | Load testing — p95 gate for list & detail endpoints |

## Build, container & CI/CD
| Technology | Purpose |
|---|---|
| Docker (multi-stage `Dockerfile`) | Build the jar in-container → slim JRE runtime image |
| `Dockerfile.local` | Runtime image from a host-built jar (for behind a TLS proxy) |
| `docker-compose.yml` | App + PostgreSQL for local one-command run |
| GitHub Actions (`docker-publish.yml`) | Build & publish image to GHCR on push to `master`/`dev` |
| GitHub Container Registry (GHCR) | Image registry: `ghcr.io/meetravig-collab/insurancepolicylistdemo` |
| Dependabot | Weekly update PRs for Maven, Docker base images, and Actions |

### CI hardening notes
- Workflow actions are **pinned to commit SHAs** (with version comments) to prevent a moved/compromised tag from injecting code.
- Actions run on **Node 24** versions (checkout v6, docker login v4 / metadata v6 / build-push v7).
- Dependabot **alerts** and **automated security fixes** are enabled on the repository.
