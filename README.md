# Taskboard API

[![CI](https://github.com/gabryelvs/taskboard-api/actions/workflows/ci.yml/badge.svg)](https://github.com/gabryelvs/taskboard-api/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Trello-like task manager REST API: projects, invite-only membership, ordered
columns and cards, comments, priorities and deadlines. Java 21 / Spring Boot 3.3.

**Live demo:** https://taskboard-api-h3yu.onrender.com/swagger-ui.html (Swagger UI). The free instance sleeps when idle, so the first request after a quiet spell can take a minute or two.

## Highlights

- **JWT auth with refresh rotation** — 15-minute access tokens; opaque refresh
  tokens stored hashed (SHA-256), rotated on every use, revocable via logout.
- **No-leak authorization** — non-members get 404 (not 403) on any
  project-scoped resource, so the API never confirms a board exists.
- **Dense position ordering** — moving a card runs one transaction that takes
  pessimistic locks on both columns, closes the gap in the source column and
  opens one in the target; positions stay 0..n-1 with no fractional-rank hacks.
- **RFC 7807 errors** — every failure is `application/problem+json`.
- **Real-database tests** — 72 tests: 59 integration tests against PostgreSQL
  via Testcontainers plus 13 unit tests, run on every push in GitHub Actions.
- **Flyway migrations** — schema is versioned; Hibernate runs in
  `ddl-auto: validate` only.

## Architecture

```mermaid
flowchart LR
    Client -->|Bearer JWT| Controllers
    Controllers --> Services
    Services --> ProjectAccessService
    Services --> Repositories
    Repositories --> PG[(PostgreSQL)]
```

| Package | Responsibility |
|---|---|
| `auth` | Register/login/refresh/logout, JWT issuing and validation, refresh-token store |
| `project` | Projects CRUD and membership (invite, remove, roles) |
| `column` | Board columns with dense position ordering |
| `card` | Cards: CRUD, cross-column filtering, transactional move |
| `comment` | Card comments with author-only edit/delete |
| `common` | Security config, project access checks (the 404-no-leak rule), RFC 7807 exception handler, OpenAPI config |

## API overview

All endpoints except `/auth/**` require a `Authorization: Bearer <accessToken>`
header. "Member" means any member of the project the resource belongs to;
requests from non-members return 404.

| Method | Path | Access |
|---|---|---|
| POST | `/auth/register` | public |
| POST | `/auth/login` | public |
| POST | `/auth/refresh` | public (valid refresh token) |
| POST | `/auth/logout` | public (revokes the refresh token) |
| GET | `/projects` | authenticated (lists your projects) |
| POST | `/projects` | authenticated (creator becomes owner) |
| GET | `/projects/{id}` | member |
| PATCH | `/projects/{id}` | owner |
| DELETE | `/projects/{id}` | owner |
| GET | `/projects/{projectId}/members` | member |
| POST | `/projects/{projectId}/members` | owner (invite by email) |
| DELETE | `/projects/{projectId}/members/{userId}` | owner |
| GET | `/projects/{projectId}/columns` | member |
| POST | `/projects/{projectId}/columns` | member |
| PATCH | `/columns/{id}` | member (rename) |
| PATCH | `/columns/{id}/position` | member (reorder) |
| DELETE | `/columns/{id}` | member |
| GET | `/columns/{columnId}/cards` | member |
| POST | `/columns/{columnId}/cards` | member |
| GET | `/projects/{projectId}/cards` | member (filter by `priority`, `dueBefore`, `assigneeId`) |
| GET | `/cards/{id}` | member |
| PATCH | `/cards/{id}` | member |
| PATCH | `/cards/{id}/move` | member (target column + position) |
| DELETE | `/cards/{id}` | member |
| GET | `/cards/{cardId}/comments` | member |
| POST | `/cards/{cardId}/comments` | member |
| PATCH | `/comments/{id}` | comment author |
| DELETE | `/comments/{id}` | comment author |

Full interactive docs at `/swagger-ui.html`.

## Run locally

Needs Java 21 and Docker.

```bash
docker compose up -d          # starts PostgreSQL 16 on :5432
./mvnw spring-boot:run        # http://localhost:8080/swagger-ui.html
```

## Run tests

```bash
./mvnw test                   # needs Docker running (Testcontainers)
```

> **Windows + Docker Desktop note:** on Docker Desktop 29 or newer,
> Testcontainers may fail to detect the daemon. If that happens, create
> `%USERPROFILE%\.testcontainers.properties` with
> `docker.host=npipe:////./pipe/dockerDesktopLinuxEngine` and
> `%USERPROFILE%\.docker-java.properties` with `api.version=1.44`.

## Example flow

```bash
# 1. Register (returns accessToken + refreshToken)
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"me@example.com","password":"s3cret-pass","name":"Gabryel"}'

export TOKEN=<accessToken from the response>

# 2. Create a project
curl -s -X POST http://localhost:8080/projects \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Job hunt","description":"Applications pipeline"}'

# 3. Add a column to it
curl -s -X POST http://localhost:8080/projects/<projectId>/columns \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"To do"}'

# 4. Add a card to the column
curl -s -X POST http://localhost:8080/columns/<columnId>/cards \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Apply to Monzo","priority":"HIGH","deadline":"2026-08-01T09:00:00Z"}'

# 5. Move the card to another column, position 0
curl -s -X PATCH http://localhost:8080/cards/<cardId>/move \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"columnId":"<otherColumnId>","position":0}'
```

## Deploy

Deploys to a [Render](https://render.com) free web service (built from the
`Dockerfile`, 512 MB RAM / 0.1 CPU, spins down after 15 minutes without
traffic) with [Neon](https://neon.tech) serverless Postgres as the database.
Render redeploys on every push to `main`.

**Render service settings**

| Setting | Value |
|---|---|
| Build | from the included `Dockerfile` |
| Port | `8080`, set with the `PORT` environment variable below |
| Health check path | `/actuator/health` |

**Environment variables**

| Variable | Value |
|---|---|
| `DATABASE_URL` | Neon's **direct** (non-pooler) connection string, pasted as-is — e.g. `postgresql://user:pass@ep-xxx.eu-west-2.aws.neon.tech/taskboard?sslmode=require&channel_binding=require`. The app converts it to a JDBC URL itself (see `DatabaseUrlEnvironmentPostProcessor`). The direct string is required, not the pooled one: Flyway takes session-level advisory locks that PgBouncer's transaction pooling breaks. |
| `JWT_SECRET` | 32+ random bytes, e.g. `openssl rand -hex 32` |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | `8080` (tells Render which port the app listens on) |

**Honest caveat:** the free instance spins down after 15 minutes idle,
so the first request after that waits for the instance to come back and
Spring to start. Start-up measured at ~38s locally with a quarter of a
CPU; Render's free instance has 0.1 CPU, so expect a minute or more, plus
Neon resuming from suspend.

Startup was measured with:

```bash
docker build -t taskboard-api .
docker run --rm --memory=512m --cpus=0.25 -p 8080:8080 \
  -e DATABASE_URL=postgresql://... -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=$(openssl rand -hex 32) taskboard-api
```

against a throwaway `postgres:16` container on a Docker network, which
logged `Started TaskboardApplication in 38.004 seconds` and served
`{"status":"UP"}` from `/actuator/health`.

## License

MIT — see [LICENSE](LICENSE).
