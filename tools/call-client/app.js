import { CallSession } from "./call-session.js";
import { acquireCallMedia, toggleTrack } from "./media-policy.js";
import { createMediaPeer } from "./webrtc-peer.js";

const ids = [
  "httpUrl", "wsUrl", "token", "calleeId", "mediaType", "connect", "start", "accept",
  "reject", "end", "toggleAudio", "toggleVideo", "localVideo", "remoteVideo", "log",
];
const elements = Object.fromEntries(ids.map(id => [id, document.getElementById(id)]));
const terminalStatuses = ["REJECTED", "CANCELLED", "MISSED", "ENDED", "FAILED"];
const clientInstanceId = sessionStorage.clientInstanceId ?? crypto.randomUUID();
sessionStorage.clientInstanceId = clientInstanceId;
elements.token.value = sessionStorage.accessToken ?? "";

let stomp;
let call;
let session = new CallSession();
let localStream;
let media;
let localDescriptionId;
let localDescriptionPublished = false;
let pendingLocalCandidates = [];
let disconnectedTimer;

function log(message, value) {
  elements.log.textContent += `${new Date().toISOString()} ${message}${value ? ` ${JSON.stringify(value)}` : ""}\n`;
  elements.log.scrollTop = elements.log.scrollHeight;
}

async function api(path, options = {}) {
  const response = await fetch(`${elements.httpUrl.value}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${elements.token.value}`,
      "Content-Type": "application/json",
      ...options.headers,
    },
  });
  if (!response.ok) throw new Error(`${response.status}: ${await response.text()}`);
  return response.status === 204 ? null : response.json();
}

function publish(destination, body) {
  stomp.publish({ destination, body: JSON.stringify(body) });
}

async function acquireMedia(mediaType) {
  if (localStream) return;
  const result = await acquireCallMedia(mediaType);
  localStream = result.stream;
  if (result.cameraFallback) log("camera unavailable; continuing audio-only");
  updateButtons();
}

async function ensurePeer(rebuild = false) {
  if (media && !rebuild) { control("READY"); return; }
  if (media) stopPeer();
  await acquireMedia(call.mediaType);
  localDescriptionId = null;
  localDescriptionPublished = false;
  pendingLocalCandidates = [];
  media = createMediaPeer({
    iceServers: JSON.parse(sessionStorage.iceServers ?? "[]"),
    localStream,
    localVideo: elements.localVideo,
    remoteVideo: elements.remoteVideo,
    onCandidate(candidate) {
      if (!localDescriptionPublished) pendingLocalCandidates.push(candidate);
      else sendCandidate(candidate);
    },
    onConnectionState(state) {
      log("peer connection", state);
      if (state === "connected") {
        clearTimeout(disconnectedTimer);
        control("CONNECTED");
      } else if (state === "disconnected") {
        clearTimeout(disconnectedTimer);
        disconnectedTimer = setTimeout(
          () => control("RECOVERY_REQUIRED", crypto.randomUUID(), false), 5000,
        );
      } else if (state === "failed") {
        clearTimeout(disconnectedTimer);
        control("RECOVERY_REQUIRED", crypto.randomUUID(), false);
      }
    },
    onTrackEnded(kind) {
      log(`${kind} track ended`);
      if (kind === "audio") endWithReason("MEDIA_ERROR").catch(error => log("end error", error.message));
      updateButtons();
    },
  });
  control("READY");
}

function sendCandidate(candidate) {
  publish(`/app/calls/${call.callId}/signals`, {
    clientInstanceId,
    signal: {
      messageId: crypto.randomUUID(), generation: session.generation, type: "ICE_CANDIDATE",
      candidate: candidate?.candidate ?? null, sdpMid: candidate?.sdpMid ?? null,
      sdpMLineIndex: candidate?.sdpMLineIndex ?? null,
      usernameFragment: candidate?.usernameFragment ?? null, descriptionId: localDescriptionId,
    },
  });
}

