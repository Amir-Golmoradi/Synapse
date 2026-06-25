package dev.amir.synapse.messaging.domain.policy;

import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.util.Set;

public final class RoomGuards {
  private static final int DIRECT_MESSAGE_PARTICIPANT_COUNT = 2;
  private static final int MAX_GROUP_CAPACITY = 2000;

  private RoomGuards() {}

  public static void validateCreation(
      RoomType roomType, String name, String avatarUrl, Set<MemberId> members) {
    if (members == null || members.isEmpty()) {
      throw new RoomValidationException("Room must have at least one member.");
    }
    if (roomType.equals(RoomType.DIRECT)) {
      if (members.size() != DIRECT_MESSAGE_PARTICIPANT_COUNT) {
        throw new RoomValidationException(
            "Direct messages must have exactly %d participants."
                .formatted(DIRECT_MESSAGE_PARTICIPANT_COUNT));
      }
      if (name != null && !name.isBlank()) {
        throw new RoomValidationException("Direct messages cannot have a name.");
      }
      if (avatarUrl != null && !avatarUrl.isBlank()) {
        throw new RoomValidationException("Direct messages cannot have an avatar.");
      }
    }
    if ((roomType.equals(RoomType.GROUP) || roomType.equals(RoomType.CHANNEL))
        && (name == null || name.isBlank())) {
      throw new RoomValidationException("Groups and Channels must have a name.");
    }
  }

  public static void validateCanAddMember(RoomType roomType, int currentParticipantCount) {
    if (roomType.equals(RoomType.DIRECT)) {
      throw new RoomValidationException("Cannot add members to a direct message.");
    }
    if (roomType.equals(RoomType.GROUP) && currentParticipantCount >= MAX_GROUP_CAPACITY) {
      throw new RoomValidationException("Group has reached the maximum number of members.");
    }
  }
}
