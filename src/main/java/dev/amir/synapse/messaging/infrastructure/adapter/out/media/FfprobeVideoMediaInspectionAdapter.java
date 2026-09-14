package dev.amir.synapse.messaging.infrastructure.adapter.out.media;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.out.VideoMediaInspectionPort;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.AudioCodec;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class FfprobeVideoMediaInspectionAdapter implements VideoMediaInspectionPort {
  private static final int MAX_OUTPUT_BYTES = 1024 * 1024;

  private final ObjectMapper objectMapper;
  private final VideoMessageSettings settings;

  public FfprobeVideoMediaInspectionAdapter(
      ObjectMapper objectMapper, VideoMessageSettings settings) {
    this.objectMapper = objectMapper;
    this.settings = settings;
  }

  @Override
  public VideoMetadata inspect(Path path, String declaredContentType, long sizeBytes) {
    var contentType = normalizeContentType(declaredContentType);
    var command =
        List.of(
            "ffprobe",
            "-v",
            "error",
            "-show_entries",
            "format=format_name,duration:stream=codec_type,codec_name,width,height,duration",
            "-of",
            "json",
            path.toAbsolutePath().toString());
    try {
      var process = new ProcessBuilder(command).redirectErrorStream(true).start();
      if (!process.waitFor(settings.probeTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw VideoMessageException.invalid("Video inspection timed out.");
      }
      var output = process.getInputStream().readNBytes(MAX_OUTPUT_BYTES + 1);
      if (process.exitValue() != 0 || output.length > MAX_OUTPUT_BYTES) {
        throw VideoMessageException.invalid("Video container could not be inspected.");
      }
      return parse(objectMapper.readTree(output), contentType, sizeBytes);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw VideoMessageException.unavailable("Video inspection was interrupted.", exception);
    } catch (IOException exception) {
      throw VideoMessageException.unavailable("Video inspection is unavailable.", exception);
    }
  }

  private static VideoMetadata parse(JsonNode root, String contentType, long sizeBytes) {
    var formatName = root.path("format").path("format_name").asText("");
    if (!containerMatches(formatName, contentType)) {
      throw VideoMessageException.unsupported("Declared and detected video formats differ.");
    }

    var videoStreams = new ArrayList<JsonNode>();
    var audioStreams = new ArrayList<JsonNode>();
    for (var stream : root.path("streams")) {
      switch (stream.path("codec_type").asText()) {
        case "video" -> videoStreams.add(stream);
        case "audio" -> audioStreams.add(stream);
        default -> throw VideoMessageException.unsupported("Unexpected media track.");
      }
    }
    if (videoStreams.size() != 1 || audioStreams.size() > 1) {
      throw VideoMessageException.invalid(
          "Video must contain one video and at most one audio track.");
    }

    var video = videoStreams.getFirst();
    var videoCodec = videoCodec(video.path("codec_name").asText());
    var audioCodec =
        audioStreams.isEmpty()
            ? null
            : audioCodec(audioStreams.getFirst().path("codec_name").asText());
    validateCodecCombination(contentType, videoCodec, audioCodec);
    var durationSeconds = duration(root.path("format"), video, firstOrNull(audioStreams));
    var durationMs = (long) Math.ceil(durationSeconds * 1_000.0);
    return new VideoMetadata(
        contentType,
        sizeBytes,
        durationMs,
        video.path("width").asInt(),
        video.path("height").asInt(),
        videoCodec,
        audioCodec);
  }

  private static String normalizeContentType(String value) {
    var normalized = value.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
    if (!"video/webm".equals(normalized) && !"video/mp4".equals(normalized)) {
      throw VideoMessageException.unsupported("Declared video content type is unsupported.");
    }
    return normalized;
  }

  private static boolean containerMatches(String formatName, String contentType) {
    return "video/webm".equals(contentType)
        ? formatName.contains("webm") || formatName.contains("matroska")
        : formatName.contains("mp4") || formatName.contains("mov");
  }

  private static VideoCodec videoCodec(String codec) {
    return switch (codec) {
      case "vp8" -> VideoCodec.VP8;
      case "vp9" -> VideoCodec.VP9;
      case "h264" -> VideoCodec.H264;
      default -> throw VideoMessageException.unsupported("Video codec is unsupported.");
    };
  }

  private static AudioCodec audioCodec(String codec) {
    return switch (codec) {
      case "opus" -> AudioCodec.OPUS;
      case "aac" -> AudioCodec.AAC;
      default -> throw VideoMessageException.unsupported("Audio codec is unsupported.");
    };
  }

  private static void validateCodecCombination(
      String contentType, VideoCodec videoCodec, @Nullable AudioCodec audioCodec) {
    var compatible =
        "video/webm".equals(contentType)
            ? (videoCodec == VideoCodec.VP8 || videoCodec == VideoCodec.VP9)
                && (audioCodec == null || audioCodec == AudioCodec.OPUS)
            : videoCodec == VideoCodec.H264 && (audioCodec == null || audioCodec == AudioCodec.AAC);
    if (!compatible) {
      throw VideoMessageException.unsupported("Video codecs do not match the container.");
    }
  }

  private static double duration(JsonNode... nodes) {
    double maximum = 0;
    for (var node : nodes) {
      if (node != null) {
        maximum = Math.max(maximum, node.path("duration").asDouble(0));
      }
    }
    if (!Double.isFinite(maximum) || maximum <= 0) {
      throw VideoMessageException.invalid("Video duration is unavailable.");
    }
    return maximum;
  }

  private static @Nullable JsonNode firstOrNull(List<JsonNode> nodes) {
    return nodes.isEmpty() ? null : nodes.getFirst();
  }
}
