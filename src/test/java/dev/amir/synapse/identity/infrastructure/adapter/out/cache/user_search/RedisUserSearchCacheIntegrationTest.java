package dev.amir.synapse.identity.infrastructure.adapter.out.cache.user_search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheLookup;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheToken;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class RedisUserSearchCacheIntegrationTest {
  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static RedisUserSearchCacheAdapter cache;

  @BeforeAll
  static void setUpRedis() {
    var configuration =
        new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory = new LettuceConnectionFactory(configuration);
    connectionFactory.afterPropertiesSet();

    var redis = new RedisTemplate<String, String>();
    var serializer = new StringRedisSerializer();
    redis.setConnectionFactory(connectionFactory);
    redis.setKeySerializer(serializer);
    redis.setValueSerializer(serializer);
    redis.afterPropertiesSet();
    cache =
        new RedisUserSearchCacheAdapter(
            redis, JsonMapper.builder().build(), Duration.ofMillis(500));
  }

  @AfterAll
  static void closeRedisConnection() {
    connectionFactory.destroy();
  }

  @Test
  void cachedSliceExpiresAtConfiguredTtl() {
    var query = new UserSearchQuery("ttl", 0, 20);
    cache.put(query, result(query, "ttl_user"), missToken(query));

    assertThat(cache.get(query)).isInstanceOf(UserSearchCacheLookup.Hit.class);
    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(
            () -> assertThat(cache.get(query)).isInstanceOf(UserSearchCacheLookup.Miss.class));
  }

  @Test
  void generationIncrementMakesExistingSliceUnreachable() {
    var query = new UserSearchQuery("generation", 0, 20);
    cache.put(query, result(query, "generation_user"), missToken(query));

    assertThat(cache.get(query)).isInstanceOf(UserSearchCacheLookup.Hit.class);
    cache.incrementGeneration();

    assertThat(cache.get(query)).isInstanceOf(UserSearchCacheLookup.Miss.class);
  }

  private static UserSearchCacheToken missToken(UserSearchQuery query) {
    var lookup = cache.get(query);
    assertThat(lookup).isInstanceOf(UserSearchCacheLookup.Miss.class);
    return ((UserSearchCacheLookup.Miss) lookup).token();
  }

  private static UserSearchResult result(UserSearchQuery query, String handle) {
    return new UserSearchResult(
        List.of(new UserSearchItem(UUID.randomUUID(), handle, "Cached User", null)),
        query.page(),
        query.size(),
        false);
  }
}
