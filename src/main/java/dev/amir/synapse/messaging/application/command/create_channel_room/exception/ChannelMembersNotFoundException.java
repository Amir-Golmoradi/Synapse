package dev.amir.synapse.messaging.application.command.create_channel_room.exception;

import java.util.Set;
import java.util.UUID;

public class ChannelMembersNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ChannelMembersNotFoundException(Set<UUID> missingMemberIds) {
    super("The following channel members were not found: " + missingMemberIds);
  }
}
