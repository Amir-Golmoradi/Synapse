export class CallSession {
  constructor(generation = 0) {
    this.generation = generation;
    this.pendingCandidates = [];
    this.seenSignals = new Set();
  }

  replaceGeneration(generation) {
    if (generation < this.generation) return false;
    if (generation > this.generation) {
      this.generation = generation;
      this.pendingCandidates = [];
      this.seenSignals.clear();
    }
    return true;
  }

  acceptSignal(signal) {
    if (signal.generation !== this.generation || this.seenSignals.has(signal.messageId)) {
      return false;
    }
    this.seenSignals.add(signal.messageId);
    return true;
  }

  bufferCandidate(candidate) {
    if (this.pendingCandidates.length >= 256) {
      throw new Error("Too many buffered ICE candidates");
    }
    this.pendingCandidates.push(candidate);
  }

  drainCandidates() {
    return this.pendingCandidates.splice(0);
  }
}
