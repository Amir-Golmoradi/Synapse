# 7. Voice call lifecycle and signaling boundary

Date: 2026-09-10

## Status

Accepted

## Context

Voice calls need durable lifecycle decisions, short-lived WebRTC negotiation, authenticated routing,
and predictable cleanup. Synapse already has JWT-authenticated STOMP infrastructure, PostgreSQL,
and a simple in-memory broker. It does not have a Call or Presence implementation.

## Decision

Voice calls live in a separate `call` bounded context. REST carries transactional lifecycle
commands. Private STOMP user destinations carry lifecycle notifications, connection control, and
WebRTC offer/answer/ICE messages. PostgreSQL stores lifecycle, participant reservations, deadlines,
and bounded recovery metadata. It does not store SDP, ICE candidates, or media.

The state machine is `RINGING -> CONNECTING -> ACTIVE`, with `RECOVERING` for a rebuilt peer
connection and explicit terminal states `REJECTED`, `CANCELLED`, `MISSED`, `ENDED`, and `FAILED`.
Each registered user can have one nonterminal call. Browser refresh can resume the same call from the
same browser instance through a new negotiation generation.

Shared STOMP endpoint, authentication, broker, and destination-dispatch infrastructure lives in
`shared`. Messaging and Call contribute capability-specific destination policies.

## Alternatives considered

- Call behavior inside Messaging was rejected because call lifecycle is independent of rooms and
  messages.
- STOMP-only lifecycle commands were rejected because REST provides clearer transaction and retry
  outcomes.
- Redis ownership was rejected for the single-instance implementation because PostgreSQL already
  provides the required correctness boundary and Redis would not distribute the simple broker.
- Persisting signals was rejected because stale SDP and candidates are unsafe to replay; recovery
  starts a fresh negotiation generation.
- Treating acceptance as `ACTIVE` was rejected because acceptance does not prove connectivity.

## Consequences

The implementation supports deterministic lifecycle history, participant isolation, busy-state
enforcement, and bounded refresh recovery. Live notification delivery remains best effort and must
be reconciled with the current-call API. Horizontal deployment requires a distributed broker and
session-routing design. Reliable internet calling requires separately operated TURN infrastructure.
