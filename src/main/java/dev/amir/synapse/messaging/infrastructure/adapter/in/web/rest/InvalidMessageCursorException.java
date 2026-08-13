package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import java.io.Serial;

final class InvalidMessageCursorException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  InvalidMessageCursorException() {
    super("The message history cursor is invalid");
  }

  InvalidMessageCursorException(Throwable cause) {
    super("The message history cursor is invalid", cause);
  }
}
