export function createMediaPeer({
  iceServers,
  localStream,
  localVideo,
  remoteVideo,
  onCandidate,
  onConnectionState,
  onTrackEnded,
  createPeer = configuration => new RTCPeerConnection(configuration),
}) {
  const peer = createPeer({ iceServers });
  localStream.getTracks().forEach(track => {
    track.addEventListener("ended", () => onTrackEnded(track.kind));
    peer.addTrack(track, localStream);
  });
  localVideo.srcObject = localStream;
  peer.ontrack = event => { remoteVideo.srcObject = event.streams[0]; };
  peer.onicecandidate = event => onCandidate(event.candidate);
  peer.onconnectionstatechange = () => onConnectionState(peer.connectionState);
  return {
    peer,
    stop() {
      peer.close();
      localStream.getTracks().forEach(track => track.stop());
      localVideo.srcObject = null;
      remoteVideo.srcObject = null;
    },
  };
}
