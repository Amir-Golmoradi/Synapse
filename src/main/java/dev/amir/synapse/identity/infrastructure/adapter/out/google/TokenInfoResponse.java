package dev.amir.synapse.identity.infrastructure.adapter.out.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenInfoResponse(
    String sub,
    String email,
    @JsonProperty("given_name") String givenName,
    @JsonProperty("family_name") String familyName,
    String picture) {}
