# 2. Flyway owns the database schema

Date: 2026-06-26

## Status

Accepted

## Context

Application code in Synapse is version-controlled, reviewed, and deployed
through a controlled process. The database schema was not. Tables were created
at runtime by Hibernate's `ddl-auto: update`, which inspects the JPA entities on
startup and mutates the schema to match.

This approach has serious problems for a project that intends to be
production-oriented:

- **No history.** There is no record of how the schema reached its current
  state, when it changed, or why. It cannot be reviewed in a pull request.
- **No reproducibility.** The exact DDL depends on the Hibernate version,
  dialect, and entity annotations, so two databases can silently diverge. A
  schema cannot be recreated deterministically for debugging or testing.
- **`update` only adds.** It does not safely rename columns, change types, or
  remove fields. Renames leave orphaned columns; type changes are often silently
  ignored. It cannot express data backfills or careful multi-step changes.
- **It is unsafe in production.** Allowing an ORM to rewrite the production
  schema on startup, based on whatever the entities currently say, risks data
  loss. This is why `ddl-auto: update` is not an acceptable production strategy.

Concretely, the `users` and `refresh_tokens` tables had Flyway migrations
(`V1`, `V2`), but the `rooms` and `room_members` tables existed only because
`ddl-auto: update` created them at runtime — there was no migration describing
them, and the schema was therefore not under any source control.

## Decision

Flyway is the single owner of the database schema.

- All schema changes are expressed as ordered, versioned SQL migrations
  (`V1__...`, `V2__...`, `V3__...`) committed to the repository under
  `src/main/resources/db/`.
- Applied migrations are immutable. To change the schema, a new migration is
  added; an already-applied migration is never edited. Flyway enforces this
  through a checksum in its `flyway_schema_history` table and refuses to start
  if an applied migration has been altered.
- Hibernate no longer generates schema. `ddl-auto` is set to `validate`:
  Flyway creates and changes the schema, and Hibernate only verifies on startup
  that the resulting schema matches the entity mappings, failing fast if they
  have drifted.
- `ddl-auto: update` is abandoned for all environments.

A `V3__create_rooms_table.sql` migration was added so the `rooms` and
`room_members` tables are described by a reviewed migration rather than inferred
by Hibernate.

## Alternatives Considered

- **Keep `ddl-auto: update`.** Rejected for the reasons above — it is the
  absence of a migration strategy and is unsafe in production.
- **Liquibase.** A capable alternative whose main advantage is
  database-agnostic changelogs (XML/YAML/JSON) and richer changeset semantics.
  Synapse targets PostgreSQL exclusively and prefers plain, reviewable SQL, so
  the abstraction would add cost without solving a problem we have. Flyway, with
  hand-written SQL, fits the project better. Flyway was also already wired into
  the build.

## Consequences

### Positive

- The schema is version-controlled, reviewable in pull requests, and has an
  auditable history via `flyway_schema_history`.
- Any database — a fresh laptop, CI, staging, production — reaches an identical
  schema by replaying the same ordered migrations.
- Conflicting schema changes become visible as colliding migration versions in a
  pull request, instead of silently diverging databases.
- The team controls exactly what each change does, including operations
  `ddl-auto` cannot express (backfills, safe renames, concurrent indexes).
- Persistence technology is decoupled from schema ownership: because Flyway, not
  Hibernate, owns the schema, future adapters (for example JOOQ) can read and
  write the same tables without changing who manages the schema.

### Negative

- Every schema change now requires writing and reviewing a migration. This is
  intended discipline, but it is more work than letting Hibernate infer changes.
- A migration must exactly match what the entities expect, or `validate` will
  fail at startup. Adding a column to an entity without a corresponding migration
  is now a startup error rather than a silent auto-fix — again intended, but it
  requires care.
- Migrations are immutable once applied, so mistakes are corrected by adding a
  new migration, never by editing the old one.

### Notes

- All current migrations keep the schema creation-only. Tightening or evolving
  existing tables will be done through new `V*` migrations.
- The `room_members.user_id` column is intentionally not a foreign key to the
  `users` table: messaging must not hold a database-level dependency on
  identity's tables, preserving the bounded-context boundary.