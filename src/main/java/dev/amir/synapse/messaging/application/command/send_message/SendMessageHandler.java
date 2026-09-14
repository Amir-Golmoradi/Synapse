package dev.amir.synapse.messaging.application.command.send_message;

import dev.amir.synapse.messaging.application.service.MessageNotifications;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_message.SendMessageUseCase;
import dev.amir.synapse.messaging.domain.port.out.MessageWritePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendMessageHandler implements SendMessageUseCase {
  private final MessageWritePort messageWritePort;
  private final MessageNotifications notifications;

  public SendMessageHandler(MessageWritePort messageWritePort, MessageNotifications notifications) {
    this.messageWritePort = messageWritePort;
    this.notifications = notifications;
  }

  @Override
  @Transactional
  public MessageView handle(SendMessageCommand command) {
    var message =
        messageWritePort.saveAuthorized(
            command.roomId(), command.senderId(), command.clientMessageId(), command.text());
    notifications.publish(message);
    return message;
  }
}
