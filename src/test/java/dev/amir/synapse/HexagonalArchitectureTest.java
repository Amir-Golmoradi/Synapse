package dev.amir.synapse;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// HexagonalArchitectureTest.java
@AnalyzeClasses(packages = "dev.amir.synapse")
class HexagonalArchitectureTest {

  // Domain must never touch adapters or Spring
  @ArchTest
  static final ArchRule domain_must_not_depend_on_adapters =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..");

  // Domain must never touch Spring annotations
  @ArchTest
  static final ArchRule domain_must_not_depend_on_spring =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .and(not(simpleName("package-info")))
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.springframework..");

  // Domain must never touch JPA
  @ArchTest
  static final ArchRule domain_must_not_depend_on_jpa =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("jakarta.persistence..");

  // Adapters/in must only call ports/in — never application directly
  @ArchTest
  static final ArchRule adapters_in_must_only_call_ports_in =
      noClasses()
          .that()
          .resideInAPackage("..adapter.in..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..application..");

  // Application handlers must not depend on adapter implementations
  @ArchTest
  static final ArchRule application_must_not_depend_on_adapters =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..");
}
