# 3. User handle

Date: 2026-07-02

## Status

Proposed

## Context

Synapse currently identifies users internally by UUID, Google subject, and
email. Those identifiers are not suitable as public product identifiers:

- UUIDs are stable but not human-friendly.
- Google IDs are identity-provider internals and must never become a public API.
- Email addresses are private personal data and must not be exposed for
  discovery, room creation, or search.

Messaging needs a public, stable-enough identifier so users can discover and
reference each other without leaking private identity data. The handle also
needs to support efficient prefix search in PostgreSQL for the first release.

The handle is part of the Identity bounded context. Messaging can refer to user
IDs through existing context boundaries, but handle ownership, validation,
provisioning, and search policy belong to Identity.

## Decision

Add a `Handle` value object to the Identity domain and attach it to the `User`
aggregate.

The handle format is:

- Allowed characters: lowercase ASCII `a-z`, digits `0-9`, and underscore `_`.
- Length: 3 to 32 characters after normalization.
- Normalization: trim surrounding whitespace and lowercase with `Locale.ROOT`.
- Uniqueness: globally unique across all users.
- Reserved words: blocked before persistence, with the list maintained in the
  domain policy. The list must include platform, system, support, security,
  administrator, and API-looking names that would confuse users or future
  routes.

The database schema will add a non-null `handle` column to `users`, a unique
constraint, and an index that supports prefix lookup.

Users may change their handle, but not freely. Handle changes are rate-limited
to once every 30 days. A successful handle change emits a domain event so
derived indexes and future integrations can react explicitly.

Emails and Google IDs remain private identity attributes and are not returned by
public user discovery APIs.

## Alternatives Considered

### Use email as the public identifier

Rejected. Email addresses are private data and may reveal a user's real-world
identity or organization. They also change for reasons unrelated to product
identity.

### Use UUIDs only

Rejected. UUIDs are appropriate for internal references and durable links, but
they are not suitable for user discovery or human communication.

### Allow mixed-case handles

Rejected for v1. Mixed-case handles create avoidable ambiguity around equality,
URLs, and search. Synapse can display a separate profile name for presentation;
the handle itself is normalized lowercase identity.

### Allow punctuation beyond underscore

Rejected for v1. More punctuation complicates URLs, search tokenization,
validation, and user support. The narrow character set is enough for a first
release.

## Consequences

### Positive

- Users get a human-friendly public identifier that does not expose private
  identity data.
- Prefix search can be implemented simply and efficiently in PostgreSQL.
- Messaging and future social features can reference users by handle without
  coupling to email or Google account details.
- Handle changes become explicit domain behavior instead of ad hoc profile
  edits.

### Negative

- Existing users need a backfilled handle before the column can be non-null.
- A reserved-word policy must be maintained and reviewed as product routes and
  support concepts grow.
- Rate-limiting handle changes requires storing enough state to determine when
  a user last changed their handle.

### Follow-up Work

- Add the `Handle` value object and domain tests.
- Add the Flyway migration, unique constraint, and prefix-search index.
- Backfill existing users through the handle provisioning algorithm.
- Add handle-change behavior and tests in a separate focused item.
