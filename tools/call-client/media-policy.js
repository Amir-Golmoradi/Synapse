export async function acquireCallMedia(mediaType, mediaDevices = navigator.mediaDevices) {
  if (mediaType === "VOICE") {
    return { stream: await requireAudio(mediaDevices, false), cameraFallback: false };
  }
  if (mediaType !== "VIDEO") throw new Error("Unsupported call media type");
  try {
    return {
      stream: await mediaDevices.getUserMedia({ audio: true, video: true }),
      cameraFallback: false,
    };
  } catch (cameraError) {
    try {
      return { stream: await requireAudio(mediaDevices, true), cameraFallback: true };
    } catch (microphoneError) {
      throw new AggregateError(
        [cameraError, microphoneError],
        "Microphone permission is required to join a call",
      );
    }
  }
}

async function requireAudio(mediaDevices, afterVideoFailure) {
  try {
    return await mediaDevices.getUserMedia({ audio: true, video: false });
  } catch (error) {
    if (afterVideoFailure) throw error;
    throw new Error("Microphone permission is required to join a call", { cause: error });
  }
}

export function toggleTrack(stream, kind) {
  const track = stream?.getTracks().find(candidate => candidate.kind === kind);
  if (!track) return null;
  track.enabled = !track.enabled;
  return track.enabled;
}
