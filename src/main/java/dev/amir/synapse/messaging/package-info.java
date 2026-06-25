@ApplicationModule(
    allowedDependencies = {
      "identity :: identity-user-lookup",
      "shared :: shared-domain",
      "identity"
    })
package dev.amir.synapse.messaging;

import org.springframework.modulith.ApplicationModule;
