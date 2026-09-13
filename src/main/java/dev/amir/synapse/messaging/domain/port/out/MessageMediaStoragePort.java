package dev.amir.synapse.messaging.domain.port.out;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface MessageMediaStoragePort {
  StagedMedia stage(InputStream content, long maximumBytes);

  String promote(StagedMedia stagedMedia, String contentType);

  Path resolveForRead(String storageKey);

  void delete(String storageKey);

  void discard(StagedMedia stagedMedia);

  void delete(StoredObject storedObject);

  List<StoredObject> findStaleStaging(Instant olderThan, int limit);

  List<StoredObject> findStaleFinal(Instant olderThan, int limit);

  record StagedMedia(Path path, long sizeBytes, byte[] sha256) {
    public StagedMedia {
      Objects.requireNonNull(path, "Staged path cannot be null");
      if (sizeBytes <= 0) {
        throw new IllegalArgumentException("Staged media must not be empty");
      }
      if (sha256 == null || sha256.length != 32) {
        throw new IllegalArgumentException("SHA-256 digest must contain 32 bytes");
      }
      sha256 = Arrays.copyOf(sha256, sha256.length);
    }

    @Override
    public byte[] sha256() {
      return Arrays.copyOf(sha256, sha256.length);
    }
  }

  record StoredObject(String storageKey, Path path) {}
}
