package dev.amir.synapse.messaging.application.command.create_channel_room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.application.api.user_lookup.UserLookupResult;
import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelCommand;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateChannelHandlerTest {

  @Test
  void validatesInitialMembersWithBulkLookup() {
    var roomPort = mock(SaveRoomPort.class);
    var lookupUseCase = mock(UserLookupUseCase.class);
    var handler = new CreateChannelHandler(roomPort, lookupUseCase);
    var creator = UUID.randomUUID();
    var firstMember = UUID.randomUUID();
    var secondMember = UUID.randomUUID();
    var extraMembers = Set.of(firstMember, secondMember);
    var command = new CreateChannelCommand(creator, "Announcements", null, extraMembers);

    when(lookupUseCase.existsByUserId(creator)).thenReturn(true);
    when(lookupUseCase.getUsersByIds(extraMembers))
        .thenReturn(
            Map.of(
                firstMember, lookupResult(firstMember), secondMember, lookupResult(secondMember)));
    when(roomPort.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = handler.handle(command);

    assertThat(response.memberCount()).isEqualTo(3);

    var roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomPort).save(roomCaptor.capture());
    var savedRoom = roomCaptor.getValue();
    assertThat(savedRoom.memberCount()).isEqualTo(3);
    assertThat(savedRoom.hasMember(MemberId.of(firstMember))).isTrue();
    assertThat(savedRoom.hasMember(MemberId.of(secondMember))).isTrue();

    verify(lookupUseCase).existsByUserId(creator);
    verify(lookupUseCase).getUsersByIds(extraMembers);
    verify(lookupUseCase, never()).existsByUserId(firstMember);
    verify(lookupUseCase, never()).existsByUserId(secondMember);
  }

  private static UserLookupResult lookupResult(UUID userId) {
    return new UserLookupResult(userId, "User " + userId, null);
  }
}
