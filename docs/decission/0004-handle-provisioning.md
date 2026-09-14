# 4. Initial Handle Provisioning

Date: 2026-07-02

## Status

Accepted (amended 2026-07-19)

## Context

Google sign-in provisions users just in time. Display names are neither unique
nor restricted to ASCII, and concurrent requests can derive the same readable
Handle. A read-before-write availability check has a race window.

## Decision

`InitialHandlePolicy` is the sole allocation policy. It sanitizes the verified
display name; email is never an allocation input. Common accented Latin names
are decomposed into readable ASCII, while Unicode-only or otherwise unusable
names go directly to the stable fallback.

For one generated `UserId`, candidates are attempted in this exact order:

1. The readable base when it is valid and not reserved.
2. `_1` through `_5`, truncating the base independently so each result remains
   at most 32 characters.
3. `u_` followed by the complete unsigned 128-bit UUID encoded as 25
   zero-padded base-36 characters.

Reserved bases skip only the bare candidate, so `admin_1` can be allocated. The
`u_` prefix is reserved by allocation policy for UUID fallbacks; a display-name
base beginning with it is prefixed with `member_`.

Each candidate is inserted and flushed in its own `REQUIRES_NEW` transaction.
PostgreSQL's named unique constraints are the authority. A Handle conflict
advances to the next candidate. A Google-subject conflict reloads the winning
user. An email conflict reloads by Google subject first and otherwise becomes a
409 account conflict. Unknown integrity failures are not relabeled.

The UUID fallback must succeed whenever the generated `UserId` is unique. A
fallback Handle or primary-key collision is treated as provisioning exhaustion
and returns 503; all failed attempts remain rolled back.

## Consequences

- Allocation is deterministic, bounded, race-safe, and independent of Redis.
- The same `UserId` is reused for every retry.
- There is no random retry phase, migration dependency, or email-derived base.
- Database resets are acceptable during active development, so upgrade and
  backfill history are not part of this decision.
