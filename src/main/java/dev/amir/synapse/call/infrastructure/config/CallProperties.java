package dev.amir.synapse.call.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("synapse.call")
public record CallProperties(
    Duration ringingTimeout,
    Duration connectionTimeout,
    Duration recoveryTimeout,
    Duration livenessLease,
    Duration timeoutSweep,
    int timeoutBatchSize) {
  public CallProperties {
    ringingTimeout = defaultValue(ringingTimeout, Duration.ofSeconds(45));
    connectionTimeout = defaultValue(connectionTimeout, Duration.ofSeconds(30));
    recoveryTimeout = defaultValue(recoveryTimeout, Duration.ofSeconds(30));
    livenessLease = defaultValue(livenessLease, Duration.ofSeconds(30));
    timeoutSweep = defaultValue(timeoutSweep, Duration.ofSeconds(1));
    timeoutBatchSize = timeoutBatchSize <= 0 ? 100 : timeoutBatchSize;
    requirePositive("ringing-timeout", ringingTimeout);
    requirePositive("connection-timeout", connectionTimeout);
    requirePositive("recovery-timeout", recoveryTimeout);
    requirePositive("liveness-lease", livenessLease);
    requirePositive("timeout-sweep", timeoutSweep);
    if (timeoutBatchSize > 1_000) {
      throw new IllegalArgumentException("timeout-batch-size must not exceed 1000");
    }
  }

  private static Duration defaultValue(Duration value, Duration defaultValue) {
    return value == null ? defaultValue : value;
  }

  private static void requirePositive(String name, Duration value) {
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
