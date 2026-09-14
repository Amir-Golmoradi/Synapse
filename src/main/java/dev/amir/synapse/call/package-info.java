@ApplicationModule(
    allowedDependencies = {
      "identity :: identity-user-lookup",
      "shared :: shared-domain",
      "shared :: shared-websocket"
    })
package dev.amir.synapse.call;

import org.springframework.modulith.ApplicationModule;
