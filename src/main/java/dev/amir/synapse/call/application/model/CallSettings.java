package dev.amir.synapse.call.application.model;

import java.time.Duration;

public record CallSettings(
    Duration ringingTimeout,
    Duration connectionTimeout,
    Duration recoveryTimeout,
    Duration livenessLease,
    int timeoutBatchSize) {}
