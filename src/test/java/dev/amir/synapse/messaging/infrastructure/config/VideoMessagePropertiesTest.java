package dev.amir.synapse.messaging.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class VideoMessagePropertiesTest {

  @Test
  void rejectsConfigurationThatExceedsDatabaseHardLimits() {
    assertThatThrownBy(
            () -> properties(DataSize.ofMegabytes(26), Duration.ofSeconds(60), 1_920, 2_073_600))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> properties(DataSize.ofMegabytes(25), Duration.ofSeconds(61), 1_920, 2_073_600))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> properties(DataSize.ofMegabytes(25), Duration.ofSeconds(60), 1_921, 2_073_600))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static VideoMessageProperties properties(
      DataSize size, Duration duration, int dimension, long pixels) {
    return new VideoMessageProperties(
        Path.of("media"),
        size,
        duration,
        dimension,
        pixels,
        Duration.ofSeconds(5),
        Duration.ofHours(1),
        Duration.ofHours(24),
        Duration.ofHours(1),
        100);
  }
}
