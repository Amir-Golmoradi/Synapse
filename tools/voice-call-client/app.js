import { CallSession } from "./call-session.js";
import { createAudioPeer } from "./webrtc-peer.js";

const elements = Object.fromEntries(
  ["httpUrl", "wsUrl", "token", "calleeId", "connect", "start", "accept", "reject", "end", "remoteAudio", "log"]
    .map(id => [id, document.getElementById(id)]),
);
const clientInstanceId = sessionStorage.clientInstanceId ?? crypto.randomUUID();
sessionStorage.clientInstanceId = clientInstanceId;
elements.token.value = sessionStorage.accessToken ?? "";

let stomp;
let call;
let session = new CallSession();
let media;
let peerPromise;
let localDescriptionId;
let localDescriptionPublished = false;
let pendingLocalCandidates = [];

function log(message, value) {
  elements.log.textContent += `${new Date().toISOString()} ${message}${value ? ` ${JSON.stringify(value)}` : ""}\n`;
  elements.log.scrollTop = elements.log.scrollHeight;
}

async function api(path, options = {}) {
  const response = await fetch(`${elements.httpUrl.value}${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${elements.token.value}`, "Content-Type": "application/json", ...options.headers },
  });
  if (!response.ok) throw new Error(`${response.status}: ${await response.text()}`);
  return response.status === 204 ? null : response.json();
}

function publish(destination, body) {
  stomp.publish({ destination, body: JSON.stringify(body) });
}

async function ensurePeer(rebuild = false) {
  if (media && !rebuild) { control("READY"); return; }
  if (peerPromise && !rebuild) return peerPromise;
  if (media) media.stop();
  localDescriptionId = null;
  localDescriptionPublished = false;
  pendingLocalCandidates = [];
  peerPromise = createAudioPeer({
    iceServers: JSON.parse(sessionStorage.iceServers ?? "[]"),
    remoteAudio: elements.remoteAudio,
    onCandidate(candidate) {
      if (!localDescriptionPublished) pendingLocalCandidates.push(candidate);
      else sendCandidate(candidate);
    },
    onConnected: () => control("CONNECTED"),
    onFailed: () => control("RECOVERY_REQUIRED", crypto.randomUUID(), false),
  });
  media = await peerPromise;
  peerPromise = null;
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

async function requireMicrophonePermission() {
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
  stream.getTracks().forEach(track => track.stop());
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
      signal: { messageId: localDescriptionId, generation: session.generation, type: "ANSWER", sdp: answer.sdp, descriptionId: signal.messageId },
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
        body: JSON.stringify({ clientInstanceId, requestId: crypto.randomUUID(), observedGeneration: current.negotiationGeneration, peerConnectionRetained: false }),
      });
      session.replaceGeneration(call.negotiationGeneration);
      updateButtons();
      if (["CONNECTING", "ACTIVE", "RECOVERING"].includes(call.status)) await ensurePeer();
    }
  } else if (event.type === "CALL_INCOMING" || event.type === "CALL_ACCEPTED" || event.type === "CALL_RECOVERING") {
    call = event.call;
    session.replaceGeneration(call.negotiationGeneration);
    updateButtons();
    if (event.type === "CALL_ACCEPTED") await ensurePeer();
    if (event.type === "CALL_RECOVERING") await ensurePeer(true);
  } else if (event.type === "NEGOTIATION_READY" && event.callerOffers) {
    const offer = await media.peer.createOffer();
    localDescriptionId = crypto.randomUUID();
    await media.peer.setLocalDescription(offer);
    publish(`/app/calls/${call.callId}/signals`, {
      clientInstanceId,
      signal: { messageId: localDescriptionId, generation: session.generation, type: "OFFER", sdp: offer.sdp },
    });
    markDescriptionPublished();
  } else if (event.call) {
    call = event.call;
    if (["REJECTED", "CANCELLED", "MISSED", "ENDED", "FAILED"].includes(call.status)) stopMedia();
    updateButtons();
  }
}

function currentUserId() {
  try {
    const encoded = elements.token.value.split(".")[1].replaceAll("-", "+").replaceAll("_", "/");
    return JSON.parse(atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "="))).sub;
  }
  catch { return null; }
}

function stopMedia() {
  if (media) media.stop();
  media = null;
  peerPromise = null;
  localDescriptionId = null;
  localDescriptionPublished = false;
  pendingLocalCandidates = [];
}
function updateButtons() {
  const ringing = call?.status === "RINGING";
  const callee = call?.calleeId === currentUserId();
  elements.accept.disabled = !(ringing && callee);
  elements.reject.disabled = !(ringing && callee);
  elements.end.disabled = !call || ["REJECTED", "CANCELLED", "MISSED", "ENDED", "FAILED"].includes(call.status);
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
  });
  stomp.activate();
};

elements.start.onclick = async () => {
  await requireMicrophonePermission();
  call = await api("/api/v1/calls", { method: "POST", body: JSON.stringify({ calleeId: elements.calleeId.value, clientRequestId: crypto.randomUUID(), clientInstanceId }) });
  session = new CallSession(call.negotiationGeneration);
  updateButtons();
};
elements.accept.onclick = async () => {
  await requireMicrophonePermission();
  call = await api(`/api/v1/calls/${call.callId}/accept`, { method: "POST", body: JSON.stringify({ clientInstanceId }) });
  session.replaceGeneration(call.negotiationGeneration); updateButtons(); await ensurePeer();
};
elements.reject.onclick = async () => { call = await api(`/api/v1/calls/${call.callId}/reject`, { method: "POST" }); updateButtons(); };
elements.end.onclick = async () => { call = await api(`/api/v1/calls/${call.callId}/end`, { method: "POST", body: JSON.stringify({ reason: "HANGUP" }) }); stopMedia(); updateButtons(); };

setInterval(() => { if (stomp?.connected && call && !["REJECTED", "CANCELLED", "MISSED", "ENDED", "FAILED"].includes(call.status)) control("HEARTBEAT"); }, 10000);
