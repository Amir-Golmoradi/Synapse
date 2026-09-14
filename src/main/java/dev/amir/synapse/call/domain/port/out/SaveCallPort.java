package dev.amir.synapse.call.domain.port.out;

import dev.amir.synapse.call.domain.model.Call;

public interface SaveCallPort {
  Call save(Call call);

  Call saveAndFlush(Call call);
}
