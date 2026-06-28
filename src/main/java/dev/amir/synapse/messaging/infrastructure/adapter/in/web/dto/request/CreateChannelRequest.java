package dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

public record CreateChannelRequest(
    @NotBlank @Size(max = 100) String name,
    @URL @Size(max = 2048) @Nullable String avatarUrl,
    @NotNull @Size(max = 500, message = "Cannot add more than 500 initial members")
        Set<@NotNull UUID> initialMemberIds) {}
