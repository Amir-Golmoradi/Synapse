package dev.amir.synapse.messaging.domain.port.in.send_message;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import java.util.UUID;

public record SendMessageCommand(UUID senderId, UUID roomId, UUID clientMessageId, String text) {
  public static final int MAX_TEXT_CODE_POINTS = 4096;
  private static final char NUL_CHARACTER = '\0';

  public SendMessageCommand {
    requireIdentifier(senderId, "Sender ID");
    requireIdentifier(roomId, "Room ID");
    requireIdentifier(clientMessageId, "Client message ID");
    validateText(text);
  }

  private static void requireIdentifier(UUID identifier, String name) {
    if (identifier == null) {
      throw new MessageValidationException(name + " cannot be null.");
    }
  }

  private static void validateText(String text) {
    if (text == null) {
      throw new MessageValidationException("Message text cannot be null.");
    }
    if (text.isBlank()) {
      throw new MessageValidationException("Message text cannot be blank.");
    }

    var codePointCount = 0;
    var index = 0;
    while (index < text.length()) {
      var character = text.charAt(index);
      if (character == NUL_CHARACTER) {
        throw new MessageValidationException("Message text cannot contain NUL characters.");
      }
      if (Character.isHighSurrogate(character)) {
        if (index + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(index + 1))) {
          throw new MessageValidationException("Message text contains malformed Unicode.");
        }
        index += 2;
      } else if (Character.isLowSurrogate(character)) {
        throw new MessageValidationException("Message text contains malformed Unicode.");
      } else {
        index++;
      }

      codePointCount++;
      if (codePointCount > MAX_TEXT_CODE_POINTS) {
        throw new MessageValidationException(
            "Message text cannot exceed " + MAX_TEXT_CODE_POINTS + " Unicode code points.");
      }
    }
  }
}
