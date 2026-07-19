package dev.amir.synapse.identity.application.query.user_search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheLookup;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCachePort;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheToken;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchPort;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSearchHandlerTest {

  private UserSearchPort searchPort;
  private UserSearchCachePort cachePort;
  private UserSearchHandler handler;

  @BeforeEach
  void setUp() {
    searchPort = mock(UserSearchPort.class);
    cachePort = mock(UserSearchCachePort.class);
    handler = new UserSearchHandler(searchPort, cachePort);
  }

  @Test
  void returnsCachedSliceWithoutConsultingPostgres() {
    var query = new UserSearchQuery("AMI", 0, 20);
    var cached = result(query, "ami", true);
    when(cachePort.get(query)).thenReturn(new UserSearchCacheLookup.Hit(cached));

    var actual = handler.handle(query);

    assertThat(actual).isSameAs(cached);
    verify(cachePort).get(query);
    verify(cachePort, never()).put(any(), any(), any());
    verifyNoInteractions(searchPort);
  }

  @Test
  void loadsAndCachesPostgresSliceOnCacheMiss() {
    var query = new UserSearchQuery("AMI", 1, 20);
    var databaseResult = result(query, "ami_1", false);
    var token = new UserSearchCacheToken("7");
    when(cachePort.get(query)).thenReturn(new UserSearchCacheLookup.Miss(token));
    when(searchPort.search(query)).thenReturn(databaseResult);

    var actual = handler.handle(query);

    assertThat(actual).isSameAs(databaseResult);
    verify(cachePort).get(query);
    verify(searchPort).search(query);
    verify(cachePort).put(query, databaseResult, token);
  }

  @Test
  void cacheReadFailureFallsBackToPostgresWithoutWarmingAnUnknownGeneration() {
    var query = new UserSearchQuery("ami", 0, 20);
    var databaseResult = result(query, "ami.1", false);
    when(cachePort.get(query)).thenThrow(new IllegalStateException("redis unavailable"));
    when(searchPort.search(query)).thenReturn(databaseResult);

    var actual = handler.handle(query);

    assertThat(actual).isSameAs(databaseResult);
    verify(searchPort).search(query);
    verify(cachePort, never()).put(any(), any(), any());
  }

  @Test
  void cacheWriteFailureDoesNotFailSuccessfulPostgresSearch() {
    var query = new UserSearchQuery("ami", 0, 20);
    var databaseResult = result(query, "ami_2", false);
    var token = new UserSearchCacheToken("3");
    when(cachePort.get(query)).thenReturn(new UserSearchCacheLookup.Miss(token));
    when(searchPort.search(query)).thenReturn(databaseResult);
    doThrow(new IllegalStateException("redis unavailable"))
        .when(cachePort)
        .put(query, databaseResult, token);

    var actual = handler.handle(query);

    assertThat(actual).isSameAs(databaseResult);
    verify(searchPort).search(query);
  }

  private static UserSearchResult result(UserSearchQuery query, String handle, boolean hasNext) {
    var item =
        new UserSearchItem(
            UUID.fromString("11111111-1111-1111-1111-111111111111"), handle, "Amir", null);
    return new UserSearchResult(List.of(item), query.page(), query.size(), hasNext);
  }
}
