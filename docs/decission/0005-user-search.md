# 5. Handle Discovery

Date: 2026-07-02

## Status

Accepted (amended 2026-07-19)

## Context

Authenticated users need privacy-safe discovery for messaging. PostgreSQL is
already the identity source of truth; Redis can reduce repeated prefix-query
cost but must not become a correctness dependency.

## Decision

Expose authenticated Handle-prefix discovery at:

`GET /api/v1/users/search?prefix=ami&page=0&size=20`

Prefixes are lowercased and must contain 1 through 32 Handle-alphabet
characters without consecutive periods. Results are ordered by Handle and
contain only `userId`, `handle`, `displayName`, and nullable
`profilePictureUrl`. The envelope contains `items`, `page`, `size`, and
`hasNext`.

The PostgreSQL JPA adapter selects a projection, requests `size + 1`, and uses
the extra row to derive `hasNext` without a count query. Underscores are escaped
in `LIKE` patterns so they remain literal; periods are naturally literal.

Redis caches normalized slices for 60 seconds behind an application port. Keys
include schema version, cache generation, prefix, page, and size. Successful
user creation increments the generation atomically instead of scanning or
deleting keys. Redis read, write, and generation failures are swallowed as
cache misses, with PostgreSQL serving the request. Redis is never consulted for
allocation or uniqueness.

## Consequences

- Discovery cannot expose email or Google subject by construction.
- PostgreSQL remains available as the complete fallback during Redis outages.
- Generation changes invalidate all previously cached search slices in O(1).
- Mutable display-name/avatar changes can remain cached for at most the TTL.
- A future jOOQ or search-engine adapter may replace the read implementation
  behind the same port without changing the public contract.
