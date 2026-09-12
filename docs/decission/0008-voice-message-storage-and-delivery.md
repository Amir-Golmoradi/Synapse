# 8. Voice message storage and delivery

Date: 2026-09-12

## Status

Accepted

## Context

Synapse Messaging already provides room authorization, sender-scoped message idempotency,
PostgreSQL history, and live room-topic delivery. It has no binary upload or storage abstraction.
Voice messages are recorded before sending, persisted, and replayed later; they are not interactive
WebRTC calls.

## Decision

Voice Message belongs to the Messaging bounded context. Clients upload audio with authenticated
multipart HTTP. PostgreSQL stores a typed message and a one-to-one media metadata record. A private
filesystem adapter stores immutable bytes behind an outbound port for the MVP. Downloads recheck
room membership and support one HTTP byte range. Canonical voice messages use the same room STOMP
topic and history query as text messages.

WebM/Opus, Ogg/Opus, and MP4/AAC are accepted up to 16 MiB and a declared duration of five minutes.
Synapse performs structural header validation and keeps audio outside its static web root. It does
not transcode, relay media through WebRTC, or store audio in PostgreSQL or Redis.

The upload is stored before a short database transaction. Database failure triggers compensating
deletion, and an age-based scanner removes partial and unreferenced objects. Live delivery is an
after-commit, best-effort application notification; REST history remains the recovery mechanism.

## Alternatives considered

- Call-module ownership was rejected because voice messages are durable asynchronous messages, not
  real-time sessions.
- Binary STOMP upload was rejected because the broker is not a bulk-transfer transport.
- PostgreSQL `BYTEA` and Redis storage were rejected because media bytes do not belong in the
  relational or ephemeral state stores.
- S3/MinIO was deferred because no object-storage infrastructure exists and a port keeps that
  migration local to an adapter.
- Creating the database message before upload was rejected because it exposes incomplete messages
  and requires an upload state machine.
- FFmpeg transcoding was deferred because it adds native processing and operational queues without
  an MVP requirement for one normalized codec.

## Consequences

The first deployment remains single-instance and must mount and back up a durable media volume.
PostgreSQL and the filesystem are not one atomic resource, so compensation and delayed cleanup are
required. Duration is bounded client metadata rather than fully codec-verified. Multi-instance
deployment requires shared object storage, while guaranteed realtime delivery requires a
system-wide outbox and distributed broker.
