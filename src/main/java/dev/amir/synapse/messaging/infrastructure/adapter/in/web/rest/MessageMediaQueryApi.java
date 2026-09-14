package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.port.in.get_message_media.GetMessageMediaUseCase;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageMediaQueryApi {
  private final GetMessageMediaUseCase getMessageMedia;

  public MessageMediaQueryApi(GetMessageMediaUseCase getMessageMedia) {
    this.getMessageMedia = getMessageMedia;
  }

  @GetMapping("/{messageId}/media")
  public ResponseEntity<Resource> get(@PathVariable UUID messageId, Authentication authentication) {
    var media = getMessageMedia.handle(messageId, UUID.fromString(authentication.getName()));
    var extension = "video/webm".equals(media.contentType()) ? "webm" : "mp4";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(media.contentType()))
        .contentLength(media.sizeBytes())
        .cacheControl(
            CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePrivate().noTransform())
        .eTag('"' + HexFormat.of().formatHex(media.sha256()) + '"')
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .header("X-Content-Type-Options", "nosniff")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename("video-message." + extension).build().toString())
        .body(new FileSystemResource(media.path()));
  }
}