function markDescriptionPublished() {
  localDescriptionPublished = true;
  pendingLocalCandidates.splice(0).forEach(sendCandidate);
}

function control(type, requestId = null, peerConnectionRetained = false) {
  if (!call) return;
  publish(`/app/calls/${call.callId}/control`, {
    clientInstanceId, generation: session.generation, type, requestId, peerConnectionRetained,
  });
}

async function handleSignal(envelope) {
  const signal = envelope.signal;
  if (!call || envelope.callId !== call.callId || !session.acceptSignal(signal)) return;
  if (signal.type === "OFFER") {
    await media.peer.setRemoteDescription({ type: "offer", sdp: signal.sdp });
    for (const candidate of session.drainCandidates()) await media.peer.addIceCandidate(candidate);
    const answer = await media.peer.createAnswer();
    localDescriptionId = crypto.randomUUID();
    await media.peer.setLocalDescription(answer);
    publish(`/app/calls/${call.callId}/signals`, {
      clientInstanceId,
      signal: {
        messageId: localDescriptionId, generation: session.generation, type: "ANSWER",
        sdp: answer.sdp, descriptionId: signal.messageId,
      },
    });
    markDescriptionPublished();
  } else if (signal.type === "ANSWER") {
    await media.peer.setRemoteDescription({ type: "answer", sdp: signal.sdp });
    for (const candidate of session.drainCandidates()) await media.peer.addIceCandidate(candidate);
  } else {
    const candidate = signal.candidate === null ? null : {
      candidate: signal.candidate, sdpMid: signal.sdpMid,
      sdpMLineIndex: signal.sdpMLineIndex, usernameFragment: signal.usernameFragment,
    };
    if (media.peer.remoteDescription) await media.peer.addIceCandidate(candidate);
    else session.bufferCandidate(candidate);
  }
}

async function handleEvent(event) {
  log("event", event);
  if (event.type === "CLIENT_READY") {
    elements.start.disabled = false;
    const current = await api("/api/v1/calls/current");
    if (current) {
      call = await api(`/api/v1/calls/${current.callId}/resume`, {
        method: "POST",
        body: JSON.stringify({
          clientInstanceId, requestId: crypto.randomUUID(),
          observedGeneration: current.negotiationGeneration, peerConnectionRetained: false,
        }),
      });
      session.replaceGeneration(call.negotiationGeneration);
      if (["CONNECTING", "ACTIVE", "RECOVERING"].includes(call.status)) await ensurePeer();
    }
  } else if (["CALL_INCOMING", "CALL_ACCEPTED", "CALL_RECOVERING"].includes(event.type)) {
    call = event.call;
    elements.mediaType.value = call.mediaType;
    session.replaceGeneration(call.negotiationGeneration);
    if (event.type === "CALL_ACCEPTED") await ensurePeer();
    if (event.type === "CALL_RECOVERING") await ensurePeer(true);
  } else if (event.type === "NEGOTIATION_READY" && event.callerOffers) {
    const offer = await media.peer.createOffer();
    localDescriptionId = crypto.randomUUID();
    await media.peer.setLocalDescription(offer);
    publish(`/app/calls/${call.callId}/signals`, {
      clientInstanceId,
      signal: {
        messageId: localDescriptionId, generation: session.generation,
        type: "OFFER", sdp: offer.sdp,
      },
    });
    markDescriptionPublished();
  } else if (event.call) {
    call = event.call;
    if (terminalStatuses.includes(call.status)) stopMedia();
  }
  updateButtons();
}

function currentUserId() {
  try {
    const encoded = elements.token.value.split(".")[1].replaceAll("-", "+").replaceAll("_", "/");
    return JSON.parse(atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "="))).sub;
  } catch { return null; }
}

function stopPeer() {
  clearTimeout(disconnectedTimer);
  if (media) {
    media.peer.close();
    elements.localVideo.srcObject = null;
    elements.remoteVideo.srcObject = null;
  }
  media = null;
}

