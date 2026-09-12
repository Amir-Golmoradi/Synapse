package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaQuery;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaUseCase;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("api/v1/room/{roomId}/messages/{messageId}/media")
public class VoiceMediaApi {
  private static final int BUFFER_SIZE = 16 * 1024;
  private final GetVoiceMediaUseCase getVoiceMediaUseCase;
  private final HttpByteRangeParser rangeParser;

  public VoiceMediaApi(GetVoiceMediaUseCase getVoiceMediaUseCase, HttpByteRangeParser rangeParser) {
    this.getVoiceMediaUseCase = getVoiceMediaUseCase;
    this.rangeParser = rangeParser;
  }

  @GetMapping
  public ResponseEntity<StreamingResponseBody> download(
      Authentication authentication,
      @PathVariable("roomId") UUID roomId,
      @PathVariable("messageId") UUID messageId,
      @RequestHeader(name = HttpHeaders.RANGE, required = false) @Nullable String rangeHeader) {
    var download =
        getVoiceMediaUseCase.handle(
            new GetVoiceMediaQuery(
                UUID.fromString(authentication.getName()),
                roomId,
                messageId,
                rangeParser.parse(rangeHeader)));
    StreamingResponseBody body =
        output -> {
          try (var input = download.content()) {
            copy(input, output, download.contentLength());
          }
        };
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(download.mimeType()));
    headers.setContentLength(download.contentLength());
    headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
    headers.setCacheControl(CacheControl.noStore().cachePrivate());
    headers.setContentDisposition(
        ContentDisposition.inline()
            .filename("voice-" + messageId + extension(download.mimeType()))
            .build());
    headers.set("X-Content-Type-Options", "nosniff");
    if (download.partial()) {
      var last = download.offset() + download.contentLength() - 1;
      headers.set(
          HttpHeaders.CONTENT_RANGE,
          "bytes " + download.offset() + "-" + last + "/" + download.totalSize());
    }
    return new ResponseEntity<>(
        body, headers, download.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK);
  }

  private static void copy(java.io.InputStream input, java.io.OutputStream output, long length)
      throws IOException {
    var remaining = length;
    var buffer = new byte[BUFFER_SIZE];
    while (remaining > 0) {
      var read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
      if (read < 0) {
        throw new IOException("Voice media ended before the expected content length");
      }
      output.write(buffer, 0, read);
      remaining -= read;
    }
  }

  private static String extension(String mimeType) {
    return switch (mimeType) {
      case "audio/webm" -> ".webm";
      case "audio/ogg" -> ".ogg";
      case "audio/mp4" -> ".m4a";
      default -> "";
    };
  }
}
