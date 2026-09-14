# Voice and video calls

Synapse coordinates one-to-one WebRTC Voice and Video Calls. It authenticates participants,
persists lifecycle and media intent, routes signaling, and enforces one nonterminal call per user.
Audio and video flow directly between WebRTC peers or through an external TURN relay; media never
passes through the Synapse application.

## Lifecycle API

All endpoints require `Authorization: Bearer <synapse-access-token>`.

| Method and path | Purpose |
|---|---|
| `POST /api/v1/calls` | Start a call with `calleeId`, `clientRequestId`, `clientInstanceId`, and `mediaType`. |
| `POST /api/v1/calls/{id}/accept` | Accept from the first owning callee browser instance. |
| `POST /api/v1/calls/{id}/reject` | Reject a ringing call. |
| `POST /api/v1/calls/{id}/end` | End with `HANGUP` or report `MEDIA_ERROR`. |
| `POST /api/v1/calls/{id}/resume` | Recover the same call after refresh or signaling loss. |
| `GET /api/v1/calls/{id}` | Read an authorized call snapshot. |
| `GET /api/v1/calls/current` | Reconcile the authenticated user's nonterminal call. |

`mediaType` is required and accepts `VOICE` or `VIDEO`. Retry an uncertain start with the same
`clientRequestId` and identical callee, browser instance, and media type. Changing any of those
values under the same key returns `CALL_IDEMPOTENCY_CONFLICT`.

## STOMP protocol

Connect to `/ws` and put the Synapse bearer token in the STOMP `CONNECT` headers. Subscribe before
registering the client:

- `/user/queue/calls/events`
- `/user/queue/calls/signals`
- `/user/queue/calls/errors`

Then send `{ "clientInstanceId": "uuid" }` to `/app/calls/client-ready`. The server returns a
session-specific `CLIENT_READY` event. Lifecycle requests that start or accept media require this
registration.

Send WebRTC `OFFER`, `ANSWER`, and `ICE_CANDIDATE` envelopes to
`/app/calls/{callId}/signals`. Every envelope has a unique `messageId` and current `generation`.
Callers offer and callees answer. Signals are routed only to the counterpart's owning session. SDP
is opaque to Synapse and may describe audio, audio plus video, or an audio-only fallback for a Video
Call. The server retains only offer/answer IDs and hashes plus bounded candidate counts; SDP and
candidates are never stored.

Send `READY`, `CONNECTED`, `HEARTBEAT`, and `RECOVERY_REQUIRED` control messages to
`/app/calls/{callId}/control`. The server asks the original caller to offer only after both owners
are ready. Clients must discard obsolete generations and deduplicate signal IDs.

## Client media policy

Voice Calls request `getUserMedia({ audio: true, video: false })`. Video Calls first request
`getUserMedia({ audio: true, video: true })`; if camera acquisition fails, clients may retry with
audio only while retaining `mediaType=VIDEO`. Microphone access is required to start or accept.

Acquire media only after explicit user interaction. Callers should acquire before creating the call,
and recipients should acquire after selecting Accept but before sending acceptance. Muting and
camera-off use `MediaStreamTrack.enabled` and do not require backend signaling. An ended camera
track leaves the call active; an unexpectedly ended microphone track should report `MEDIA_ERROR`.

## Recovery and timeouts

Persist `clientInstanceId` in tab-scoped storage and reuse it after refresh. Reconnect STOMP,
register, query `/current`, and call `/resume`. Unless the existing peer connection was retained on
the same backend instance, the server increments the generation and both clients rebuild their peer
connections. Device transfer and mid-call media renegotiation are not supported.

Defaults are 45 seconds for ringing, 30 seconds for initial connection and recovery, a 30-second
participant lease, 10-second application heartbeats, and a one-second timeout sweep. Lifecycle and
signal commands recheck state under a database lock.

## Development harness and ICE

See `tools/call-client/README.md`. The harness demonstrates permissions, camera fallback, local and
remote media, private signaling, track toggles, termination, and browser-refresh recovery.

For same-machine testing an empty ICE-server list is sufficient. For separate networks, configure a
STUN service and an external TURN service through client runtime configuration. Browsers select the
usable candidate pair automatically. TURN credentials must not be committed.

## Deployment limits

The current simple broker and local session registry support one application instance. Lifecycle
state is durable, but live notifications are best effort; reconnecting clients reconcile through
REST and start a new generation when delivery is uncertain. JWTs are checked at STOMP connection
time, so clients reconnect with refreshed tokens.
