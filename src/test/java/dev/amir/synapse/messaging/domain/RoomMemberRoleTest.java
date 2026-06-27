package dev.amir.synapse.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoomMemberRoleTest {

  // Small helper: role of a given member in a room.
  private static RoomRole roleOf(Room room, MemberId id) {
    return room.getMembers().get(id).getRole();
  }

  @Nested
  class CreationAssignsRoles {

    @Test
    void groupCreatorBecomesOwner() {
      var creator = MemberId.generate();
      var room = Room.createGroupRoom(creator, "Engineering", null);

      assertThat(roleOf(room, creator)).isEqualTo(RoomRole.OWNER);
    }

    @Test
    void channelCreatorBecomesOwner() {
      var creator = MemberId.generate();
      var room = Room.createChannel(creator, "Announcements", null);

      assertThat(roleOf(room, creator)).isEqualTo(RoomRole.OWNER);
    }

    @Test
    void directRoomParticipantsAreBothMembersWithNoOwner() {
      var a = MemberId.generate();
      var b = MemberId.generate();
      var room = Room.createDirectRoom(a, b);

      assertThat(roleOf(room, a)).isEqualTo(RoomRole.MEMBER);
      assertThat(roleOf(room, b)).isEqualTo(RoomRole.MEMBER);
    }

    @Test
    void addedMemberJoinsAsPlainMember() {
      var owner = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      var added = MemberId.generate();

      room.addMember(added);

      assertThat(roleOf(room, added)).isEqualTo(RoomRole.MEMBER);
    }
  }

  @Nested
  class OwnerChangesRoles {

    @Test
    void ownerPromotesMemberToAdmin() { // Rule 4
      var owner = MemberId.generate();
      var member = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(member);

      room.changeMemberRole(owner, member, RoomRole.ADMIN);

      assertThat(roleOf(room, member)).isEqualTo(RoomRole.ADMIN);
    }

    @Test
    void ownerDemotesAdminToMember() { // Rule 6
      var owner = MemberId.generate();
      var member = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(member);
      room.changeMemberRole(owner, member, RoomRole.ADMIN);

      room.changeMemberRole(owner, member, RoomRole.MEMBER);

      assertThat(roleOf(room, member)).isEqualTo(RoomRole.MEMBER);
    }

    @Test
    void promotingAdminToOwnerDemotesOldOwnerToAdmin() { // Rule 5 — the single-owner swap
      var owner = MemberId.generate();
      var admin = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(admin);
      room.changeMemberRole(owner, admin, RoomRole.ADMIN);

      room.changeMemberRole(owner, admin, RoomRole.OWNER);

      // the new owner holds OWNER
      assertThat(roleOf(room, admin)).isEqualTo(RoomRole.OWNER);
      // the old owner is demoted to ADMIN — never two owners
      assertThat(roleOf(room, owner)).isEqualTo(RoomRole.ADMIN);
      // exactly one OWNER remains
      long owners =
          room.getMembers().values().stream().filter(m -> m.getRole() == RoomRole.OWNER).count();
      assertThat(owners).isEqualTo(1);
    }
  }

  @Nested
  class RoleChangeRejections {

    @Test
    void nonOwnerCannotChangeRoles() {
      var owner = MemberId.generate();
      var admin = MemberId.generate();
      var member = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(admin);
      room.addMember(member);
      room.changeMemberRole(owner, admin, RoomRole.ADMIN);

      // an ADMIN (not the owner) tries to promote a member -> rejected (rule 7 deferred)
      assertThatThrownBy(() -> room.changeMemberRole(admin, member, RoomRole.ADMIN))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("owner");
    }

    @Test
    void ownerCannotChangeOwnRole() {
      var owner = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);

      assertThatThrownBy(() -> room.changeMemberRole(owner, owner, RoomRole.ADMIN))
          .isInstanceOf(RoomValidationException.class);
    }

    @Test
    void cannotPromoteMemberDirectlyToOwner() { // must go through ADMIN first (rule 5)
      var owner = MemberId.generate();
      var member = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(member);

      assertThatThrownBy(() -> room.changeMemberRole(owner, member, RoomRole.OWNER))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("ADMIN");
    }

    @Test
    void cannotPromoteNonMemberToAdmin() { // target must be a MEMBER currently
      var owner = MemberId.generate();
      var admin = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);
      room.addMember(admin);
      room.changeMemberRole(owner, admin, RoomRole.ADMIN);

      // already ADMIN -> promoting to ADMIN again is rejected (no-op role)
      assertThatThrownBy(() -> room.changeMemberRole(owner, admin, RoomRole.ADMIN))
          .isInstanceOf(RoomValidationException.class);
    }

    @Test
    void targetMustBeAMember() {
      var owner = MemberId.generate();
      var stranger = MemberId.generate();
      var room = Room.createGroupRoom(owner, "Engineering", null);

      assertThatThrownBy(() -> room.changeMemberRole(owner, stranger, RoomRole.ADMIN))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("Target");
    }

    @Test
    void directRoomsRejectRoleChanges() {
      var a = MemberId.generate();
      var b = MemberId.generate();
      var room = Room.createDirectRoom(a, b);

      assertThatThrownBy(() -> room.changeMemberRole(a, b, RoomRole.ADMIN))
          .isInstanceOf(RoomValidationException.class)
          .hasMessageContaining("Direct");
    }
  }
}
