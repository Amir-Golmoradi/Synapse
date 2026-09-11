# Voice call development harness

Run `npm install`, then `npm run serve`. Configure
`SYNAPSE_WEBSOCKET_ALLOWED_ORIGINS=http://localhost:5173`, open the page in two browser profiles,
and paste a different Synapse access token into each page.

The harness keeps its browser-instance identifier and development access token in the tab's
`sessionStorage` so a refresh can exercise Call recovery. This is a development convenience, not a
production credential-storage recommendation.

Set `sessionStorage.iceServers` in each browser before connecting when testing STUN/TURN, for
example a JSON array of standard `RTCIceServer` objects. No credentials are committed here.
