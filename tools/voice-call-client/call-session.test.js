import test from "node:test";
import assert from "node:assert/strict";
import { CallSession } from "./call-session.js";

test("drops duplicate and obsolete signals", () => {
  const session = new CallSession(2);
  const signal = { messageId: "one", generation: 2 };
  assert.equal(session.acceptSignal(signal), true);
  assert.equal(session.acceptSignal(signal), false);
  assert.equal(session.acceptSignal({ messageId: "old", generation: 1 }), false);
});

test("new generation clears buffered candidates and deduplication", () => {
  const session = new CallSession(1);
  session.bufferCandidate({ candidate: "candidate" });
  session.acceptSignal({ messageId: "one", generation: 1 });
  assert.equal(session.replaceGeneration(2), true);
  assert.deepEqual(session.drainCandidates(), []);
  assert.equal(session.acceptSignal({ messageId: "one", generation: 2 }), true);
});
