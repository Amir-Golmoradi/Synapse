package dev.amir.synapse.messaging;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class MessagingArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter().importPackages("dev.amir.synapse.messaging");

  @Test
  void domainAndApplicationDoNotDependOnInfrastructure() {
    noClasses()
        .that()
        .resideInAnyPackage(
            "dev.amir.synapse.messaging.domain..", "dev.amir.synapse.messaging.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("dev.amir.synapse.messaging.infrastructure..")
        .check(classes);
  }

  @Test
  void messagingDoesNotDependOnCall() {
    noClasses()
        .that()
        .resideInAnyPackage("dev.amir.synapse.messaging..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("dev.amir.synapse.call..")
        .check(classes);
  }
}
