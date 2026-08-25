package dev.amir.synapse;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  @Test
  void modulesRespectTheirDeclaredBoundaries() {
    ApplicationModules.of(MainApplication.class).verify();
  }
}
