package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import dev.amir.synapse.messaging.domain.exception.InvalidVoiceMediaException;
import dev.amir.synapse.messaging.domain.exception.UnsupportedVoiceMediaException;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageUseCase;
import dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request.VoiceMessageUploadMetadata;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "api/v1/room/{roomId}/messages", produces = APPLICATION_JSON_VALUE)
public class VoiceMessageApi {
  private final SendVoiceMessageUseCase sendVoiceMessageUseCase;

  public VoiceMessageApi(SendVoiceMessageUseCase sendVoiceMessageUseCase) {
    this.sendVoiceMessageUseCase = sendVoiceMessageUseCase;
  }

  @PostMapping(value = "/voice", consumes = MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<dev.amir.synapse.messaging.domain.port.in.message.MessageView> upload(
      Authentication authentication,
      @PathVariable("roomId") UUID roomId,
      @Valid @RequestPart("metadata") VoiceMessageUploadMetadata metadata,
      @RequestPart("audio") MultipartFile audio) {
    if (audio.isEmpty()) {
      throw new InvalidVoiceMediaException("Voice message media cannot be empty.");
    }
    var contentType = audio.getContentType();
    if (contentType == null || contentType.isBlank()) {
      throw new UnsupportedVoiceMediaException();
    }
    var result =
        sendVoiceMessageUseCase.handle(
            new SendVoiceMessageCommand(
                UUID.fromString(authentication.getName()),
                roomId,
                metadata.clientMessageId(),
                metadata.durationMs(),
                contentType,
                audio.getSize(),
                audio::getInputStream));
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(result.message());
  }
}
