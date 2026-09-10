package dev.amir.synapse.call.application.model;

import java.util.UUID;

public record CallNegotiationInstruction(
    String type, UUID callId, int generation, boolean callerOffers) {}
