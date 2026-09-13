package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;

public record MessagePersistenceResult(MessageView message, boolean created) {}
