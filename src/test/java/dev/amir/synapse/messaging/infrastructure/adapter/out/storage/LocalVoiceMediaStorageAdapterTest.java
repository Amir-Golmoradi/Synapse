package dev.amir.synapse.messaging.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.exception.InvalidVoiceMediaException;
import dev.amir.synapse.messaging.infrastructure.config.VoiceMessageProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalVoiceMediaStorageAdapterTest {
  @TempDir Path root;

  @Test
  void storesHashesReadsAndDeletesValidatedOggOpus() throws Exception {
    var adapter = adapter();
    var content = oggOpus("payload");
    var stored =
        adapter.store(
            UUID.randomUUID(),
            "audio/ogg",
            content.length,
            () -> new ByteArrayInputStream(content));

    assertThat(stored.mimeType()).isEqualTo("audio/ogg");
    assertThat(stored.sizeBytes()).isEqualTo(content.length);
    assertThat(stored.sha256()).hasSize(32);
    try (var input = adapter.open(stored.storageKey(), 4)) {
      assertThat(input.readAllBytes())
          .containsExactly(java.util.Arrays.copyOfRange(content, 4, content.length));
    }

    adapter.delete(stored.storageKey());
    assertThat(root.resolve(stored.storageKey())).doesNotExist();
  }

  @Test
  void rejectsMismatchedAndTraversalContent() {
    var adapter = adapter();
    var content = oggOpus("payload");

    assertThatThrownBy(
            () ->
                adapter.store(
                    UUID.randomUUID(),
                    "audio/webm",
                    content.length,
                    () -> new ByteArrayInputStream(content)))
        .isInstanceOf(InvalidVoiceMediaException.class);
    assertThatThrownBy(() -> adapter.open("../../secret", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void listsOnlyObjectsOlderThanCutoff() throws Exception {
    var adapter = adapter();
    var content = oggOpus("old");
    var stored =
        adapter.store(
            UUID.randomUUID(),
            "audio/ogg",
            content.length,
            () -> new ByteArrayInputStream(content));
    Files.setLastModifiedTime(
        root.resolve(stored.storageKey()),
        java.nio.file.attribute.FileTime.from(Instant.parse("2026-01-01T00:00:00Z")));

    assertThat(adapter.listOlderThan(Instant.parse("2026-01-02T00:00:00Z")))
        .extracting(object -> object.storageKey())
        .containsExactly(stored.storageKey());
  }

  private LocalVoiceMediaStorageAdapter adapter() {
    return new LocalVoiceMediaStorageAdapter(
        new VoiceMessageProperties(
            root, Duration.ofHours(1), Duration.ofHours(24), Duration.ofHours(1)));
  }

  private static byte[] oggOpus(String payload) {
    return ("OggS" + "header" + "OpusHead" + payload)
        .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  }
}
