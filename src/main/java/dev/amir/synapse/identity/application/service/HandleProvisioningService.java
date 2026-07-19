package dev.amir.synapse.identity.application.service;

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
import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.service.InitialHandlePolicy;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Allocates an initial Handle using isolated insert attempts and database-owned uniqueness. */
@Service
public class HandleProvisioningService {
  private static final Logger LOGGER = LoggerFactory.getLogger(HandleProvisioningService.class);

  private final CreateUserPort createUser;
  private final LoadUserPort loadUser;
  private final UserSearchCachePort userSearchCache;
  private final InitialHandlePolicy initialHandlePolicy = new InitialHandlePolicy();

  public HandleProvisioningService(
      CreateUserPort createUser, LoadUserPort loadUser, UserSearchCachePort userSearchCache) {
    this.createUser = createUser;
    this.loadUser = loadUser;
    this.userSearchCache = userSearchCache;
  }

  public User provision(VerifiedOidcProfile profile) {
    var userId = UserId.generate();
    var candidates = initialHandlePolicy.candidates(profile.displayName(), userId);

    for (var index = 0; index < candidates.size(); index++) {
      var candidate = candidates.get(index);
      try {
        var created = createUser.create(newUser(userId, profile, candidate));
        invalidateSearchCache();
        return created;
      } catch (HandleConflictException exception) {
        if (isFallback(index, candidates)) {
          throw new HandleProvisioningExhaustedException(exception);
        }
      } catch (GoogleSubjectConflictException exception) {
        return loadWinningGoogleUser(profile, exception);
      } catch (EmailConflictException exception) {
        return resolveEmailConflict(profile);
      } catch (UserIdConflictException exception) {
        throw new HandleProvisioningExhaustedException(exception);
      }
    }
    throw new HandleProvisioningExhaustedException();
  }

  private static User newUser(UserId userId, VerifiedOidcProfile profile, Handle candidateHandle) {
    return User.registerViaGoogle(
        userId,
        profile.email(),
        profile.subjectId(),
        candidateHandle,
        profile.displayName(),
        profile.profilePictureUrl());
  }

  private User loadWinningGoogleUser(
      VerifiedOidcProfile profile, GoogleSubjectConflictException cause) {
    return loadUser
        .findByGoogleId(profile.subjectId())
        .orElseThrow(() -> new HandleProvisioningExhaustedException(cause));
  }

  private User resolveEmailConflict(VerifiedOidcProfile profile) {
    var concurrentGoogleWinner = loadUser.findByGoogleId(profile.subjectId());
    if (concurrentGoogleWinner.isPresent()) {
      return concurrentGoogleWinner.get();
    }
    loadUser.findByEmail(profile.email());
    throw new AccountConflictException();
  }

  private static boolean isFallback(int index, List<Handle> candidates) {
    return index == candidates.size() - 1;
  }

  private void invalidateSearchCache() {
    try {
      userSearchCache.incrementGeneration();
    } catch (RuntimeException exception) {
      LOGGER.debug("User search cache invalidation failed after user creation", exception);
    }
  }
}
