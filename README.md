<div align="center">

<!-- Replace this comment with your banner image once exported -->
<!-- <img src="docs/assets/banner.png" alt="Synapse Banner" width="100%"> -->

# Synapse

[![CI/CD](https://img.shields.io/badge/CI%2FCD-passing-emerald?style=flat-square)](https://github.com/dev-amir/synapse)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%7C%20DDD-blueviolet?style=flat-square)](https://alistair.cockburn.us/hexagonal-architecture/)
[![Code Style](https://img.shields.io/badge/Code%20Style-Google-blue?style=flat-square)](https://google.github.io/styleguide/javaguide.html)

**A real-time communication backend built to deeply understand WebRTC, WebSocket, and distributed systems — engineered with Hexagonal Architecture and Domain-Driven Design.**

</div>

---

## What is Synapse?

Synapse is a backend engine for real-time communication — chat, voice calls, video calls, and async media messaging. It is not a production SaaS. It is a deliberate learning project where every architectural decision is intentional, every abstraction is reasoned, and the goal is deep understanding over quick shipping.

The project applies **Hexagonal Architecture (Ports & Adapters)** and **DDD tactical patterns** strictly — not as decoration, but as the mechanism for keeping domain logic isolated from frameworks, databases, and external services. Built on Java 21 and Spring Boot 4.0.6.

---

## Why I Built This

I wanted to understand what happens below the surface of real-time systems — how WebRTC negotiates peer connections, how WebSocket sessions are managed at scale, how you model stateful communication flows inside a clean domain model without leaking infrastructure concerns into your business logic.

Synapse is the environment where I answer those questions through code.

---

## Module Status

| Module | Domain | Status |
|---|---|---|
| `identity` | Authentication, user registration, Google OAuth | ✅ Functional end-to-end |
| `messaging` | Real-time text & media messaging (WebSocket) | 🔧 In progress |
| `call` | Voice & video signaling via WebRTC | 🔧 In progress |
| `presence` | Ephemeral user status tracking (Redis) | 🔧 In progress |
| `shared` | Cross-cutting utilities & shared kernel | ✅ Stable |

---

## Architecture

Synapse uses a strict multi-module layout where each Bounded Context lives in its own module. Every module enforces the Hexagonal structure internally:

```
synapse/
├── identity/               # Identity & Access Management
│   └── src/main/java/dev/amir/synapse/identity/
│       ├── domain/         # Aggregates, Value Objects, Domain Events, Ports
│       │   ├── model/      # User, Identity — rich domain models with invariants
│       │   ├── port/
│       │   │   ├── in/     # Use Case interfaces (inbound)
│       │   │   └── out/    # Repository & external service contracts (outbound)
│       │   ├── event/      # Domain Events (UserRegisteredEvent, ...)
│       │   └── value_object/
│       ├── application/    # Application Services — orchestrate domain tasks
│       └── infrastructure/ # Adapters — JPA, Google API, JWT
│           └── adapter/
│               ├── in/     # Inbound: REST controllers
│               └── out/    # Outbound: DB persistence, Google token verification
├── messaging/
├── call/
├── presence/
├── shared/
└── pom.xml                 # Parent BOM
```

**Key design rules enforced across all modules:**
- Domain layer has zero framework dependencies — no Spring, no JPA
- Ports are defined in the domain; adapters live in infrastructure
- Application services depend only on port interfaces, never on concrete adapters
- Each Bounded Context exposes its own API surface through `port/in`

---

## Authentication Flow

The identity module implements a stateless Google OAuth flow where the client handles the browser-side authentication and passes the signed Google ID token directly to the backend for verification and provisioning.

```
┌─────────────┐        ┌───────────────────┐        ┌──────────────────────┐
│  Client App │        │  Synapse Backend   │        │  Google OAuth Engine │
└─────────────┘        └───────────────────┘        └──────────────────────┘
      │                          │                              │
      │── 1. Native auth ──────>│                              │
      │   (obtains ID token)     │                              │
      │                          │                              │
      │── 2. POST /v1/auth/google│                              │
      │   { googleIdToken }  ───>│                              │
      │                          │── 3. Verify signature ──────>│
      │                          │<── 4. Verified claims ───────│
      │                          │                              │
      │                          │── 5. JIT provisioning        │
      │                          │   (register if new user)     │
      │                          │                              │
      │<── 6. Native JWT ────────│                              │
```

> **Note on `redirect_uri_mismatch`:** Since Synapse processes client-minted tokens — not browser redirects — there is no backend callback URI. In Google Cloud Console, set your **Authorized redirect URIs** to your client environment (e.g., `http://localhost:8020/scalar` for local testing), not a backend endpoint.

---

## Getting Started

### Prerequisites

- Docker Engine & Compose V2
- JDK 21
- A `.env` file in the project root:

```env
# Networking
SYNAPSE_SERVICE_HOST_PORT=8020
SYNAPSE_SERVICE_CONTAINER_PORT=8080

# PostgreSQL
POSTGRES_IMAGE=postgres:16-alpine
POSTGRES_HOST_PORT=5432
POSTGRES_CONTAINER_PORT=5432
POSTGRES_DB=synapse_db
POSTGRES_USER=synapse_admin
POSTGRES_PASSWORD=vault_secure_database_password

# Redis
REDIS_IMAGE=redis:7.2-alpine
REDIS_HOST_PORT=6379
REDIS_CONTAINER_PORT=6379

# JWT (minimum 32 bytes)
JWT_SECRET=your-super-secret-synapse-key-32-bytes-long-minimum!!
```

### Start Infrastructure

```bash
docker compose up -d synapse-database synapse-cache --wait
```

The `--wait` flag blocks until PostgreSQL is healthy and Redis is ready.

### Run the Identity Module

```bash
./mvnw spring-boot:run -pl identity
```

The server starts on `http://localhost:8020`.

---

## API Documentation

Synapse ships with interactive API documentation out of the box — no external clients needed.

| Interface | URL |
|---|---|
| Scalar UI (interactive) | `http://localhost:8020/scalar` |
| OpenAPI spec (raw JSON) | `http://localhost:8020/v3/api-docs` |

Test the auth endpoint directly:

```bash
curl -X POST http://localhost:8020/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"googleIdToken": "your_google_id_token_here"}'
```

---

## Code Quality

Every commit passes through a local pre-commit hook and the GitHub Actions pipeline. Code that fails formatting or static analysis cannot be merged.

| Check | Tool | When |
|---|---|---|
| Formatting | Spotless (Google Java Format 1.19.2) | Pre-commit + CI |
| Style rules | Checkstyle | CI |
| Bug detection | PMD + SpotBugs | CI |
| Full build | Maven | CI |

### Run checks locally

```bash
# Auto-format the codebase
./mvnw spotless:apply

# Run static analysis
./mvnw checkstyle:check pmd:check spotbugs:check

# Full verification (includes tests)
./mvnw clean verify
```

### Install the pre-commit hook

```bash
./mvnw initialize
```

This links `.git/hooks/pre-commit` via `maven-antrun-plugin`. From this point, every local commit runs formatting verification before it goes through.

---

## Key Design Decisions

**Why Hexagonal Architecture?**
I wanted the domain to be testable without spinning up Spring or a database. Every use case can be tested by injecting in-memory fakes through the port interfaces.

**Why split modules by Bounded Context instead of technical layers?**
Organizing by feature (identity, messaging, call) rather than by layer (controllers, services, repositories) means each module can evolve independently. The `messaging` module doesn't know anything about `identity` at the code level.

**Why a headless OAuth flow?**
Mobile and desktop clients handle the browser interaction natively. Having the backend process a signed token (rather than managing a redirect loop) keeps the backend stateless and decoupled from the client's platform.

---

## Roadmap

- [ ] WebSocket session management in `messaging`
- [ ] WebRTC signaling server in `call` (SDP offer/answer, ICE negotiation)
- [ ] Presence tracking with Redis pub/sub in `presence`
- [ ] Domain event publishing between Bounded Contexts via Spring Modulith
- [ ] Integration test coverage per module

---

## License

MIT — do whatever you want with it.