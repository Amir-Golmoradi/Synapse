# Voice messages

Voice messages are immutable, asynchronous messages owned by the Messaging bounded context. They
are not calls: recording and playback use normal client media APIs and authenticated HTTP, not
WebRTC signaling or peer-to-peer media.

## Client flow

1. Connect to `/ws`, authenticate the STOMP `CONNECT` frame, and subscribe to
   `/topic/rooms/{roomId}` as for text messages.
2. Select a supported recorder output: `audio/webm`, `audio/ogg`, or `audio/mp4`.
3. Stop recording before five minutes and keep the payload below 16 MiB.
4. Generate one `clientMessageId` and reuse it for every retry of the same recording.
5. Upload a multipart request to `POST /api/v1/room/{roomId}/messages/voice` with the bearer token.
6. Reconcile the returned and STOMP-delivered canonical message by `messageId` and
   `clientMessageId`.
7. Fetch audio from `GET /api/v1/room/{roomId}/messages/{messageId}/media` with the bearer token.

The multipart request has an `application/json` part named `metadata`:

```json
{
  "clientMessageId": "uuid",
  "durationMs": 18750
}
```

The binary part is named `audio` and must declare its audio MIME type. A newly created upload
returns `201`; an exact idempotent replay returns `200`. Reusing the client message ID with
different audio or metadata returns `409`.

## Canonical message

```json
{
  "messageId": "uuid",
  "roomId": "uuid",
  "senderId": "uuid",
  "clientMessageId": "uuid",
  "type": "VOICE",
  "text": null,
  "voice": {
    "durationMs": 18750,
    "mimeType": "audio/ogg",
    "sizeBytes": 241380
  },
  "createdAt": "2026-09-12T12:00:00Z"
}
```

Storage keys, paths, filenames, and checksums are private server metadata. Room membership is
checked for every upload and playback request. Removed users cannot continue playing media merely
because they know a message ID.

## Playback and recovery

The media endpoint accepts one `Range: bytes=...` range and returns `206 Partial Content` for
seeking. Browser clients using bearer authentication can fetch the response into a Blob and play a
Blob URL. Invalid or multiple ranges return `416`.

Live delivery remains best effort. After a disconnect, use the existing room-history endpoint and
deduplicate results by `messageId`. Audio is stored outside PostgreSQL and outside public static
resources. The database stores only message and media metadata.

## Operational limits

- The MVP filesystem adapter requires one application instance and a durable mounted volume.
- Valid media is retained while its message exists.
- Partial files older than one hour and unreferenced files older than 24 hours are cleaned.
- The server performs bounded structural format inspection but does not transcode or fully decode
  uploaded media.
- Production multi-instance deployment should replace the filesystem adapter with shared object
  storage and add a system-wide transactional delivery outbox.
