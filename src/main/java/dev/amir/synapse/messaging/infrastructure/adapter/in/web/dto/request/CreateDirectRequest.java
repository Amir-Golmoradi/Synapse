package dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDirectRequest(@NotNull UUID recipientId) {}
