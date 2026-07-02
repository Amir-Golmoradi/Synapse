package dev.amir.synapse.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.event.MemberCreatedEvent;
import dev.amir.synapse.messaging.domain.event.MemberRemovedEvent;
import dev.amir.synapse.messaging.domain.event.RoomArchivedEvent;
import dev.amir.synapse.messaging.domain.event.RoomCreatedEvent;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoomDomainInvariantTest {
  private static final int GROUP_MAX_MEMBERS = 2000;

  private static Set<MemberId> memberIds(int count) {
    return IntStream.range(0, count).mapToObj(i -> MemberId.generate()).collect(Collectors.toSet());
  }

  private static RoomRole roleOf(Room room, MemberId memberId) {
    return room.getMembers().get(memberId).getRole();
  }

  @Nested
  class CreationInvariants {

    @Test
    void directRoomRequiresTwoDistinctParticipantsAndHasNoNameOrAvatar() {
      var creator = MemberId.generate();
      var recipient = MemberId.generate();

      var room = Room.createDirectRoom(creator, recipient);

      assertThat(room.getRoomType()).isEqualTo(RoomType.DIRECT);
      assertThat(room.getStatus()).isEqualTo(RoomStatus.ACTIVE);
      assertThat(room.getName()).isNull();
      assertThat(room.getAvatarUrl()).isNull();
      assertThat(room.getCreatedAt()).isNotNull();
      assertThat(room.getLastMessagesAt()).isEqualTo(room.getCreatedAt());
      assertThat(room.getVersion()).isNull();
      assertThat(room.getMembers()).containsOnlyKeys(creator, recipient);
      assertThat(roleOf(room, creator)).isEqualTo(RoomRole.MEMBER);
      assertThat(roleOf(room, recipient)).isEqualTo(RoomRole.MEMBER);
      assertThat(room.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              RoomCreatedEvent.class,
              event -> {
                assertThat(event.roomId()).isEqualTo(room.getId());
                assertThat(event.type()).isEqualTo(RoomType.DIRECT);
                assertThat(event.name()).isNull();
                assertThat(event.avatarUrl()).isNull();
              });
    }

    @Test
    void directRoomRejectsSelfConversation() {
      var user = MemberId.generate();

      assertThatThrownBy(() -> Room.createDirectRoom(user, user))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("same user");
    }

    @Test
    void groupCreationNormalizesNameAndAssignsOwner() {
      var creator = MemberId.generate();

      var room = Room.createGroupRoom(creator, "  Engineering  ", null);

      assertThat(room.getRoomType()).isEqualTo(RoomType.GROUP);
      assertThat(room.getName()).isEqualTo("Engineering");
      assertThat(room.getMembers()).containsOnlyKeys(creator);
      assertThat(roleOf(room, creator)).isEqualTo(RoomRole.OWNER);
      assertThat(room.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              RoomCreatedEvent.class, event -> assertThat(event.name()).isEqualTo("Engineering"));
    }

    @Test
    void channelCreationNormalizesNameAndAssignsOwner() {
      var creator = MemberId.generate();

      var room = Room.createChannel(creator, "\nAnnouncements\t", null);

      assertThat(room.getRoomType()).isEqualTo(RoomType.CHANNEL);
      assertThat(room.getName()).isEqualTo("Announcements");
      assertThat(room.getMembers()).containsOnlyKeys(creator);
      assertThat(roleOf(room, creator)).isEqualTo(RoomRole.OWNER);
      assertThat(room.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              RoomCreatedEvent.class, event -> assertThat(event.name()).isEqualTo("Announcements"));
    }

    @Test
    void groupAndChannelCreationRejectBlankNames() {
      var creator = MemberId.generate();

      assertThatThrownBy(() -> Room.createGroupRoom(creator, "   ", null))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("blank");
      assertThatThrownBy(() -> Room.createChannel(creator, "\t", null))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("blank");
    }
  }

  @Nested
  class ArchivedRoomInvariants {

    @Test
    void archiveMarksRoomReadOnlyAndEmitsArchiveEvent() {
      var owner = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.clearDomainEvents();

      room.archive();

      assertThat(room.getStatus()).isEqualTo(RoomStatus.ARCHIVED);
      assertThat(room.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              RoomArchivedEvent.class, event -> assertThat(event.roomId()).isEqualTo(room.getId()));
    }

    @Test
    void archivedRoomRejectsAllMutationsWithoutChangingState() {
      var owner = MemberId.generate();
      var member = MemberId.generate();
      var newcomer = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", "https://cdn.example/avatar.png");
      room.addMember(member);
      room.archive();
      room.clearDomainEvents();
      var lastMessagesAt = room.getLastMessagesAt();

      assertThatThrownBy(() -> room.addMember(newcomer))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(() -> room.addMembers(Set.of(newcomer)))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(() -> room.removeMember(member))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(() -> room.changeMemberRole(owner, member, RoomRole.ADMIN))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(() -> room.rename("Platform"))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(() -> room.changeAvatar("https://cdn.example/new-avatar.png"))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(room::recordMessageActivity)
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");
      assertThatThrownBy(room::archive)
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("archived");

      assertThat(room.getStatus()).isEqualTo(RoomStatus.ARCHIVED);
      assertThat(room.getName()).isEqualTo("Engineering");
      assertThat(room.getAvatarUrl()).isEqualTo("https://cdn.example/avatar.png");
      assertThat(room.getLastMessagesAt()).isEqualTo(lastMessagesAt);
      assertThat(room.getMembers()).containsOnlyKeys(owner, member);
      assertThat(roleOf(room, member)).isEqualTo(RoomRole.MEMBER);
      assertThat(room.pullDomainEvents()).isEmpty();
    }
  }

  @Nested
  class MemberMutationInvariants {

    @Test
    void addMemberAddsPlainMemberAndEmitsMemberCreatedEvent() {
      var owner = MemberId.generate();
      var newcomer = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.clearDomainEvents();

      room.addMember(newcomer);

      assertThat(room.getMembers()).containsKeys(owner, newcomer);
      assertThat(roleOf(room, newcomer)).isEqualTo(RoomRole.MEMBER);
      assertThat(room.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              MemberCreatedEvent.class,
              event -> {
                assertThat(event.roomId()).isEqualTo(room.getId());
                assertThat(event.memberId()).isEqualTo(newcomer);
              });
    }

    @Test
    void addMemberRejectsDuplicateWithoutChangingState() {
      var owner = MemberId.generate();
      var member = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(member);
      room.clearDomainEvents();

      assertThatThrownBy(() -> room.addMember(member))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("already a member");

      assertThat(room.getMembers()).containsOnlyKeys(owner, member);
      assertThat(room.pullDomainEvents()).isEmpty();
    }

    @Test
    void directRoomRejectsMemberAdditionsAndRemovals() {
      var first = MemberId.generate();
      var second = MemberId.generate();
      var room = Room.createDirectRoom(first, second);
      room.clearDomainEvents();

      assertThatThrownBy(() -> room.addMember(MemberId.generate()))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("direct message");
      assertThatThrownBy(() -> room.removeMember(first))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("Direct message");

      assertThat(room.getMembers()).containsOnlyKeys(first, second);
      assertThat(room.pullDomainEvents()).isEmpty();
    }

    @Test
    void removeMemberRemovesExistingMemberAndEmitsMemberRemovedEvent() {
      var owner = MemberId.generate();
      var member = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(member);
      room.clearDomainEvents();

      room.removeMember(member);

      assertThat(room.getMembers()).containsOnlyKeys(owner);
      assertThat(room.hasMember(member)).isFalse();
      assertThat(room.pullDomainEvents())
          .singleElement()
          .isInstanceOfSatisfying(
              MemberRemovedEvent.class,
              event -> {
                assertThat(event.roomId()).isEqualTo(room.getId());
                assertThat(event.memberId()).isEqualTo(member);
              });
    }

    @Test
    void removeMemberRejectsNonMemberWithoutChangingState() {
      var owner = MemberId.generate();
      var stranger = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.clearDomainEvents();

      assertThatThrownBy(() -> room.removeMember(stranger))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("not a member");

      assertThat(room.getMembers()).containsOnlyKeys(owner);
      assertThat(room.pullDomainEvents()).isEmpty();
    }

    @Test
    void removeMemberRejectsLastMemberWithoutChangingState() {
      var owner = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.clearDomainEvents();

      assertThatThrownBy(() -> room.removeMember(owner))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("last member");

      assertThat(room.getMembers()).containsOnlyKeys(owner);
      assertThat(roleOf(room, owner)).isEqualTo(RoomRole.OWNER);
      assertThat(room.pullDomainEvents()).isEmpty();
    }

    @Test
    void renameNormalizesName() {
      var room = Room.createGroupRoom(MemberId.generate(), "Engineering", null);

      room.rename("  Platform  ");

      assertThat(room.getName()).isEqualTo("Platform");
    }
  }

  @Nested
  class CapacityInvariants {

    @Test
    void groupAllowsExactlyMaximumCapacity() {
      var owner = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);

      room.addMembers(memberIds(GROUP_MAX_MEMBERS - 1));

      assertThat(room.memberCount()).isEqualTo(GROUP_MAX_MEMBERS);
    }

    @Test
    void groupRejectsMembersBeyondMaximumCapacityWithoutChangingState() {
      var owner = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMembers(memberIds(GROUP_MAX_MEMBERS - 1));
      room.clearDomainEvents();

      assertThatThrownBy(() -> room.addMember(MemberId.generate()))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("maximum");

      assertThat(room.memberCount()).isEqualTo(GROUP_MAX_MEMBERS);
      assertThat(room.pullDomainEvents()).isEmpty();
    }

    @Test
    void channelAllowsMoreMembersThanGroupCapacity() {
      var owner = MemberId.generate();
      var room = Room.createChannel(owner, "Announcements", null);

      assertThatCode(() -> room.addMembers(memberIds(GROUP_MAX_MEMBERS)))
          .doesNotThrowAnyException();

      assertThat(room.memberCount()).isEqualTo(GROUP_MAX_MEMBERS + 1);
    }
  }
}
