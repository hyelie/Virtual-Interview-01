package com.newsfeed.demo.feed.service;

import java.util.List;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.newsfeed.demo.config.RabbitMQConstants;
import com.newsfeed.demo.feed.cache.NewsFeedCacheService;
import com.newsfeed.demo.feed.dto.FanoutMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 팬아웃 워커 서비스 - RabbitMQ 메시지 큐에서 팬아웃 메시지를 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FanoutWorkerService {

  private final NewsFeedCacheService newsFeedCacheService;

  /**
   * 팬아웃 메시지 처리
   */
  @RabbitListener(queues = RabbitMQConstants.FANOUT_QUEUE)
  public void processFanoutMessage(FanoutMessage fanoutMessage) {
    try {
      log.info("팬아웃 메시지 처리 시작: authorId={}, postId={}, followerCount={}",
          fanoutMessage.getAuthorId(), fanoutMessage.getPostId(),
          fanoutMessage.getFollowerIds().size());

      Long postId = fanoutMessage.getPostId();
      List<Long> followerIds = fanoutMessage.getFollowerIds();

      // 각 팔로워의 뉴스피드에 포스트 ID 추가
      for (Long followerId : followerIds) {
        try {
          newsFeedCacheService.addToUserFeed(followerId, postId);
          log.debug("팔로워 {}의 뉴스피드에 포스트 {} 푸시 완료", followerId, postId);
        } catch (Exception e) {
          log.error("팔로워 {}의 뉴스피드 푸시 실패: postId={}", followerId, postId, e);
        }
      }

      log.info("팬아웃 메시지 처리 완료: postId={}, 팔로워 수={}", postId, followerIds.size());

    } catch (Exception e) {
      log.error("팬아웃 메시지 처리 중 오류 발생: message={}", fanoutMessage, e);
    }
  }
}
