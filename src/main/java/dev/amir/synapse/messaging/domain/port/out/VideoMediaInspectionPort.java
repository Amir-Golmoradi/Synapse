package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import java.nio.file.Path;

@FunctionalInterface
public interface VideoMediaInspectionPort {
  VideoMetadata inspect(Path path, String declaredContentType, long sizeBytes);
}
