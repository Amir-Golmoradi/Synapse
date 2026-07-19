# 3. User Handle

Date: 2026-07-02

## Status

Accepted (amended 2026-07-19)

## Context

UUIDs are durable but not friendly for discovery, while Google subjects and
email addresses are private identity data. Synapse therefore needs a stable,
public identifier owned by the Identity context.

## Decision

Every user has a globally unique `Handle`. A Handle is normalized by trimming
surrounding whitespace and lowercasing with `Locale.ROOT`, then validated using
these rules:

- Length is 2 through 32 characters.
- Allowed characters are ASCII `a-z`, `0-9`, period (`.`), and underscore (`_`).
- Two consecutive periods are forbidden.
- There is no required first or last character and repeated underscores are
  allowed.
- Exact reserved names such as `admin`, `api`, `root`, `system`, and `user` are
  forbidden. Non-exact variants such as `admin_1` remain valid.

PostgreSQL owns global uniqueness through the named `uq_user_handle`
constraint. Email and Google subject never appear in discovery responses.

Initial allocation is defined separately in ADR 0004. User-initiated Handle
changes and their rate limits are outside this feature.

## Consequences

- Public identity is independent from private provider data.
- Periods and underscores require explicit literal escaping in prefix queries.
- Validation and reserved-name behavior have one authority: the `Handle` value
  object.
- Database uniqueness, rather than a preflight availability query or Redis,
  decides which registration owns a Handle.
