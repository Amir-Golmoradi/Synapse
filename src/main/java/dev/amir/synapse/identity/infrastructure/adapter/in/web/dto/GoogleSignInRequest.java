package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleSignInRequest(@NotBlank String idToken) {}
