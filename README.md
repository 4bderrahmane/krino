# Interview Slot Manager

A monorepo containing the full Interview Slot Manager application, split into two
independently deployable services:

| Path        | Service | Stack |
|-------------|---------|-------|
| [`client/`](./client) | Web frontend | React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, axios, i18next |
| [`server/`](./server) | REST API     | Spring Boot (Java 21), Spring Data JPA, Spring Security + OAuth2/JWT, MySQL (H2 for tests) |

The client talks to the server's REST API. By default the client expects the API at
`http://localhost:8080/api` (see `client/src/shared/services/api.ts`).

> This repository was created by merging two previously separate repositories.
> The full commit history of both projects is preserved under the `client/` and
> `server/` paths.

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

- **Node.js** 18+ and npm (for the client)
- **JDK 21** (for the server — a Maven wrapper `mvnw` is included, so a local Maven install is optional)
- A **MySQL** database for the server in non-test runs
- **Docker** + **Docker Compose** (optional, to run everything in containers)

## Running locally

### Server (`http://localhost:8080`)

The server reads its configuration from `server/src/main/resources/application.properties`,
which is intentionally **not committed** (it holds DB credentials and secrets). Create it
before running — at minimum it needs your MySQL datasource and JWT settings.

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

From the repository root:

```bash
docker compose up --build
```

This builds and starts both services:

- **server** — Spring Boot API on [http://localhost:8080](http://localhost:8080) (matches the client's hardcoded API URL)
- **client** — served by nginx on [http://localhost:5173](http://localhost:5173)

> The server still needs a reachable MySQL instance and an `application.properties`
> with the correct datasource. Adjust `docker-compose.yml` (and add a database service)
> to match your environment before relying on it in CI/production.
