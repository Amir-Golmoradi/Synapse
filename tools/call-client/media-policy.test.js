import test from "node:test";
import assert from "node:assert/strict";
import { acquireCallMedia, toggleTrack } from "./media-policy.js";

const stream = tracks => ({ getTracks: () => tracks });

test("voice requests microphone only", async () => {
  const requests = [];
  const audio = stream([{ kind: "audio", enabled: true }]);
  const result = await acquireCallMedia("VOICE", {
    async getUserMedia(constraints) { requests.push(constraints); return audio; },
  });
  assert.deepEqual(requests, [{ audio: true, video: false }]);
  assert.equal(result.stream, audio);
  assert.equal(result.cameraFallback, false);
});

test("video falls back to audio when camera acquisition fails", async () => {
  const requests = [];
  const audio = stream([{ kind: "audio", enabled: true }]);
  const result = await acquireCallMedia("VIDEO", {
    async getUserMedia(constraints) {
      requests.push(constraints);
      if (constraints.video) throw new Error("camera denied");
      return audio;
    },
  });
  assert.deepEqual(requests, [
    { audio: true, video: true },
    { audio: true, video: false },
  ]);
  assert.equal(result.cameraFallback, true);
});

test("microphone failure prevents joining", async () => {
  await assert.rejects(
    acquireCallMedia("VOICE", { async getUserMedia() { throw new Error("denied"); } }),
    /Microphone permission is required/,
  );
});

test("toggle changes an existing track without renegotiation", () => {
  const microphone = { kind: "audio", enabled: true };
  const media = stream([microphone]);
  assert.equal(toggleTrack(media, "audio"), false);
  assert.equal(microphone.enabled, false);
  assert.equal(toggleTrack(media, "video"), null);
});
