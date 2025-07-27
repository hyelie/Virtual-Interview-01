package com.newsfeed.demo.feed.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsFeedCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String NEWS_FEED_KEY_PREFIX = "newsfeed:user:";
  private static final int NEWS_FEED_CACHE_TTL = 3600; // 1시간
  private static final int MAX_FEED_SIZE = 1000; // 최대 캐시 크기 (1000개 포스트)
  private static final int CACHE_MISS_THRESHOLD = 100; // Cache Miss 임계값 (100개 미만이면 Cache Miss로 간주)

  /**
   * 사용자의 뉴스피드에 포스트 ID 추가 (Fanout)
   * 캐시 크기 제한을 적용하여 최신 포스트만 유지
   */
  public void addToUserFeed(Long userId, Long postId) {
    String key = NEWS_FEED_KEY_PREFIX + userId;
    try {
      // 포스트 추가
      redisTemplate.opsForZSet().add(key, postId.toString(), System.currentTimeMillis());

      // 캐시 크기 제한 적용
      Long currentSize = redisTemplate.opsForZSet().size(key);
      if (currentSize != null && currentSize > MAX_FEED_SIZE) {
        // 가장 오래된 포스트들을 제거하여 크기 제한 유지
        redisTemplate.opsForZSet().removeRange(key, 0, currentSize - MAX_FEED_SIZE - 1);
        log.debug("뉴스피드 캐시 크기 제한 적용: userId={}, 제거된 포스트 수={}", userId, currentSize - MAX_FEED_SIZE);
      }

      redisTemplate.expire(key, NEWS_FEED_CACHE_TTL, TimeUnit.SECONDS);
      log.debug("뉴스피드 캐시에 포스트 추가: userId={}, postId={}", userId, postId);
    } catch (Exception e) {
      log.error("뉴스피드 캐시 추가 실패: userId={}, postId={}", userId, postId, e);
    }
  }

  /**
   * 사용자의 뉴스피드에서 포스트 ID 제거
   */
  public void removeFromUserFeed(Long userId, Long postId) {
    String key = NEWS_FEED_KEY_PREFIX + userId;
    try {
      redisTemplate.opsForZSet().remove(key, postId.toString());
      log.debug("뉴스피드 캐시에서 포스트 제거: userId={}, postId={}", userId, postId);
    } catch (Exception e) {
      log.error("뉴스피드 캐시 제거 실패: userId={}, postId={}", userId, postId, e);
    }
  }

  /**
   * 사용자의 뉴스피드 조회 (캐시에서)
   * Cache Miss 감지 및 처리 로직 포함
   */
  public List<Long> getUserFeed(Long userId, Long cursor, int size) {
    String key = NEWS_FEED_KEY_PREFIX + userId;
    try {
      Set<Object> postIds;
      if (cursor != null) {
        // 커서 기반 페이지네이션
        postIds = redisTemplate.opsForZSet().reverseRangeByScore(
            key, 0, cursor - 1, 0, size);
      } else {
        // 최신 포스트부터 조회
        postIds = redisTemplate.opsForZSet().reverseRange(key, 0, size - 1);
      }

      if (postIds != null) {
        List<Long> result = postIds.stream()
            .map(id -> Long.valueOf(id.toString()))
            .toList();

        // Cache Miss 감지: 요청한 크기보다 훨씬 적은 데이터가 있으면 Cache Miss로 간주
        if (result.size() < CACHE_MISS_THRESHOLD && result.size() < size) {
          log.warn("Cache Miss 감지: userId={}, 요청 크기={}, 실제 크기={}", userId, size, result.size());
          // Cache Miss 이벤트를 기록하거나 알림을 보낼 수 있음
        }

        return result;
      }
    } catch (Exception e) {
      log.error("뉴스피드 캐시 조회 실패: userId={}", userId, e);
    }
    return List.of();
  }

  /**
   * Cache Miss 여부 확인
   */
  public boolean isCacheMiss(Long userId, int requestedSize) {
    String key = NEWS_FEED_KEY_PREFIX + userId;
    try {
      Long cacheSize = redisTemplate.opsForZSet().size(key);
      return cacheSize != null && cacheSize < CACHE_MISS_THRESHOLD && cacheSize < requestedSize;
    } catch (Exception e) {
      log.error("Cache Miss 확인 실패: userId={}", userId, e);
      return true; // 에러 발생 시 Cache Miss로 간주
    }
  }

  /**
   * 사용자의 뉴스피드 캐시 무효화
   */
  public void invalidateUserFeed(Long userId) {
    String key = NEWS_FEED_KEY_PREFIX + userId;
    try {
      redisTemplate.delete(key);
      log.debug("뉴스피드 캐시 무효화: userId={}", userId);
    } catch (Exception e) {
      log.error("뉴스피드 캐시 무효화 실패: userId={}", userId, e);
    }
  }
}
