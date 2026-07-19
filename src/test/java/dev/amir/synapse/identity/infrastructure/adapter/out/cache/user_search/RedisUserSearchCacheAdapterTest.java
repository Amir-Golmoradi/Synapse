package dev.amir.synapse.identity.infrastructure.adapter.out.cache.user_search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheLookup;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheToken;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class RedisUserSearchCacheAdapterTest {
  private RedisTemplate<String, String> redis;
  private ValueOperations<String, String> values;
  private RedisUserSearchCacheAdapter adapter;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redis = mock(RedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    adapter =
        new RedisUserSearchCacheAdapter(
            redis, JsonMapper.builder().build(), RedisUserSearchCacheAdapter.CACHE_TTL);
  }

  @Test
  void readsSliceFromCurrentGeneration() {
    var query = new UserSearchQuery("ami", 0, 20);
    var expected = result(query);
    when(values.get("identity:user-search:v1:generation")).thenReturn("4");
    when(values.get("identity:user-search:v1:g:4:prefix:ami:page:0:size:20"))
        .thenReturn(JsonMapper.builder().build().writeValueAsString(expected));

    var cached = adapter.get(query);

    assertThat(cached).isEqualTo(new UserSearchCacheLookup.Hit(expected));
  }

  @Test
  void writesSliceForSixtySeconds() {
    var query = new UserSearchQuery("ami", 1, 20);
    var result = result(query);

    adapter.put(query, result, new UserSearchCacheToken("7"));

    var json = ArgumentCaptor.forClass(String.class);
    verify(values)
        .set(
            org.mockito.ArgumentMatchers.eq(
                "identity:user-search:v1:g:7:prefix:ami:page:1:size:20"),
            json.capture(),
            org.mockito.ArgumentMatchers.eq(RedisUserSearchCacheAdapter.CACHE_TTL));
    assertThat(json.getValue()).contains("\"handle\":\"ami_1\"");
  }

  @Test
  void incrementsGenerationWithoutScanningKeys() {
    adapter.incrementGeneration();

    verify(values).increment("identity:user-search:v1:generation");
  }

  @Test
  void changedGenerationMakesPreviousSliceUnreachable() {
    var query = new UserSearchQuery("ami", 0, 20);
    var json = JsonMapper.builder().build().writeValueAsString(result(query));
    when(values.get("identity:user-search:v1:generation")).thenReturn("4", "5");
    when(values.get("identity:user-search:v1:g:4:prefix:ami:page:0:size:20")).thenReturn(json);
    when(values.get("identity:user-search:v1:g:5:prefix:ami:page:0:size:20")).thenReturn(null);

    assertThat(adapter.get(query)).isInstanceOf(UserSearchCacheLookup.Hit.class);
    adapter.incrementGeneration();
    assertThat(adapter.get(query)).isInstanceOf(UserSearchCacheLookup.Miss.class);
  }

  @Test
  void generationChangeBetweenMissAndPutCannotPublishStaleResultInNewGeneration() {
    var query = new UserSearchQuery("ami", 0, 20);
    var staleResult = result(query);
    when(values.get("identity:user-search:v1:generation")).thenReturn("4", "5");

    var initialLookup = adapter.get(query);

    assertThat(initialLookup).isInstanceOf(UserSearchCacheLookup.Miss.class);
    var token = ((UserSearchCacheLookup.Miss) initialLookup).token();
    assertThat(token.value()).isEqualTo("4");

    adapter.incrementGeneration();
    adapter.put(query, staleResult, token);

    assertThat(adapter.get(query))
        .isEqualTo(new UserSearchCacheLookup.Miss(new UserSearchCacheToken("5")));
    verify(values)
        .set(
            org.mockito.ArgumentMatchers.eq(
                "identity:user-search:v1:g:4:prefix:ami:page:0:size:20"),
            anyString(),
            org.mockito.ArgumentMatchers.eq(RedisUserSearchCacheAdapter.CACHE_TTL));
    verify(values, never())
        .set(
            org.mockito.ArgumentMatchers.eq(
                "identity:user-search:v1:g:5:prefix:ami:page:0:size:20"),
            anyString(),
            org.mockito.ArgumentMatchers.eq(RedisUserSearchCacheAdapter.CACHE_TTL));
  }

  @Test
  void redisOutageIsFailOpenForEveryOperation() {
    var query = new UserSearchQuery("ami", 0, 20);
    var failure = new RedisConnectionFailureException("redis unavailable");
    when(values.get(anyString())).thenThrow(failure);
    doThrow(failure).when(values).increment(anyString());

    assertThat(adapter.get(query)).isInstanceOf(UserSearchCacheLookup.Unavailable.class);
    assertThatCode(() -> adapter.put(query, result(query), new UserSearchCacheToken("0")))
        .doesNotThrowAnyException();
    assertThatCode(adapter::incrementGeneration).doesNotThrowAnyException();
  }

  private static UserSearchResult result(UserSearchQuery query) {
    var item = new UserSearchItem(UUID.randomUUID(), "ami_1", "Amir", null);
    return new UserSearchResult(List.of(item), query.page(), query.size(), false);
  }
}
