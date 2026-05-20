package dev.amir.synapse;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ArchitectureTest {
  ApplicationModules modules = ApplicationModules.of(MainApplication.class);

  @Test
  void verifyArchitectureBoundaries() {
    // This fails the unit test if any module violates its defined boundaries
    modules.verify();
  }

  @Test
  void writeComponentDocumentation() {
    // Generates structural PlantUML component diagrams inside target/modulith-docs/
    new Documenter(modules).writeDocumentation();
  }
}
