package dev.amir.synapse.call.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FindDueCallsPort {
  List<UUID> findDueCallIds(Instant now, int limit);

  List<UUID> findExpiredLeaseCallIds(Instant now, int limit);
}
