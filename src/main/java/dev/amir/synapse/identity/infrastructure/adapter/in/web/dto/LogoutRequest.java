package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {}
