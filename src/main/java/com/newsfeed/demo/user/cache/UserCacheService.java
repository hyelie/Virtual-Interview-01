package com.newsfeed.demo.user.cache;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String USER_CACHE_KEY_PREFIX = "user:";
  private static final int USER_CACHE_TTL = 1800; // 30분

  /**
   * 사용자 데이터 캐시에 저장
   */
  public void cacheUser(Long userId, Object userData) {
    String key = USER_CACHE_KEY_PREFIX + userId;
    try {
      redisTemplate.opsForValue().set(key, userData, USER_CACHE_TTL, TimeUnit.SECONDS);
      log.debug("사용자 캐시 저장: userId={}", userId);
    } catch (Exception e) {
      log.error("사용자 캐시 저장 실패: userId={}", userId, e);
    }
  }

  /**
   * 사용자 데이터 캐시에서 조회
   */
  public Object getCachedUser(Long userId) {
    String key = USER_CACHE_KEY_PREFIX + userId;
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("사용자 캐시 조회 실패: userId={}", userId, e);
      return null;
    }
  }

  /**
   * 사용자 캐시 무효화
   */
  public void invalidateUser(Long userId) {
    String key = USER_CACHE_KEY_PREFIX + userId;
    try {
      redisTemplate.delete(key);
      log.debug("사용자 캐시 무효화: userId={}", userId);
    } catch (Exception e) {
      log.error("사용자 캐시 무효화 실패: userId={}", userId, e);
    }
  }
}
