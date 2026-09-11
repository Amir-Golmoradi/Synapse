package dev.amir.synapse.call;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class CallArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter().importPackages("dev.amir.synapse.call");

  @Test
  void domainAndApplicationDoNotDependOnInfrastructureTypes() {
    noClasses()
        .that()
        .resideInAnyPackage("dev.amir.synapse.call.domain..", "dev.amir.synapse.call.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("dev.amir.synapse.call.infrastructure..")
        .check(classes);
  }

  @Test
  void callDoesNotDependOnMessaging() {
    noClasses()
        .that()
        .resideInAnyPackage("dev.amir.synapse.call..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("dev.amir.synapse.messaging..")
        .check(classes);
  }
}
