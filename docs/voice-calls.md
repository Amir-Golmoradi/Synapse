# Voice calls

Synapse coordinates one-to-one WebRTC audio calls. It authenticates participants, persists the call
lifecycle, routes signaling messages, and enforces one nonterminal call per user. Audio flows between
the WebRTC peers or through an external TURN relay; it never passes through the Synapse application.

## Lifecycle API

All endpoints require `Authorization: Bearer <synapse-access-token>`.

| Method and path | Purpose |
|---|---|
| `POST /api/v1/calls` | Start a call with `calleeId`, `clientRequestId`, and `clientInstanceId`. |
| `POST /api/v1/calls/{id}/accept` | Accept from the first owning callee browser instance. |
| `POST /api/v1/calls/{id}/reject` | Reject a ringing call. |
| `POST /api/v1/calls/{id}/end` | End with `HANGUP` or report `MEDIA_ERROR`. |
| `POST /api/v1/calls/{id}/resume` | Recover the same call after refresh or signaling loss. |
| `GET /api/v1/calls/{id}` | Read an authorized call snapshot. |
| `GET /api/v1/calls/current` | Reconcile the authenticated user's nonterminal call. |

Retry an uncertain start with the same `clientRequestId` and identical callee/browser instance.
Changing the request under the same key returns `CALL_IDEMPOTENCY_CONFLICT`.

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
Callers offer and callees answer. Signals are routed only to the counterpart's owning session.
The server retains only offer/answer IDs and hashes plus bounded candidate counts for the active
generation; SDP and candidate payloads are never stored.

Send `READY`, `CONNECTED`, `HEARTBEAT`, and `RECOVERY_REQUIRED` control messages to
`/app/calls/{callId}/control`. The server asks the original caller to offer only after both owners
are ready. Clients must discard obsolete generations and deduplicate signal IDs.

## Recovery and timeouts

Persist `clientInstanceId` in tab-scoped storage and reuse it after refresh. Reconnect STOMP,
register, query `/current`, and call `/resume`. Unless the existing peer connection was retained on
the same backend instance, the server increments the generation and both clients rebuild their peer
connections. Device transfer is not supported.

Defaults are 45 seconds for ringing, 30 seconds for initial connection and recovery, a 30-second
participant lease, 10-second application heartbeats, and a one-second timeout sweep. Lifecycle
commands recheck state and deadlines under a database lock.

## Local audio harness

See `tools/voice-call-client/README.md`. The harness demonstrates microphone permission, private
signaling, two-way audio, termination, and browser-refresh recovery. Configure explicit localhost
CORS origins before running it.

For same-machine testing an empty ICE-server list is sufficient. For separate networks, configure a
STUN service and a real external TURN service. TURN credentials belong in runtime configuration and
must not be committed.

## Deployment limits

The current simple broker and local session registry support one application instance. Lifecycle
state is durable, but live notifications are best effort; reconnecting clients reconcile through
REST and start a new negotiation when signal delivery is uncertain. JWTs are checked at STOMP
connection time, so clients must reconnect with refreshed tokens.
