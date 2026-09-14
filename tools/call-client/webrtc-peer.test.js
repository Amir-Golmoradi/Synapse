import test from "node:test";
import assert from "node:assert/strict";
import { createMediaPeer } from "./webrtc-peer.js";

test("adds audio and video tracks and stops them on cleanup", () => {
  const events = {};
  const tracks = ["audio", "video"].map(kind => ({
    kind,
    stopped: false,
    addEventListener(name, handler) { events[`${kind}:${name}`] = handler; },
    stop() { this.stopped = true; },
  }));
  const added = [];
  const peer = {
    addTrack(track) { added.push(track); },
    close() { this.closed = true; },
  };
  const localVideo = {};
  const remoteVideo = {};
  const media = createMediaPeer({
    iceServers: [],
    localStream: { getTracks: () => tracks },
    localVideo,
    remoteVideo,
    onCandidate() {},
    onConnectionState() {},
    onTrackEnded() {},
    createPeer: () => peer,
  });

  assert.deepEqual(added, tracks);
  assert.ok(events["audio:ended"]);
  assert.ok(events["video:ended"]);
  media.stop();
  assert.equal(peer.closed, true);
  assert.ok(tracks.every(track => track.stopped));
  assert.equal(localVideo.srcObject, null);
  assert.equal(remoteVideo.srcObject, null);
});
