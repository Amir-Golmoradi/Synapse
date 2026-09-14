package dev.amir.synapse.messaging.infrastructure.adapter.out.storage;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LocalMessageMediaStorageAdapter implements MessageMediaStoragePort {
  private static final Pattern STORAGE_KEY =
      Pattern.compile("^video/[0-9a-f]{2}/[0-9a-f-]{36}\\.(webm|mp4)$");
  private static final int BUFFER_SIZE = 64 * 1024;

  private final Path root;
  private final Path stagingRoot;

  public LocalMessageMediaStorageAdapter(VideoMessageSettings settings) {
    root = settings.storageRoot().toAbsolutePath().normalize();
    stagingRoot = root.resolve(".staging");
    try {
      Files.createDirectories(stagingRoot);
    } catch (IOException exception) {
      throw VideoMessageException.unavailable("Video storage cannot be initialized.", exception);
    }
  }

  @Override
  public StagedMedia stage(InputStream content, long maximumBytes) {
    Path temporary = null;
    try {
      temporary = Files.createTempFile(stagingRoot, "upload-", ".part");
      var digest = MessageDigest.getInstance("SHA-256");
      long count = 0;
      try (var output = Files.newOutputStream(temporary)) {
        var buffer = new byte[BUFFER_SIZE];
        var read = content.read(buffer);
        while (read != -1) {
          count += read;
          if (count > maximumBytes) {
            throw VideoMessageException.tooLarge();
          }
          output.write(buffer, 0, read);
          digest.update(buffer, 0, read);
          read = content.read(buffer);
        }
      }
      if (count == 0) {
        throw VideoMessageException.invalid("Video file cannot be empty.");
      }
      return new StagedMedia(temporary, count, digest.digest());
    } catch (VideoMessageException exception) {
      deletePath(temporary);
      throw exception;
    } catch (IOException | NoSuchAlgorithmException exception) {
      deletePath(temporary);
      throw VideoMessageException.unavailable("Video storage failed.", exception);
    }
  }

  @Override
  public String promote(StagedMedia stagedMedia, String contentType) {
    var id = UUID.randomUUID().toString();
    var extension = "video/webm".equals(contentType) ? "webm" : "mp4";
    var key = "video/" + id.substring(0, 2) + "/" + id + "." + extension;
    var target = checkedPath(key);
    try {
      Files.createDirectories(target.getParent());
      try {
        Files.move(stagedMedia.path(), target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(stagedMedia.path(), target);
      }
      return key;
    } catch (IOException exception) {
      throw VideoMessageException.unavailable("Video storage failed.", exception);
    }
  }

  @Override
  public Path resolveForRead(String storageKey) {
    var path = checkedPath(storageKey);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw VideoMessageException.notFound();
    }
    return path;
  }

  @Override
  public void delete(String storageKey) {
    deletePath(checkedPath(storageKey));
  }

  @Override
  public void discard(StagedMedia stagedMedia) {
    deletePath(stagedMedia.path());
  }

  @Override
  public void delete(StoredObject storedObject) {
    var normalized = storedObject.path().toAbsolutePath().normalize();
    if (!normalized.startsWith(root)) {
      throw VideoMessageException.notFound();
    }
    deletePath(normalized);
  }

  @Override
  public List<StoredObject> findStaleStaging(Instant olderThan, int limit) {
    return findStale(stagingRoot, olderThan, limit, true);
  }

  @Override
  public List<StoredObject> findStaleFinal(Instant olderThan, int limit) {
    return findStale(root.resolve("video"), olderThan, limit, false);
  }

  private List<StoredObject> findStale(
      Path directory, Instant olderThan, int limit, boolean staging) {
    if (!Files.isDirectory(directory)) {
      return List.of();
    }
    try (var paths = Files.walk(directory)) {
      var candidates =
          paths
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> lastModified(path).isBefore(olderThan))
              .sorted(Comparator.comparing(LocalMessageMediaStorageAdapter::lastModified))
              .limit(limit)
              .toList();
      var result = new ArrayList<StoredObject>(candidates.size());
      for (var candidate : candidates) {
        var key =
            staging ? candidate.getFileName().toString() : root.relativize(candidate).toString();
        result.add(new StoredObject(key.replace('\\', '/'), candidate));
      }
      return List.copyOf(result);
    } catch (IOException exception) {
      throw VideoMessageException.unavailable("Video storage cleanup failed.", exception);
    }
  }

  private Path checkedPath(String storageKey) {
    if (storageKey == null || !STORAGE_KEY.matcher(storageKey.toLowerCase(Locale.ROOT)).matches()) {
      throw VideoMessageException.notFound();
    }
    var path = root.resolve(storageKey).normalize();
    if (!path.startsWith(root)) {
      throw VideoMessageException.notFound();
    }
    return path;
  }

  private static Instant lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant();
    } catch (IOException exception) {
      return Instant.MAX;
    }
  }

  private static void deletePath(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      throw VideoMessageException.unavailable("Video storage cleanup failed.", exception);
    }
  }
}
