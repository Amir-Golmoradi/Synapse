package dev.amir.synapse.call.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartCallRequest(
    @NotNull UUID calleeId, @NotNull UUID clientRequestId, @NotNull UUID clientInstanceId) {}
