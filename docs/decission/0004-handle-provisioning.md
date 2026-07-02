# 4. Handle provisioning

Date: 2026-07-02

## Status

Proposed

## Context

Synapse creates users through just-in-time Google sign-in. After ADR 0003, every
user must have a unique handle, including users who already exist before the
handle column is introduced.

The provisioning process must work for two paths:

- Existing users during the migration/backfill.
- New users during Google sign-in.

The process also needs to be race-safe. Two users can sign in concurrently with
email local-parts that normalize to the same handle. A read-then-write
"available handle" check is not safe under concurrency because both requests can
observe the same handle as free before either commits.

## Decision

Provision the initial handle from the email local-part.

Algorithm:

1. Take the substring before `@`.
2. Normalize to lowercase with `Locale.ROOT`.
3. Replace unsupported character runs with a single underscore.
4. Trim leading and trailing underscores.
5. If the result is shorter than 3 characters or empty, use a stable fallback
   base such as `user`.
6. Truncate the base so a numeric collision suffix can still fit inside the
   32-character handle limit.
7. Try the base handle first.
8. On unique-constraint violation or reserved-word conflict, retry with numeric
   suffixes: `base_1`, `base_2`, and so on, within a bounded retry limit.

The database unique constraint is the source of truth for uniqueness. The
application may check obvious reserved words before saving, but it must not rely
on read-then-write availability checks for race safety.

If provisioning exhausts its retry budget, the operation fails with an explicit
identity error. It must not silently create a user without a handle.

The same provisioning service is used by the backfill migration path and the
Google sign-in path so existing and future users follow one rule.

## Alternatives Considered

### Ask the user to choose a handle during first sign-in

Rejected for v1. It introduces an onboarding step and UI/API flow before the
rest of identity discovery exists. Auto-provisioning gives every user a handle
immediately, and ADR 0003 allows later user-initiated changes.

### Use a random handle

Rejected. Random handles avoid collisions but provide poor user experience and
do not use the useful signal already available in email local-parts.

### Read existing handles first and pick the next suffix

Rejected as the correctness mechanism. It is acceptable as an optimization, but
it cannot be the concurrency guarantee. The unique database constraint plus
retry is the reliable path.

### Generate handles from full name

Rejected for v1. Full names are not unique, may contain more scripts and
punctuation, and may be less stable than email local-parts in the current Google
sign-in flow.

## Consequences

### Positive

- Every user receives a public handle without an additional onboarding step.
- Concurrent provisioning remains correct because uniqueness is enforced by the
  database.
- Existing users and new users share one provisioning rule.
- Collision behavior is deterministic and testable.

### Negative

- Email local-parts can be messy, so sanitization and fallback rules need
  explicit tests.
- Numeric suffixes can produce handles that are less pretty than a manually
  chosen value.
- The backfill path must handle large existing datasets carefully once Synapse
  has real production data.

### Follow-up Work

- Implement the provisioning service behind the Identity application boundary.
- Add tests for normalization, reserved words, suffixing, and concurrent
  collisions.
- Use the provisioning service from Google sign-in and from the user-handle
  backfill path.
