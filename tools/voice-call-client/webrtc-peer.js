export async function createAudioPeer({ iceServers, remoteAudio, onCandidate, onConnected, onFailed }) {
  const localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
  const peer = new RTCPeerConnection({ iceServers });
  localStream.getTracks().forEach(track => peer.addTrack(track, localStream));
  peer.ontrack = event => { remoteAudio.srcObject = event.streams[0]; };
  peer.onicecandidate = event => onCandidate(event.candidate);
  peer.onconnectionstatechange = () => {
    if (peer.connectionState === "connected") onConnected();
    if (peer.connectionState === "failed") onFailed();
  };
  return {
    peer,
    stop() {
      peer.close();
      localStream.getTracks().forEach(track => track.stop());
      remoteAudio.srcObject = null;
    },
  };
}
