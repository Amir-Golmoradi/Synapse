package dev.amir.synapse.messaging.domain.policy;

import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import java.util.UUID;

public record RoomMembershipGuardPolicy(RoomId targetRoomId) {
  public void enforce(UUID userId, LoadRoomPort port) {
    if (!isSatisfiedBy(userId, port)) {
      throw new SecurityException(
          String.format(
              "Access Denied: User [%s] is not a member of Room [%s].", userId, targetRoomId));
    }
  }

  private boolean isSatisfiedBy(UUID userId, LoadRoomPort port) {
    return port.hasActiveMembership(targetRoomId.getValue(), userId);
  }
}
