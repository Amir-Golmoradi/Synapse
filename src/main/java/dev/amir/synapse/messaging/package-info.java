@ApplicationModule(
    allowedDependencies = {
      "identity :: identity-user-lookup",
      "identity :: identity-access-token",
      "identity :: identity-value-object",
      "shared :: shared-domain",
      "identity",
      "shared"
    })
package dev.amir.synapse.messaging;

import org.springframework.modulith.ApplicationModule;
