package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.amir.synapse.identity.application.exception.AccountConflictException;
import dev.amir.synapse.identity.application.exception.EmailConflictException;
import dev.amir.synapse.identity.application.exception.GoogleSubjectConflictException;
import dev.amir.synapse.identity.application.exception.HandleConflictException;
import dev.amir.synapse.identity.application.exception.HandleProvisioningExhaustedException;
import dev.amir.synapse.identity.application.exception.UserIdConflictException;
import dev.amir.synapse.identity.application.port.out.oauth.VerifiedOidcProfile;
import dev.amir.synapse.identity.application.port.out.user.CreateUserPort;
import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCachePort;
import dev.amir.synapse.identity.application.service.HandleProvisioningService;
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.service.InitialHandlePolicy;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "server.port=0",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/create-table,classpath:db/alter-table",
      "spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
      "spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
      "synapse.google-token-url=http://localhost/tokeninfo?id_token={idToken}",
      "synapse.jwt.secret=01234567890123456789012345678901",
      "synapse.jwt.token-expiration-ms=900000"
    })
@Testcontainers
class UserPersistenceIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_identity_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Autowired private CreateUserPort createUser;

  @Autowired private LoadUserPort loadUser;

  @Autowired private HandleProvisioningService provisioningService;

  @Autowired private UserJpaRepository repository;

  @MockitoBean private UserSearchCachePort searchCache;

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void clearUsers() {
    repository.deleteAll();
  }

  @Test
  void translatesNamedHandleConstraintFromPostgresMetadata() {
    createUser.create(user("owner@example.com", "google-owner", "shared_handle"));

    assertThatThrownBy(
            () ->
                createUser.create(
                    user("challenger@example.com", "google-challenger", "shared_handle")))
        .isInstanceOf(HandleConflictException.class);

    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void translatesNamedGoogleSubjectConstraintFromPostgresMetadata() {
    createUser.create(user("owner@example.com", "shared-google-subject", "owner_handle"));

    assertThatThrownBy(
            () ->
                createUser.create(
                    user("challenger@example.com", "shared-google-subject", "challenger_handle")))
        .isInstanceOf(GoogleSubjectConflictException.class);

    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void translatesNamedEmailConstraintFromPostgresMetadata() {
    createUser.create(user("shared@example.com", "google-owner", "owner_handle"));

    assertThatThrownBy(
            () ->
                createUser.create(
                    user("shared@example.com", "google-challenger", "challenger_handle")))
        .isInstanceOf(EmailConflictException.class);

    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void translatesNamedPrimaryKeyConstraintFromPostgresMetadata() {
    var userId = UserId.generate();
    createUser.create(user(userId, "owner@example.com", "google-owner", "owner_handle"));

    assertThatThrownBy(
            () ->
                createUser.create(
                    user(
                        userId,
                        "challenger@example.com",
                        "google-challenger",
                        "challenger_handle")))
        .isInstanceOf(UserIdConflictException.class);

    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void provisioningRetriesAFullLengthReadableHandleWithSeparateSuffixTruncation() {
    var readableBase = "abcdefghijklmnopqrstuvwxyz123456";
    var expectedRetry = readableBase.substring(0, 30) + "_1";
    createUser.create(user("owner@example.com", "google-owner", readableBase));

    var created =
        provisioningService.provision(
            profile(readableBase, "google-newcomer", "newcomer@example.com"));

    assertThat(created.getHandle().value()).isEqualTo(expectedRetry).hasSize(32);
    assertThat(loadUser.findByGoogleId("google-newcomer"))
        .hasValueSatisfying(
            reloaded -> assertThat(reloaded.getHandle().value()).isEqualTo(expectedRetry));
    assertThat(repository.count()).isEqualTo(2);
    verify(searchCache).incrementGeneration();
  }

  @Test
  void fallbackHandleCollisionRollsBackTheProvisionedUser() {
    var profile = profile("Amir", "google-target", "target@example.com");
    var readableCandidates =
        new InitialHandlePolicy().candidates(profile.displayName(), UserId.generate());
    for (var index = 0; index < readableCandidates.size() - 1; index++) {
      createUser.create(
          user(
              "readable-owner-" + index + "@example.com",
              "readable-owner-" + index,
              readableCandidates.get(index).value()));
    }

    CreateUserPort fallbackBlockingCreate =
        requested -> {
          if (requested.getHandle().value().startsWith("u_")) {
            createUser.create(
                user(
                    "fallback-owner@example.com", "fallback-owner", requested.getHandle().value()));
          }
          return createUser.create(requested);
        };
    var service = new HandleProvisioningService(fallbackBlockingCreate, loadUser, searchCache);

    assertThatThrownBy(() -> service.provision(profile))
        .isInstanceOf(HandleProvisioningExhaustedException.class);

    assertThat(loadUser.findByGoogleId(profile.subjectId())).isEmpty();
    assertThat(loadUser.findByEmail(profile.email())).isEmpty();
    assertThat(repository.count()).isEqualTo(7);
    verify(searchCache, never()).incrementGeneration();
  }

  @Test
  void concurrentDistinctGoogleSubjectsWithSameDisplayNameReceiveAdjacentHandles()
      throws InterruptedException, ExecutionException, TimeoutException {
    var service = provisioningServiceWithFirstCreatesAtBarrier();

    var attempts =
        provisionConcurrently(
            service,
            profile("Concurrent Amir", "google-concurrent-1", "concurrent-1@example.com"),
            profile("Concurrent Amir", "google-concurrent-2", "concurrent-2@example.com"));

    assertThat(attempts).allSatisfy(ProvisionAttempt::assertSuccessful);
    assertThat(attempts)
        .extracting(attempt -> attempt.user().orElseThrow().getHandle().value())
        .containsExactlyInAnyOrder("concurrent_amir", "concurrent_amir_1");
    assertThat(repository.findAll())
        .extracting(UserEntity::getHandle)
        .containsExactlyInAnyOrder("concurrent_amir", "concurrent_amir_1");
    assertThat(repository.count()).isEqualTo(2);
  }

  @Test
  void concurrentCallbacksForSameGoogleSubjectReturnOneWinningUser()
      throws InterruptedException, ExecutionException, TimeoutException {
    var service = provisioningServiceWithFirstCreatesAtBarrier();
    var sharedProfile =
        profile("Same Identity", "google-shared-subject", "same-identity@example.com");

    var attempts = provisionConcurrently(service, sharedProfile, sharedProfile);

    assertThat(attempts).allSatisfy(ProvisionAttempt::assertSuccessful);
    var winnerId = attempts.getFirst().user().orElseThrow().getId();
    assertThat(attempts)
        .extracting(attempt -> attempt.user().orElseThrow().getId())
        .containsOnly(winnerId);
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            winner -> {
              assertThat(winner.getId()).isEqualTo(winnerId.value());
              assertThat(winner.getGoogleId()).isEqualTo(sharedProfile.subjectId());
              assertThat(winner.getEmail()).isEqualTo(sharedProfile.email());
            });
  }

  @Test
  void concurrentDistinctSubjectsWithSameVerifiedEmailYieldOneAccountConflict()
      throws InterruptedException, ExecutionException, TimeoutException {
    var service = provisioningServiceWithFirstCreatesAtBarrier();
    var sharedEmail = "verified-shared@example.com";

    var attempts =
        provisionConcurrently(
            service,
            profile("First Identity", "google-email-1", sharedEmail),
            profile("Second Identity", "google-email-2", sharedEmail));

    assertThat(attempts.stream().flatMap(attempt -> attempt.user().stream()).toList())
        .singleElement();
    assertThat(attempts.stream().flatMap(attempt -> attempt.failure().stream()).toList())
        .singleElement()
        .isInstanceOf(AccountConflictException.class);
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            winner -> {
              assertThat(winner.getEmail()).isEqualTo(Email.of(sharedEmail));
              assertThat(winner.getGoogleId()).isIn("google-email-1", "google-email-2");
            });
  }

  private HandleProvisioningService provisioningServiceWithFirstCreatesAtBarrier() {
    var firstCreateBarrier = new CyclicBarrier(2);
    var createInvocations = new AtomicInteger();
    CreateUserPort createAtBarrier =
        requested -> {
          if (createInvocations.incrementAndGet() <= 2) {
            await(firstCreateBarrier);
          }
          return createUser.create(requested);
        };
    return new HandleProvisioningService(createAtBarrier, loadUser, searchCache);
  }

  private List<ProvisionAttempt> provisionConcurrently(
      HandleProvisioningService service,
      VerifiedOidcProfile firstProfile,
      VerifiedOidcProfile secondProfile)
      throws InterruptedException, ExecutionException, TimeoutException {
    var executor = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var first = executor.submit(() -> awaitStartAndProvision(service, firstProfile, ready, start));
    var second =
        executor.submit(() -> awaitStartAndProvision(service, secondProfile, ready, start));

    try {
      assertThat(ready.await(5, SECONDS)).as("both provisioning workers became ready").isTrue();
      start.countDown();
      return List.of(first.get(10, SECONDS), second.get(10, SECONDS));
    } finally {
      start.countDown();
      first.cancel(true);
      second.cancel(true);
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, SECONDS))
          .as("provisioning workers terminated")
          .isTrue();
    }
  }

  private static ProvisionAttempt awaitStartAndProvision(
      HandleProvisioningService service,
      VerifiedOidcProfile profile,
      CountDownLatch ready,
      CountDownLatch start)
      throws InterruptedException {
    ready.countDown();
    if (!start.await(5, SECONDS)) {
      throw new IllegalStateException("Timed out waiting to start concurrent provisioning");
    }
    try {
      return ProvisionAttempt.success(service.provision(profile));
    } catch (RuntimeException exception) {
      return ProvisionAttempt.failure(exception);
    }
  }

  private static void await(CyclicBarrier barrier) {
    try {
      barrier.await(5, SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Concurrent create barrier was interrupted", exception);
    } catch (BrokenBarrierException | TimeoutException exception) {
      throw new IllegalStateException("Concurrent create barrier did not complete", exception);
    }
  }

  private static VerifiedOidcProfile profile(String displayName, String subjectId, String email) {
    return new VerifiedOidcProfile(
        "google", subjectId, Email.of(email), DisplayName.of(displayName), null);
  }

  private static User user(String email, String googleSubject, String handle) {
    return user(UserId.generate(), email, googleSubject, handle);
  }

  private static User user(UserId userId, String email, String googleSubject, String handle) {
    return User.reconstitute(
        userId,
        Email.of(email),
        googleSubject,
        Handle.of(handle),
        DisplayName.of("Persistence Test " + UUID.randomUUID().toString().substring(0, 8)),
        null);
  }

  private record ProvisionAttempt(Optional<User> user, Optional<RuntimeException> failure) {
    private static ProvisionAttempt success(User user) {
      return new ProvisionAttempt(Optional.of(user), Optional.empty());
    }

    private static ProvisionAttempt failure(RuntimeException failure) {
      return new ProvisionAttempt(Optional.empty(), Optional.of(failure));
    }

    private void assertSuccessful() {
      assertThat(user).isPresent();
      assertThat(failure).isEmpty();
    }
  }
}
