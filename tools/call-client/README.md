# Voice and video call development harness

Run `npm install`, then `npm run serve`. Configure
`SYNAPSE_WEBSOCKET_ALLOWED_ORIGINS=http://localhost:5173`, open the page in two browser profiles,
and paste a different Synapse access token into each page.

Select Voice or Video before starting. Video requests camera and microphone access, then falls back
to audio-only participation if the camera is unavailable. Microphone access is required. Incoming
calls request media only after the recipient selects Accept.

The harness keeps its browser-instance identifier and development access token in tab-scoped
`sessionStorage` so refresh can exercise Call recovery. This is a development convenience, not a
production credential-storage recommendation.

Set `sessionStorage.iceServers` before connecting when testing STUN/TURN, using a JSON array of
standard `RTCIceServer` objects. TURN credentials are deployment secrets and must not be committed.
