# Krino

Krino is a recruitment and interview-scheduling platform. Recruiters publish job
openings, candidates apply with a CV, and the hiring team schedules interviews
against interviewers' availability — with each time slot bookable only once, so
interviews can't be double-booked.

The repository is a monorepo split into two independently deployable services:

| Path        | Service | Stack |
|-------------|---------|-------|
| [`client/`](./client) | Web frontend | React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, axios, i18next |
| [`server/`](./server) | REST API     | Spring Boot (Java 25), Spring Data JPA, Spring Security with JWT (cookie-based), PostgreSQL (H2 for tests), Flyway, MapStruct, MinIO |

The client talks to the server's REST API. By default the client expects the API at
`http://localhost:8080/api` (see `client/src/shared/services/api.ts`).

> This repository was created by merging two previously separate repositories.
> The full commit history of both projects is preserved under the `client/` and
> `server/` paths.

## What it does

- **Job postings** — recruiters create and publish openings; candidates browse them with PostgreSQL full-text search.
- **Applications** — candidates apply and upload a CV, stored in S3-compatible object storage (MinIO).
- **Interview scheduling** — interviews are booked against interviewers' availability slots; one interview per slot is enforced at the database to prevent double-booking.
- **Roles & access** — role-based access control across admin, HR manager, interviewer, and candidate.
- **Authentication** — cookie-based JWT with refresh-token rotation; auth endpoints are rate-limited.
- **Bilingual UI** — French / English (i18next).

## Project structure

```
.
├── client/   # React + Vite frontend (served via nginx in production)
├── server/   # Spring Boot REST API
├── docker-compose.yml
├── .gitignore
└── README.md
```

## Prerequisites

- **Node.js** 20+ and npm (for the client — Vite 7 requires Node 20.19+ / 22.12+)
- **JDK 25** (for the server — a Maven wrapper `mvnw` is included, so a local Maven install is optional)
- A **PostgreSQL** database for the server in non-test runs
- **Docker** + **Docker Compose** (optional, to run everything in containers)

## Running locally

### Server (`http://localhost:8080`)

The server reads secrets and local overrides from `server/.env`. Copy the example
file and fill in the values before running the API directly:

```bash
cp server/.env.example server/.env
```

```bash
cd server
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Run the tests (uses an in-memory H2 database, no setup needed):

```bash
cd server
./mvnw test
```

### Client (`http://localhost:5173`)

```bash
cd client
npm install
npm run dev        # start Vite dev server
npm run build      # type-check + production build into client/dist
npm run lint       # run ESLint
```

## Running with Docker Compose

The application services (`db`, `server`, `client`) are gated behind the `app`
Compose profile, so start the full stack from the repository root with:

```bash
docker compose --profile app up --build
```

> Running `docker compose up` *without* `--profile app` starts only MinIO and its
> bucket initialiser (handy for testing storage in isolation).

This builds and starts:

- **server** — Spring Boot API on [http://localhost:8080](http://localhost:8080) (matches the client's hardcoded API URL)
- **client** — served by nginx on [http://localhost:5173](http://localhost:5173)
- **db** — PostgreSQL on host port `5432` (internal compose address `db:5432`)
- **minio** — S3-compatible CV storage on [http://localhost:9000](http://localhost:9000), console on [http://localhost:9001](http://localhost:9001)
- **minio-init** — creates the CV bucket (`krino-cvs`) before the server starts

The compose stack still requires `server/.env` for JWT and admin bootstrap values,
but it overrides the datasource and MinIO endpoint so containers talk over the
compose network. To reset the containerized database and MinIO data, run:

```bash
docker compose --profile app down -v
```
