package dev.amir.synapse.messaging.infrastructure.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * A response DTO has one job: carry application output safely across the HTTP boundary. Design it
 * around <span> what the client needs to render the next screen </span>, not what your domain
 * contains.
 */
public record CreateDirectResponse(UUID creatorId, UUID recipientId, Instant createdAt) {}
