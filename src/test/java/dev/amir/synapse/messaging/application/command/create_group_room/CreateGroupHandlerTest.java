package dev.amir.synapse.messaging.application.command.create_group_room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.application.api.user_lookup.UserLookupResult;
import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import dev.amir.synapse.messaging.domain.event.MembersAddedEvent;
import dev.amir.synapse.messaging.domain.event.RoomCreatedEvent;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupCommand;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateGroupHandlerTest {

  @Test
  void savesGroupWithInitialMembersAddedAsOneBatch() {
    var roomPort = mock(SaveRoomPort.class);
    var lookupUseCase = mock(UserLookupUseCase.class);
    var handler = new CreateGroupHandler(roomPort, lookupUseCase);
    var creator = UUID.randomUUID();
    var firstMember = UUID.randomUUID();
    var secondMember = UUID.randomUUID();
    var extraMembers = Set.of(firstMember, secondMember);
    var command = new CreateGroupCommand(creator, "Engineering", null, extraMembers);

    when(lookupUseCase.existsByUserId(creator)).thenReturn(true);
    when(lookupUseCase.getUsersByIds(extraMembers))
        .thenReturn(
            Map.of(
                firstMember, lookupResult(firstMember), secondMember, lookupResult(secondMember)));
    when(roomPort.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = handler.handle(command);

    assertThat(response.members()).isEqualTo(3);

    var roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomPort).save(roomCaptor.capture());
    var savedRoom = roomCaptor.getValue();
    assertThat(savedRoom.memberCount()).isEqualTo(3);
    assertThat(savedRoom.hasMember(MemberId.of(firstMember))).isTrue();
    assertThat(savedRoom.hasMember(MemberId.of(secondMember))).isTrue();

    var events = savedRoom.pullDomainEvents();
    assertThat(events).filteredOn(RoomCreatedEvent.class::isInstance).hasSize(1);
    assertThat(events)
        .filteredOn(MembersAddedEvent.class::isInstance)
        .singleElement()
        .satisfies(
            event ->
                assertThat(((MembersAddedEvent) event).memberIds())
                    .containsExactlyInAnyOrder(
                        MemberId.of(firstMember), MemberId.of(secondMember)));
  }

  private static UserLookupResult lookupResult(UUID userId) {
    return new UserLookupResult(userId, "User " + userId, null);
  }
}
