# Local development setup

This guide takes you from a fresh clone to a running backend that the Android app can call.

## 1. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 25 | The Gradle toolchain picks up JDK 25 (IntelliJ: *File → Project Structure → SDK*). |
| Docker Desktop | 4.x or later | Must be **running** before you start the app or run tests. |
| Git | any | |
| IntelliJ IDEA | recommended | Enable *Annotation Processing* for Lombok. |

You do not need to install PostgreSQL, Redis or MinIO. They all run in Docker.

## 2. Environment variables

```sh
cp .env.example .env
```

Both Docker Compose (`compose.yaml`) and Spring Boot (`spring.config.import`) read `.env`, so you configure everything in one place. `.env` is gitignored, so **never commit it**. When you add a new variable, add it to `.env.example` too.

Key variables:

| Variable | Default | Purpose |
|---|---|---|
| `POSTGRES_PORT` | `5433` | Host port of the PostGIS container. It is not `5432`, to avoid clashing with a PostgreSQL installed directly on the machine. |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `nearby` | Database credentials inside the container. |
| `REDIS_PORT` | `6379` | Redis (cache, rate limiting, idempotency). |
| `JWT_SECRET` | dev value | HS256 signing key, at least 32 bytes. Generate one with `openssl rand -base64 48`. |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Address the backend uses to reach MinIO. |
| `MINIO_PUBLIC_ENDPOINT` | `http://10.0.2.2:9000` | Host the Android app uses for presigned image URLs (see section 6). |
| `SERVER_PORT` | `8080` | API port. |

Real OS environment variables, and IntelliJ *Run Configuration* variables, always override values in `.env`.

## 3. Run the app

### Option 1: IntelliJ

Open `NearbyBackendApplication` and click *Run*. On first start, `spring-boot-docker-compose` runs `docker compose up` for PostGIS, Redis and MinIO.

### Option 2: Terminal

```sh
./gradlew bootRun          # Windows: gradlew.bat bootRun
```

On first start, Flyway creates the schema (22 tables plus the outbox) and seeds the reference data.

### Check that the app is running

| URL | Expected result |
|---|---|
| http://localhost:8080/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api/test | `{"status":"ok", ...}` |
| http://localhost:9001 | MinIO Console. Log in with `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`. |

To browse the database with DBeaver or DataGrip, use host `localhost`, port `5433`, and `nearby` as the database, user and password.

## 4. Run the tests

```sh
./gradlew build            # tests + format check + module boundaries + coverage
./gradlew test             # tests only
./gradlew test --tests '*TokenServiceTests'
./gradlew format           # format code before committing
```

- Integration tests use Testcontainers with real PostGIS and Redis, so Docker must be running. Tests do not use `.env` or the `compose.yaml` containers.
- The coverage report is at `build/reports/jacoco/test/html/index.html`. The build fails if overall line coverage is under 60%, or under 80% for `domain` / `application` packages.
- The module report is at `build/spring-modulith-docs/`.
- Unformatted code fails the build. Run `./gradlew format` and build again.

## 5. Code structure

```
src/main/java/vn/edu/uit/nearby/nearby_backend/
├── identity/          Identity + Admin
├── listing/           Listing + Search
├── transaction/       Transaction
├── areaverification/  Area Verification
├── reputation/        Reputation
├── chat/              Chat + Notify
└── shared/            Shared kernel: constant, error, security, storage, websocket, ratelimit, geo, web
```

- Each module is split into `web` / `application` / `domain` / `infrastructure` layers. Only the root package is public; it holds Facade interfaces and domain events.
- `ModularityTests` fails the build when a module reaches into another module's internals.
- Coding rules live in `.claude/clean-code-skill/`.

### Constants

Shared constants live in `shared/constant/`, one class per concern, so every name says what it is:

| Class | Holds |
|---|---|
| `HeaderNames` | Custom HTTP headers, for example `IDEMPOTENCY_KEY` |
| `ApiPaths` | URL paths and security matcher patterns |
| `WebSocketDestinations` | STOMP destination prefixes |
| `SecurityConstants` | Bearer scheme, JWT claim names, authority prefix |
| `Roles` | `USER`, `ADMIN` |
| `ErrorCodes` | Problem-detail `code` values and the problem type base URI |
| `RedisKeyPrefixes` | Redis key namespaces |
| `ApiLimits` | Page sizes and header length limits |
| `GeoConstants` | `WGS84_SRID` |

Rules:

- Standard HTTP headers come from Spring's `org.springframework.http.HttpHeaders` (`AUTHORIZATION`, `CONTENT_TYPE`...). Do not redefine them.
- Add a new constant to the class of its concern, or create a new concern class. Never use a vague name such as `HEADER` or `PREFIX`.
- A value that is an internal detail of one class stays `private` in that class. Examples are the cursor encoding separator and a request attribute name.
- Values that may change at runtime belong in `app_parameters`, not in code.

## 6. Calling the API from the Android app

| Device | Base URL | `MINIO_PUBLIC_ENDPOINT` |
|---|---|---|
| Android Emulator | `http://10.0.2.2:8080` | `http://10.0.2.2:9000` |
| Physical phone on the same Wi-Fi | `http://<machine-LAN-IP>:8080` | `http://<machine-LAN-IP>:9000` |

- Inside the emulator, `localhost` is the emulator itself, not your computer.
- Android blocks plain HTTP by default. For debug builds, add a `network_security_config` that allows cleartext traffic to `10.0.2.2` or your LAN IP.
- Presigned URLs are signed for a specific host, so `MINIO_PUBLIC_ENDPOINT` must be exactly the host the phone calls. Change it in `.env` and restart the app.
- Every API except `/api/v1/auth/**` needs `Authorization: Bearer <access-token>`. Errors follow RFC 9457 (`application/problem+json`) and include a `code` field.
- WebSocket: STOMP at `ws://<host>:8080/ws`. Send `Authorization: Bearer <token>` in the CONNECT frame.

## 7. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `password authentication failed for user "nearby"` | The app connected to a PostgreSQL installed on the machine (port 5432) instead of the container. | Keep `POSTGRES_PORT=5433` in `.env` and run `docker compose up -d` to recreate the container. `docker compose ps` should show `0.0.0.0:5433->5432`. |
| `Port 8080 was already in use` | Another instance is running, usually from IntelliJ. | Stop it, or set `SERVER_PORT=8081` in `.env`. |
| `Could not find a valid Docker environment` during tests | Docker Desktop is not running. | Start Docker Desktop and wait until it shows *Running*. |
| `Image storage not reachable` (WARN) | MinIO is not running. The app still starts. | `docker compose up -d minio` |
| Flyway `checksum mismatch` | A migration that already ran was edited. | Never edit an applied migration; add a new `V9__...sql` instead. To reset local dev data, run `docker compose down -v`, which deletes all local data. |
| `checkFormatMain FAILED` | Code is not formatted. | `./gradlew format` |

## 8. Useful Docker commands

```sh
docker compose ps                 # container status
docker compose logs -f postgres   # follow logs
docker compose down               # stop, keep data
docker compose down -v            # stop and DELETE local data
```

For production deployment, see [deploy/README.md](../deploy/README.md).
