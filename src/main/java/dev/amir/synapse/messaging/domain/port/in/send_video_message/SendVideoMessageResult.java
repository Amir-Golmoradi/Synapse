package dev.amir.synapse.messaging.domain.port.in.send_video_message;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;

public record SendVideoMessageResult(MessageView message, boolean created) {}
