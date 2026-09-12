package dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request;

import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VoiceMessageUploadMetadata(
    @NotNull UUID clientMessageId,
    @Min(1) @Max(VoiceMessageMetadata.MAX_DURATION_MS) int durationMs) {}
