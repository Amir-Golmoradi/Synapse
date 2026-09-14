package dev.amir.synapse.identity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class IdentityArchitectureTest {
  @Test
  void domainAndApplicationDoNotDependOnInfrastructureTypes() {
    var classes = new ClassFileImporter().importPackages("dev.amir.synapse.identity");

    noClasses()
        .that()
        .resideInAnyPackage(
            "dev.amir.synapse.identity.domain..", "dev.amir.synapse.identity.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("dev.amir.synapse.identity.infrastructure..")
        .check(classes);
  }
}
