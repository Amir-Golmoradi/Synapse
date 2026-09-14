package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageUseCase;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/room")
public class VideoMessageCommandApi {
  private final SendVideoMessageUseCase sendVideoMessage;

  public VideoMessageCommandApi(SendVideoMessageUseCase sendVideoMessage) {
    this.sendVideoMessage = sendVideoMessage;
  }

  @PostMapping(
      value = "/{roomId}/messages/video",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<dev.amir.synapse.messaging.domain.port.in.message.MessageView> send(
      @PathVariable UUID roomId,
      @RequestParam("clientMessageId") UUID clientMessageId,
      @RequestParam("video") MultipartFile video,
      Authentication authentication) {
    var result =
        sendVideoMessage.handle(
            new SendVideoMessageCommand(
                UUID.fromString(authentication.getName()),
                roomId,
                clientMessageId,
                video.getContentType(),
                video.getSize(),
                video::getInputStream));
    if (result.created()) {
      return ResponseEntity.created(
              URI.create("/api/v1/messages/" + result.message().messageId() + "/media"))
          .body(result.message());
    }
    return ResponseEntity.ok(result.message());
  }
}
