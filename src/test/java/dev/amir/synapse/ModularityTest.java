package dev.amir.synapse;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {
  ApplicationModules modules = ApplicationModules.of(MainApplication.class);

  @Test
  void verifyArchitectureBoundaries() {
    // This fails the unit test if any module violates its defined boundaries
    modules.verify();
  }

  @Test
  void writeComponentDocumentation() throws IOException {
    new Documenter(modules).writeDocumentation();

    var docsDir = Path.of("target/spring-modulith-docs");

    assertThat(docsDir).isDirectory();
    assertThat(Files.list(docsDir))
        .as("Modulith docs directory must contain generated PlantUML files")
        .isNotEmpty();
  }
}
