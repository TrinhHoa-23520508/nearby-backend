# Nearby backend

Backend for the hyperlocal second-hand marketplace. It is a Spring Boot 4.1 / Java 25 **modular monolith** (Spring Modulith) built on PostgreSQL + PostGIS, Redis, and MinIO.

- **Local setup:** [docs/setup-local.md](docs/setup-local.md)
- **CI:** [docs/setup-ci.md](docs/setup-ci.md)
- **Deployment:** [deploy/README.md](deploy/README.md)
- **Coding rules:** [.claude/clean-code-skill/SKILL.md](.claude/clean-code-skill/SKILL.md)
- **API contracts:** [docs/api/identity-api.md](docs/api/identity-api.md) (accounts and authorization). Postman: [docs/postman/](docs/postman/)

## Quick start

```sh
cp .env.example .env
./gradlew bootRun      # compose.yaml starts PostGIS (5433), Redis and MinIO
./gradlew build        # tests + format check + module verification + coverage gate
```

## Modules

| Package | Module | Owns |
|---|---|---|
| `identity` | Identity + Admin | users, refresh_tokens, user_blocks, reports, report_reasons, disputes, audit_log, app_parameters |
| `listing` | Listing + Search | categories, listings, listing_images, favorites |
| `transaction` | Transaction | transactions |
| `areaverification` | Area Verification | user_areas, area_verifications, device_signals |
| `reputation` | Reputation | reviews, reputation_snapshots |
| `chat` | Chat + Notify | conversations, messages, notifications, device_tokens |
| `shared` | Shared kernel (open) | security, errors, storage, WebSocket, rate limiting, `GeoPoint` |

Each module is split into `web` / `application` / `domain` / `infrastructure` layers. Only the module's root package is public, and it holds two kinds of things:

- **Facade interfaces**, for example `identity.AppParameters` and `identity.LocationConsents`.
- **Domain events** published by that module, for example `identity.UserBanned`. They go through the `event_publication` outbox.

Application services are declared as an interface (`AuthService`) with one implementation (`AuthServiceImpl`). Controllers depend only on the interface.

`ModularityTests` enforces these boundaries, so a violation fails the build. Module docs are generated to `build/spring-modulith-docs/`.

## API conventions

- **Base path:** `/api/v1`.
- **Auth:** `Authorization: Bearer <JWT>`. The JWT is HS256 and issued by `TokenService`.
- **Errors:** RFC 9457 `application/problem+json` with a stable `code` field. Throw a `shared.error` exception subtype; `GlobalExceptionHandler` is the only place that maps exceptions to HTTP.
- **Pagination:** cursor-based, using `CursorPage`.
- **Idempotency:** state-changing endpoints annotated with `@Idempotent` require an `Idempotency-Key` header.
- **Realtime:** STOMP over `/ws`. Send the JWT in the CONNECT frame's `Authorization` header.
- **Images:** clients upload and download directly with presigned URLs from `ImageStorage`.
- **Constants:** shared constants live in `shared.constant`, one class per concern (`HeaderNames`, `ApiPaths`, `ErrorCodes`, ...). Standard HTTP headers come from Spring's `HttpHeaders`. See [docs/setup-local.md](docs/setup-local.md#constants).

## Quality gates (CI)

`./gradlew build` fails on any of the following:

- a failing test
- a module boundary violation
- unformatted code (`./gradlew format` fixes it)
- line coverage under 60% overall, or under 80% for `domain` / `application` packages
