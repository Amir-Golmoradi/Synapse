# 8. Video calls extend the Call media type

Date: 2026-09-13

## Status

Accepted

## Context

Synapse already coordinates one-to-one Voice Calls through a durable Call lifecycle and ephemeral
WebRTC signaling. Video needs the same participants, authorization, exclusivity, state transitions,
recovery, and signaling, but clients must know whether camera media was requested.

## Decision

Add immutable `VOICE` and `VIDEO` media types to the existing Call aggregate and persisted call
record. Require the type when starting a call and include it in lifecycle views and idempotency.
Reuse all lifecycle, reservation, STOMP, signaling, timeout, and recovery infrastructure.

SDP remains opaque and may contain audio, video, or both. Camera acquisition may fall back to audio
for a Video Call, while microphone access remains required. Camera and microphone enabled state is
client-local and is not persisted. STUN and TURN remain external WebRTC infrastructure.

## Alternatives considered

- A separate Video Call module or aggregate was rejected because it duplicates Call behavior.
- Inferring media type from SDP was rejected because SDP is ephemeral and browser-controlled.
- Separate Video lifecycle and signaling events were rejected because media type already
  discriminates a shared lifecycle.
- Server-side media negotiation or relay was rejected because Synapse is not a media server.
- Redis runtime state was rejected for the single-instance MVP because it would not distribute the
  simple STOMP broker.

## Consequences

Existing call records migrate to Voice. New clients must send an explicit media type. Voice and
Video remain mutually exclusive under the same per-user reservation. Horizontal scaling still
requires distributed broker and session-routing work, and reliable internet connectivity requires
an independently operated TURN service.
