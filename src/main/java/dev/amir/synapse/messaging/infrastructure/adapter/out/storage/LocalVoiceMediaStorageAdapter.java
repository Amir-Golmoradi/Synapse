package dev.amir.synapse.messaging.infrastructure.adapter.out.storage;

import dev.amir.synapse.messaging.application.model.StoredMediaObject;
import dev.amir.synapse.messaging.application.model.StoredVoiceMedia;
import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.domain.exception.InvalidVoiceMediaException;
import dev.amir.synapse.messaging.domain.exception.VoiceMediaStorageException;
import dev.amir.synapse.messaging.domain.exception.VoiceMediaTooLargeException;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.VoiceUploadSource;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import dev.amir.synapse.messaging.infrastructure.config.VoiceMessageProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LocalVoiceMediaStorageAdapter implements VoiceMediaStoragePort {
  private static final int BUFFER_SIZE = 16 * 1024;
  private static final int INSPECTION_LIMIT = 1024 * 1024;
  private static final Pattern STORAGE_KEY =
      Pattern.compile(
          "^voice/(?:[0-9a-f]{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|\\.partial/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.part)$");

  private final Path root;
  private final VoiceMediaHeaderInspector inspector = new VoiceMediaHeaderInspector();

  public LocalVoiceMediaStorageAdapter(VoiceMessageProperties properties) {
    root = properties.storageRoot().toAbsolutePath().normalize();
    try {
      Files.createDirectories(root.resolve("voice/.partial"));
    } catch (IOException exception) {
      throw unavailable("Voice media storage could not be initialized.", exception);
    }
  }

  @Override
  public StoredVoiceMedia store(
      UUID messageId, String declaredMimeType, long declaredSizeBytes, VoiceUploadSource source) {
    if (declaredSizeBytes > VoiceMessageMetadata.MAX_SIZE_BYTES) {
      throw new VoiceMediaTooLargeException();
    }
    var identifier = messageId.toString();
    var temporaryKey = "voice/.partial/" + identifier + ".part";
    var finalKey = "voice/" + identifier.substring(0, 2) + "/" + identifier;
    var temporaryPath = resolve(temporaryKey);
    var finalPath = resolve(finalKey);
    try {
      Files.createDirectories(finalPath.getParent());
      var digest = sha256();
      var prefix = new ByteArrayOutputStream(INSPECTION_LIMIT);
      long actualSize = 0;
      try (var input = source.openStream();
          var output =
              Files.newOutputStream(
                  temporaryPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        var buffer = new byte[BUFFER_SIZE];
        while (true) {
          var read = input.read(buffer);
          if (read < 0) {
            break;
          }
          if (read == 0) {
            continue;
          }
          actualSize += read;
          if (actualSize > VoiceMessageMetadata.MAX_SIZE_BYTES) {
            throw new VoiceMediaTooLargeException();
          }
          digest.update(buffer, 0, read);
          var prefixRemaining = INSPECTION_LIMIT - prefix.size();
          if (prefixRemaining > 0) {
            prefix.write(buffer, 0, Math.min(read, prefixRemaining));
          }
          output.write(buffer, 0, read);
        }
      }

      if (actualSize != declaredSizeBytes) {
        throw new InvalidVoiceMediaException(
            "Voice message size does not match the uploaded content.");
      }
      var detectedMimeType = inspector.inspect(declaredMimeType, prefix.toByteArray());
      moveIntoPlace(temporaryPath, finalPath);
      return new StoredVoiceMedia(finalKey, detectedMimeType, actualSize, digest.digest());
    } catch (VoiceMediaTooLargeException | InvalidVoiceMediaException exception) {
      deleteQuietly(temporaryPath);
      throw exception;
    } catch (IOException exception) {
      deleteQuietly(temporaryPath);
      throw unavailable("Voice media could not be stored.", exception);
    } catch (RuntimeException exception) {
      deleteQuietly(temporaryPath);
      throw exception;
    }
  }

  @Override
  public InputStream open(String storageKey, long offset) {
    if (offset < 0) {
      throw new IllegalArgumentException("Media offset cannot be negative");
    }
    try {
      var input = Files.newInputStream(resolve(storageKey), StandardOpenOption.READ);
      try {
        input.skipNBytes(offset);
        return input;
      } catch (IOException exception) {
        input.close();
        throw exception;
      }
    } catch (IOException exception) {
      throw unavailable("Voice media could not be opened.", exception);
    }
  }

  @Override
  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException exception) {
      throw unavailable("Voice media could not be deleted.", exception);
    }
  }

  @Override
  public List<StoredMediaObject> listOlderThan(Instant cutoff) {
    var objects = new ArrayList<StoredMediaObject>();
    var voiceRoot = root.resolve("voice");
    try (var paths = Files.walk(voiceRoot)) {
      paths
          .filter(Files::isRegularFile)
          .forEach(
              path -> {
                try {
                  var lastModified = Files.getLastModifiedTime(path).toInstant();
                  if (lastModified.isBefore(cutoff)) {
                    var key =
                        root.relativize(path)
                            .toString()
                            .replace(path.getFileSystem().getSeparator(), "/");
                    objects.add(
                        new StoredMediaObject(key, lastModified, key.contains("/.partial/")));
                  }
                } catch (IOException exception) {
                  throw unavailable("Voice media metadata could not be read.", exception);
                }
              });
      return List.copyOf(objects);
    } catch (IOException exception) {
      throw unavailable("Voice media storage could not be scanned.", exception);
    }
  }

  private Path resolve(String storageKey) {
    if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
      throw new IllegalArgumentException("Invalid voice media storage key");
    }
    var resolved = root.resolve(storageKey).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Voice media storage key escapes the configured root");
    }
    return resolved;
  }

  private static void moveIntoPlace(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, target);
    }
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 must be available", exception);
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // The scheduled orphan cleanup is the recovery path.
    }
  }

  private static VoiceMediaStorageException unavailable(String message, Exception cause) {
    return new VoiceMediaStorageException(message, cause);
  }
}
