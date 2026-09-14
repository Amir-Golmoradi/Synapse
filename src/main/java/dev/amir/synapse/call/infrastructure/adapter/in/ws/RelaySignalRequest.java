package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import dev.amir.synapse.call.application.model.CallSignal;
import java.util.UUID;

public record RelaySignalRequest(UUID clientInstanceId, CallSignal signal) {}
