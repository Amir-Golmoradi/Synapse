# 5. User search

Date: 2026-07-02

## Status

Proposed

## Context

Messaging needs user discovery so clients can start direct rooms and invite
participants without exchanging private identifiers out of band.

Identity currently has no public search contract. The only stable identifiers
are internal UUIDs, Google IDs, and emails. ADR 0003 introduces a public handle;
search should build on that handle rather than expose private identity fields.

Search can become operationally expensive if introduced with a dedicated search
engine too early. PostgreSQL is already the source of truth, already required by
the application, and can serve handle prefix search for v1.

## Decision

Expose `UserSearchUseCase` through the Identity application contract.

For v1, search behavior is:

- Search only by handle.
- Case-insensitive prefix match.
- PostgreSQL-backed.
- Paginated.
- Ordered deterministically by handle.
- Returns public-safe fields only:
  - user ID
  - handle
  - display name
  - avatar/profile image URL
- Never returns email, Google ID, refresh-token state, or other private identity
  attributes.

The use case lives behind an outbound `UserSearchPort`. PostgreSQL is the first
adapter and remains the source of truth.

ElasticSearch/OpenSearch is deferred to a later learning-goal item. If adopted,
it must be a derived index behind the same port, not the source of truth. Index
sync must be owned explicitly through domain events and a reindex/backfill path,
and a later ADR must document the consistency model and operational cost.

## Alternatives Considered

### Search by email

Rejected. Email is private identity data and must not be exposed through public
discovery.

### Search by display name

Deferred. Display names are useful for presentation but ambiguous for v1
discovery. Handle-only search gives a clear and testable baseline.

### Use PostgreSQL trigram or fuzzy search immediately

Deferred. Prefix search is simpler, easier to reason about, and sufficient for
v1. Fuzzy search can be added after real usage demonstrates the need.

### Adopt ElasticSearch/OpenSearch immediately

Rejected for the baseline. A search engine adds operational cost and, more
importantly, introduces index synchronization as a correctness concern. That is
valuable to learn later, but PostgreSQL is the correct first implementation.

## Consequences

### Positive

- Discovery starts with a simple, privacy-safe contract.
- PostgreSQL remains the single source of truth.
- The application boundary is ready for a future search-engine adapter without
  coupling use cases to a specific backend.
- Privacy expectations are explicit in the return shape.

### Negative

- Prefix-only search is less flexible than fuzzy or display-name search.
- PostgreSQL-backed search may need additional indexing work as data grows.
- A future search-engine adapter will require explicit index-sync design rather
  than being a drop-in replacement.

### Follow-up Work

- Add the `UserSearchUseCase`, query, response DTOs, and outbound port.
- Add the PostgreSQL adapter and web endpoint.
- Add integration and web-slice tests proving pagination, prefix matching, and
  privacy of returned fields.
- Capture any future ElasticSearch/OpenSearch adoption in a dedicated ADR.
