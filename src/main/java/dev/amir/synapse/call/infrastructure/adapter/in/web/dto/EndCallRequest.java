package dev.amir.synapse.call.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.call.domain.port.in.EndCallUseCase;
import jakarta.validation.constraints.NotNull;

public record EndCallRequest(@NotNull EndCallUseCase.EndReason reason) {}
