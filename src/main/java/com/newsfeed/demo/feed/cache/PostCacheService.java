package com.newsfeed.demo.feed.cache;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String POST_CACHE_KEY_PREFIX = "post:";
  private static final int POST_CACHE_TTL = 7200; // 2시간
  private static final int MAX_CACHED_POSTS = 100000; // 최대 캐시할 포스트 수
  private static final String POST_COUNT_KEY = "post_cache:count";

  /**
   * 포스트 데이터 캐시에 저장 (크기 제한 적용)
   */
  public void cachePost(Long postId, Object postData) {
    String key = POST_CACHE_KEY_PREFIX + postId;
    try {
      // 현재 캐시된 포스트 수 확인
      Long currentCount = redisTemplate.opsForValue().increment(POST_COUNT_KEY, 0);
      if (currentCount == null) {
        currentCount = 0L;
      }

      // 크기 제한 확인
      if (currentCount >= MAX_CACHED_POSTS) {
        log.warn("포스트 캐시 크기 제한 도달: currentCount={}, maxCount={}", currentCount, MAX_CACHED_POSTS);
        // 가장 오래된 포스트들을 제거 (TTL 기반으로 자동 만료되지만, 명시적으로 정리)
        cleanupOldPosts();
      }

      // 포스트 캐시에 저장
      redisTemplate.opsForValue().set(key, postData, POST_CACHE_TTL, TimeUnit.SECONDS);

      // 캐시된 포스트 수 증가 (이미 존재하는 키는 증가하지 않음)
      if (!redisTemplate.hasKey(key)) {
        redisTemplate.opsForValue().increment(POST_COUNT_KEY, 1);
      }

      log.debug("포스트 캐시 저장: postId={}, currentCount={}", postId, currentCount);
    } catch (Exception e) {
      log.error("포스트 캐시 저장 실패: postId={}", postId, e);
    }
  }

  /**
   * 포스트 데이터 캐시에서 조회
   */
  public Object getCachedPost(Long postId) {
    String key = POST_CACHE_KEY_PREFIX + postId;
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("포스트 캐시 조회 실패: postId={}", postId, e);
      return null;
    }
  }

  /**
   * 포스트 캐시 무효화
   */
  public void invalidatePost(Long postId) {
    String key = POST_CACHE_KEY_PREFIX + postId;
    try {
      redisTemplate.delete(key);
      // 캐시된 포스트 수 감소
      redisTemplate.opsForValue().decrement(POST_COUNT_KEY, 1);
      log.debug("포스트 캐시 무효화: postId={}", postId);
    } catch (Exception e) {
      log.error("포스트 캐시 무효화 실패: postId={}", postId, e);
    }
  }

  /**
   * 오래된 포스트들을 정리하여 캐시 크기 제한 유지
   */
  private void cleanupOldPosts() {
    try {
      // Redis의 모든 post: 키들을 조회하여 TTL이 짧은 것들을 우선적으로 제거
      Set<String> keys = redisTemplate.keys(POST_CACHE_KEY_PREFIX + "*");
      if (keys != null && keys.size() > MAX_CACHED_POSTS * 0.8) { // 80% 도달 시 정리
        // TTL이 짧은 키들을 찾아서 제거
        keys.stream().filter(key -> {
          Long ttl = redisTemplate.getExpire(key);
          return ttl != null && ttl < POST_CACHE_TTL * 0.5; // TTL이 50% 이하인 것들
        }).limit((long) (keys.size() - MAX_CACHED_POSTS * 0.7)) // 70%까지 줄임
            .forEach(key -> {
              redisTemplate.delete(key);
              redisTemplate.opsForValue().decrement(POST_COUNT_KEY, 1);
            });

        log.info("오래된 포스트 캐시 정리 완료: removedCount={}", keys.size() - MAX_CACHED_POSTS * 0.7);
      }
    } catch (Exception e) {
      log.error("오래된 포스트 캐시 정리 실패", e);
    }
  }
}
