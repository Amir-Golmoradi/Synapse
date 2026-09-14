package dev.amir.synapse.messaging.domain.port.in.send_message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendMessageCommandTest {

  @Test
  void preservesWhitespaceNewlinesAndSupplementaryCharactersExactly() {
    var text = "  Hello\nworld 😀  ";

    var command = command(text);

    assertThat(command.text()).isEqualTo(text);
  }

  @Test
  void acceptsTheMaximumNumberOfUnicodeCodePoints() {
    var text = "😀".repeat(SendMessageCommand.MAX_TEXT_CODE_POINTS);

    assertThat(command(text).text()).isSameAs(text);
  }

  @Test
  void rejectsTextBeyondTheUnicodeCodePointLimit() {
    var text = "😀".repeat(SendMessageCommand.MAX_TEXT_CODE_POINTS + 1);

    assertInvalid(text, "cannot exceed 4096 Unicode code points");
  }

  @Test
  void rejectsBlankNulAndMalformedUnicodeText() {
    assertInvalid(" \n\t ", "cannot be blank");
    assertInvalid("before\0after", "cannot contain NUL");
    assertInvalid("bad\uD83D", "malformed Unicode");
    assertInvalid("bad\uDE00", "malformed Unicode");
  }

  @Test
  void reportsMissingIdentifiersAsMessageValidationErrors() {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();

    assertThatThrownBy(() -> new SendMessageCommand(null, roomId, clientMessageId, "message"))
        .isInstanceOf(MessageValidationException.class)
        .hasMessageContaining("Sender ID");
    assertThatThrownBy(() -> new SendMessageCommand(senderId, null, clientMessageId, "message"))
        .isInstanceOf(MessageValidationException.class)
        .hasMessageContaining("Room ID");
    assertThatThrownBy(() -> new SendMessageCommand(senderId, roomId, null, "message"))
        .isInstanceOf(MessageValidationException.class)
        .hasMessageContaining("Client message ID");
  }

  private static SendMessageCommand command(String text) {
    return new SendMessageCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), text);
  }

  private static void assertInvalid(String text, String messageFragment) {
    assertThatThrownBy(() -> command(text))
        .isInstanceOf(MessageValidationException.class)
        .hasMessageContaining(messageFragment);
  }
}
