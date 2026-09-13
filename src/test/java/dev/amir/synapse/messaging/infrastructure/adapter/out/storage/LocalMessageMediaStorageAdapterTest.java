package dev.amir.synapse.messaging.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMessageMediaStorageAdapterTest {
  @TempDir Path temporaryDirectory;

  @Test
  void streamsHashesPromotesResolvesAndDeletesMedia() throws Exception {
    var storage = new LocalMessageMediaStorageAdapter(settings());
    var content = "video-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    var staged = storage.stage(new ByteArrayInputStream(content), content.length);

    assertThat(staged.sizeBytes()).isEqualTo(content.length);
    assertThat(staged.sha256()).isEqualTo(MessageDigest.getInstance("SHA-256").digest(content));
    var key = storage.promote(staged, "video/webm");
    assertThat(key).matches("video/[0-9a-f]{2}/[0-9a-f-]{36}\\.webm");
    assertThat(Files.readAllBytes(storage.resolveForRead(key))).isEqualTo(content);

    storage.delete(key);
    assertThatThrownBy(() -> storage.resolveForRead(key)).isInstanceOf(VideoMessageException.class);
  }

  @Test
  void enforcesTheStreamLimitAndRemovesPartialStagingFiles() throws Exception {
    var storage = new LocalMessageMediaStorageAdapter(settings());

    assertThatThrownBy(() -> storage.stage(new ByteArrayInputStream(new byte[11]), 10))
        .isInstanceOf(VideoMessageException.class)
        .extracting(exception -> ((VideoMessageException) exception).getErrorCode())
        .isEqualTo("VIDEO_FILE_TOO_LARGE");

    try (var files = Files.list(temporaryDirectory.resolve(".staging"))) {
      assertThat(files).isEmpty();
    }
  }

  @Test
  void rejectsTraversalAndFindsOnlyOldFinalObjects() throws Exception {
    var storage = new LocalMessageMediaStorageAdapter(settings());
    assertThatThrownBy(() -> storage.resolveForRead("../../private.mp4"))
        .isInstanceOf(VideoMessageException.class);

    var staged = storage.stage(new ByteArrayInputStream(new byte[] {1}), 1);
    var key = storage.promote(staged, "video/mp4");
    var path = storage.resolveForRead(key);
    Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.from(Instant.EPOCH));

    assertThat(storage.findStaleFinal(Instant.now().minusSeconds(60), 10))
        .singleElement()
        .satisfies(object -> assertThat(object.storageKey()).isEqualTo(key));
  }

  private VideoMessageSettings settings() {
    return new VideoMessageSettings(
        temporaryDirectory,
        1_000,
        Duration.ofSeconds(60),
        1_920,
        2_073_600,
        Duration.ofSeconds(5),
        Duration.ofHours(1),
        Duration.ofHours(24),
        100);
  }
}
