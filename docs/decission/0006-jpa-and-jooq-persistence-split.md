# 6. JPA and jOOQ persistence split

Date: 2026-07-08

## Status

Accepted

## Context

Synapse uses a modular-monolith architecture with DDD and hexagonal boundaries.
The domain and application layers depend on ports, while infrastructure adapters
implement those ports.

Persistence has two different needs:

- Aggregate persistence, where the application loads a domain object, executes
  behavior, and saves the changed aggregate with its invariants intact.
- Read-model queries, where the application needs projected data, pagination,
  filtering, sorting, counts, or SQL-specific access patterns without loading a
  full aggregate.

JPA is already used for aggregate persistence. It works well for current write
models such as `User`, `RefreshToken`, and `Room`, where the adapter maps
between persistence entities and domain objects.

jOOQ is useful for explicit SQL read models. It gives compile-time help for SQL
shape, supports PostgreSQL-specific queries, and avoids bending JPA aggregate
mapping into projection/reporting use cases.

ADR 0002 already states that Flyway owns the schema. This makes it possible for
JPA and jOOQ adapters to coexist over the same database without either
persistence technology owning schema evolution.

## Decision

Use JPA and jOOQ together, with clear adapter responsibilities.

- JPA remains the preferred tool for aggregate persistence and write-model
  adapters.
- jOOQ is introduced for read-model, projection, search, and reporting-style
  adapters where explicit SQL is the better fit.
- Flyway remains the single owner of the database schema.
- Domain code must not import JPA, jOOQ, JDBC, Spring persistence APIs, or
  generated database classes.
- Application ports must expose domain concepts, commands, queries, and
  explicit DTO/projection records, not jOOQ records or JPA entities.
- Infrastructure adapters may use JPA or jOOQ internally to implement ports.
- Generated jOOQ sources are infrastructure details and must not become part of
  domain or inbound-port contracts.

The intended split is:

- JPA: load/save aggregates such as users, refresh tokens, and rooms.
- jOOQ: list/search/read projections such as room inbox summaries, user search
  results, reporting views, and future dashboard/admin queries.

The first jOOQ use should be a read-model adapter, not a replacement for an
existing aggregate write adapter.

## Build and Code Generation Policy

jOOQ code generation must not make normal builds depend on a developer's local
database being available.

`./mvnw clean verify` should remain reproducible for CI and contributors. A
normal verification build must not fail because `localhost` PostgreSQL is down
or because a local `.env` file is missing.

Acceptable approaches include:

- Generate jOOQ sources through an explicit Maven profile or dedicated command.
- Generate from a deterministic schema created by Flyway in a controlled
  Testcontainers-backed codegen step.
- Commit generated jOOQ sources if that workflow is chosen and documented.

Unacceptable approaches include:

- Binding jOOQ code generation to the default Maven lifecycle when it connects
  to a local database.
- Reading `.env` as part of the default Maven lifecycle just to make code
  generation work.
- Mixing jOOQ introduction with unrelated observability, README, or feature
  changes.

## Alternatives Considered

### Use JPA for everything

Rejected. JPA is a good aggregate-persistence tool, but projection-heavy reads,
pagination, filtering, and SQL-specific query tuning can become awkward when
forced through aggregate mappings.

### Replace JPA with jOOQ entirely

Rejected. The current aggregate persistence model already works, and replacing
it would create unnecessary risk. The goal is to demonstrate stronger
hexagonal boundaries, not churn persistence technology for its own sake.

### Use jOOQ directly from application handlers

Rejected. It would couple application use cases to a concrete database access
technology. Ports should hide whether a query is implemented by JPA, jOOQ,
JDBC, or another adapter.

### Let Hibernate manage schema for JPA and jOOQ separately

Rejected. ADR 0002 establishes Flyway as the schema owner. Keeping one schema
authority is what allows multiple persistence adapters to coexist safely.

## Consequences

### Positive

- The repository demonstrates hexagonal architecture more clearly: the same
  application boundary can be implemented by different persistence adapters.
- JPA can stay focused on aggregate lifecycle and invariants.
- jOOQ can serve efficient read models without leaking SQL concerns into the
  domain.
- Flyway remains the single reviewed history of schema changes.
- Future read-side features such as user search can use explicit SQL without
  reshaping aggregate persistence.

### Negative

- The project now has two persistence technologies to configure, test, and
  explain.
- Developers must keep adapter boundaries disciplined so generated jOOQ types
  do not leak into ports or domain code.
- jOOQ code generation requires a deliberate workflow. If configured poorly, it
  can make builds fragile.
- Tests must cover both JPA aggregate adapters and jOOQ read-model adapters.

## Follow-up Work

- Add jOOQ build support in a dedicated build-focused PR.
- Decide and document the code-generation workflow before binding it to Maven.
- Convert one read-model adapter to jOOQ first, preferably a room summary or
  user-search projection.
- Keep aggregate write adapters on JPA unless a separate ADR justifies changing
  that boundary.
