package dev.amir.synapse.messaging.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.application.model.StoredMediaObject;
import dev.amir.synapse.messaging.application.model.VoiceMediaCleanupSettings;
import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageMediaPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoiceMediaCleanupServiceTest {
  private static final Instant NOW = Instant.parse("2026-09-12T12:00:00Z");

  private final VoiceMediaStoragePort storagePort = mock(VoiceMediaStoragePort.class);
  private final VoiceMessageMediaPort mediaPort = mock(VoiceMessageMediaPort.class);
  private final VoiceMediaCleanupService service =
      new VoiceMediaCleanupService(
          storagePort,
          mediaPort,
          new VoiceMediaCleanupSettings(Duration.ofHours(1), Duration.ofHours(24)),
          Clock.fixed(NOW, ZoneOffset.UTC),
          new SimpleMeterRegistry());

  @Test
  void deletesExpiredPartialAndUnreferencedOrphanButKeepsReferencedMedia() {
    var partial =
        new StoredMediaObject("voice/.partial/partial.part", NOW.minusSeconds(7_200), true);
    var orphan = new StoredMediaObject("voice/aa/orphan", NOW.minusSeconds(90_000), false);
    var referenced = new StoredMediaObject("voice/bb/referenced", NOW.minusSeconds(90_000), false);
    when(storagePort.listOlderThan(NOW.minus(Duration.ofHours(1))))
        .thenReturn(List.of(partial, orphan, referenced));
    when(mediaPort.isStorageKeyReferenced(orphan.storageKey())).thenReturn(false);
    when(mediaPort.isStorageKeyReferenced(referenced.storageKey())).thenReturn(true);

    service.clean();

    verify(storagePort).delete(partial.storageKey());
    verify(storagePort).delete(orphan.storageKey());
    verify(storagePort, never()).delete(referenced.storageKey());
    verify(mediaPort, never()).isStorageKeyReferenced(partial.storageKey());
  }
}
