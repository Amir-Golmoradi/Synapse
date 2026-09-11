package dev.amir.synapse.call.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ResumeCallRequest(
    @NotNull UUID clientInstanceId,
    @NotNull UUID requestId,
    @Min(0) int observedGeneration,
    boolean peerConnectionRetained) {}
