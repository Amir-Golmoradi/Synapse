package dev.amir.synapse.messaging.infrastructure.adapter.out.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.AudioCodec;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class FfprobeVideoMediaInspectionAdapterTest {
  @TempDir Path temporaryDirectory;

  private FfprobeVideoMediaInspectionAdapter inspection;

  @BeforeEach
  void setUp() {
    inspection = new FfprobeVideoMediaInspectionAdapter(JsonMapper.builder().build(), settings());
  }

  @Test
  void inspectsARealBrowserCompatibleWebmContainer() throws Exception {
    var video = temporaryDirectory.resolve("recording.webm");
    createWebm(video);

    var metadata = inspection.inspect(video, "video/webm;codecs=vp9,opus", Files.size(video));

    assertThat(metadata.contentType()).isEqualTo("video/webm");
    assertThat(metadata.sizeBytes()).isEqualTo(Files.size(video));
    assertThat(metadata.durationMs()).isBetween(1L, 2_000L);
    assertThat(metadata.width()).isEqualTo(320);
    assertThat(metadata.height()).isEqualTo(240);
    assertThat(metadata.videoCodec()).isEqualTo(VideoCodec.VP9);
    assertThat(metadata.audioCodec()).isEqualTo(AudioCodec.OPUS);
  }

  @Test
  void rejectsCorruptContentAndDeclaredContainerSpoofing() throws Exception {
    var corrupt = temporaryDirectory.resolve("corrupt.webm");
    Files.write(corrupt, new byte[] {1, 2, 3, 4});
    assertThatThrownBy(() -> inspection.inspect(corrupt, "video/webm", Files.size(corrupt)))
        .isInstanceOf(VideoMessageException.class)
        .extracting(exception -> ((VideoMessageException) exception).getErrorCode())
        .isEqualTo("VIDEO_MEDIA_INVALID");

    var valid = temporaryDirectory.resolve("valid.webm");
    createWebm(valid);
    assertThatThrownBy(() -> inspection.inspect(valid, "video/mp4", Files.size(valid)))
        .isInstanceOf(VideoMessageException.class)
        .extracting(exception -> ((VideoMessageException) exception).getErrorCode())
        .isEqualTo("VIDEO_FORMAT_UNSUPPORTED");
  }

  private static void createWebm(Path target) throws Exception {
    var command =
        List.of(
            "ffmpeg",
            "-nostdin",
            "-v",
            "error",
            "-f",
            "lavfi",
            "-i",
            "color=c=black:s=320x240:d=0.25",
            "-f",
            "lavfi",
            "-i",
            "anullsrc=r=48000:cl=mono",
            "-shortest",
            "-c:v",
            "libvpx-vp9",
            "-c:a",
            "libopus",
            target.toString());
    var process = new ProcessBuilder(command).redirectErrorStream(true).start();
    assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
    var output = new String(process.getInputStream().readAllBytes());
    assertThat(process.exitValue()).withFailMessage(output).isZero();
  }

  private VideoMessageSettings settings() {
    return new VideoMessageSettings(
        temporaryDirectory,
        25L * 1_024 * 1_024,
        Duration.ofSeconds(60),
        1_920,
        2_073_600,
        Duration.ofSeconds(5),
        Duration.ofHours(1),
        Duration.ofHours(24),
        100);
  }
}