function stopMedia() {
  clearTimeout(disconnectedTimer);
  if (media) media.stop();
  else localStream?.getTracks().forEach(track => track.stop());
  media = null;
  localStream = null;
  elements.localVideo.srcObject = null;
  elements.remoteVideo.srcObject = null;
  localDescriptionId = null;
  localDescriptionPublished = false;
  pendingLocalCandidates = [];
}

function updateButtons() {
  const ringing = call?.status === "RINGING";
  const callee = call?.calleeId === currentUserId();
  const terminal = !call || terminalStatuses.includes(call.status);
  elements.start.disabled = !stomp?.connected || !terminal;
  elements.mediaType.disabled = !terminal;
  elements.accept.disabled = !(ringing && callee);
  elements.reject.disabled = !(ringing && callee);
  elements.end.disabled = terminal;
  elements.toggleAudio.disabled = !localStream?.getAudioTracks().length;
  elements.toggleVideo.disabled = !localStream?.getVideoTracks().length;
}

async function endWithReason(reason) {
  if (!call || terminalStatuses.includes(call.status)) return;
  call = await api(`/api/v1/calls/${call.callId}/end`, {
    method: "POST", body: JSON.stringify({ reason }),
  });
  stopMedia();
  updateButtons();
}

elements.connect.onclick = () => {
  sessionStorage.accessToken = elements.token.value;
  stomp = new StompJs.Client({
    brokerURL: elements.wsUrl.value,
    connectHeaders: { Authorization: `Bearer ${elements.token.value}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect() {
      stomp.subscribe("/user/queue/calls/events", frame => handleEvent(JSON.parse(frame.body)).catch(error => log("event error", error.message)));
      stomp.subscribe("/user/queue/calls/signals", frame => handleSignal(JSON.parse(frame.body)).catch(error => log("signal error", error.message)));
      stomp.subscribe("/user/queue/calls/errors", frame => log("call error", JSON.parse(frame.body)));
      publish("/app/calls/client-ready", { clientInstanceId });
    },
    onStompError: frame => log("STOMP error", frame.headers.message),
    onWebSocketClose: event => log("WebSocket closed", { code: event.code }),
  });
  stomp.activate();
};

elements.start.onclick = async () => {
  try {
    const mediaType = elements.mediaType.value;
    await acquireMedia(mediaType);
    call = await api("/api/v1/calls", {
      method: "POST",
      body: JSON.stringify({
        calleeId: elements.calleeId.value, clientRequestId: crypto.randomUUID(),
        clientInstanceId, mediaType,
      }),
    });
    session = new CallSession(call.negotiationGeneration);
    updateButtons();
  } catch (error) { stopMedia(); log("start failed", error.message); }
};

elements.accept.onclick = async () => {
  try {
    await acquireMedia(call.mediaType);
    call = await api(`/api/v1/calls/${call.callId}/accept`, {
      method: "POST", body: JSON.stringify({ clientInstanceId }),
    });
    session.replaceGeneration(call.negotiationGeneration);
    await ensurePeer();
    updateButtons();
  } catch (error) { stopMedia(); log("accept failed", error.message); }
};
elements.reject.onclick = async () => {
  call = await api(`/api/v1/calls/${call.callId}/reject`, { method: "POST" }); updateButtons();
};
elements.end.onclick = () => endWithReason("HANGUP").catch(error => log("end error", error.message));
elements.toggleAudio.onclick = () => {
  const enabled = toggleTrack(localStream, "audio");
  elements.toggleAudio.textContent = enabled ? "Mute" : "Unmute";
};
elements.toggleVideo.onclick = () => {
  const enabled = toggleTrack(localStream, "video");
  elements.toggleVideo.textContent = enabled ? "Camera off" : "Camera on";
};

setInterval(() => {
  if (stomp?.connected && call && !terminalStatuses.includes(call.status)) control("HEARTBEAT");
}, 10000);
