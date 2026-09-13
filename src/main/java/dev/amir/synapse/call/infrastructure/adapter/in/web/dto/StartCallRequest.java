package dev.amir.synapse.call.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.call.domain.enums.CallMediaType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartCallRequest(
    @NotNull UUID calleeId,
    @NotNull UUID clientRequestId,
    @NotNull UUID clientInstanceId,
    @NotNull CallMediaType mediaType) {}
