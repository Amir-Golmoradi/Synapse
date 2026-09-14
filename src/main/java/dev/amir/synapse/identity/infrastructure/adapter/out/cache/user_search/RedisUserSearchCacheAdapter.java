package dev.amir.synapse.identity.infrastructure.adapter.out.cache.user_search;

import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheLookup;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCachePort;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheToken;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisUserSearchCacheAdapter implements UserSearchCachePort {
  static final Duration CACHE_TTL = Duration.ofSeconds(60);

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisUserSearchCacheAdapter.class);
  private static final String KEY_PREFIX = "identity:user-search:v1";
  private static final String GENERATION_KEY = KEY_PREFIX + ":generation";
  private static final String INITIAL_GENERATION = "0";

  private final RedisTemplate<String, String> redis;
  private final ObjectMapper objectMapper;
  private final Duration cacheTtl;

  public RedisUserSearchCacheAdapter(
      @Qualifier("redisTemplate") RedisTemplate<String, String> redis,
      ObjectMapper objectMapper,
      @Value("${synapse.user-search.cache-ttl:60s}") Duration cacheTtl) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.cacheTtl = cacheTtl;
  }

  @Override
  public UserSearchCacheLookup get(UserSearchQuery query) {
    try {
      String generation = currentGeneration();
      String json = redis.opsForValue().get(sliceKey(generation, query));
      if (json == null) {
        return new UserSearchCacheLookup.Miss(new UserSearchCacheToken(generation));
      }
      return new UserSearchCacheLookup.Hit(objectMapper.readValue(json, UserSearchResult.class));
    } catch (RuntimeException exception) {
      LOGGER.debug("User search cache read failed; using PostgreSQL", exception);
      return new UserSearchCacheLookup.Unavailable();
    }
  }

  @Override
  public void put(UserSearchQuery query, UserSearchResult result, UserSearchCacheToken token) {
    try {
      String json = objectMapper.writeValueAsString(result);
      redis.opsForValue().set(sliceKey(token.value(), query), json, cacheTtl);
    } catch (RuntimeException exception) {
      LOGGER.debug("User search cache write failed; result was not cached", exception);
    }
  }

  @Override
  public void incrementGeneration() {
    try {
      redis.opsForValue().increment(GENERATION_KEY);
    } catch (RuntimeException exception) {
      LOGGER.debug("User search cache generation increment failed", exception);
    }
  }

  private String currentGeneration() {
    String generation = redis.opsForValue().get(GENERATION_KEY);
    return generation == null ? INITIAL_GENERATION : generation;
  }

  private static String sliceKey(String generation, UserSearchQuery query) {
    return "%s:g:%s:prefix:%s:page:%d:size:%d"
        .formatted(KEY_PREFIX, generation, query.prefix(), query.page(), query.size());
  }
}
