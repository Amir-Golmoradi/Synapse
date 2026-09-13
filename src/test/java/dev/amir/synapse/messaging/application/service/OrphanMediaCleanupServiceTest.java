package dev.amir.synapse.messaging.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaReadPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort.StoredObject;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrphanMediaCleanupServiceTest {

  @Test
  void deletesStaleStagingAndOnlyUnreferencedFinalObjects() {
    var storage = mock(MessageMediaStoragePort.class);
    var references = mock(MessageMediaReadPort.class);
    var now = Instant.parse("2026-09-13T12:00:00Z");
    var staged = new StoredObject("upload.part", Path.of("staging/upload.part"));
    var orphan = new StoredObject("video/aa/orphan.webm", Path.of("video/aa/orphan.webm"));
    var referenced = new StoredObject("video/bb/used.webm", Path.of("video/bb/used.webm"));
    when(storage.findStaleStaging(now.minus(Duration.ofHours(1)), 100)).thenReturn(List.of(staged));
    when(storage.findStaleFinal(now.minus(Duration.ofHours(24)), 100))
        .thenReturn(List.of(orphan, referenced));
    when(references.isStorageKeyReferenced(orphan.storageKey())).thenReturn(false);
    when(references.isStorageKeyReferenced(referenced.storageKey())).thenReturn(true);
    var service =
        new OrphanMediaCleanupService(
            storage,
            references,
            settings(),
            Clock.fixed(now, ZoneOffset.UTC),
            new SimpleMeterRegistry());

    service.cleanup();

    verify(storage).delete(staged);
    verify(storage).delete(orphan);
    verify(storage, never()).delete(referenced);
  }

  private static VideoMessageSettings settings() {
    return new VideoMessageSettings(
        Path.of("media"),
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
