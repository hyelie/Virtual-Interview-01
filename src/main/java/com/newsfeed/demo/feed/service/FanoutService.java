package com.newsfeed.demo.feed.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.newsfeed.demo.config.RabbitMQConstants;
import com.newsfeed.demo.feed.dto.FanoutMessage;
import com.newsfeed.demo.feed.entity.Post;
import com.newsfeed.demo.user.entity.User;
import com.newsfeed.demo.user.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 팬아웃 서비스 - 포스트 생성 시 팔로워들에게 비동기로 전파
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FanoutService {

  private final FollowRepository followRepository;
  private final RabbitTemplate rabbitTemplate;

  /**
   * 새 포스트를 팔로워들의 뉴스피드에 푸시
   */
  public void fanoutToFollowers(User author, Post post) {
    try {
      log.info("Fanout 시작: authorId={}, postId={}", author.getId(), post.getId());

      // 팔로워 목록 조회
      List<User> followers = followRepository.findFollowersByFollowing(author);
      List<Long> followerIds = followers.stream().map(User::getId).collect(Collectors.toList());

      log.info("팔로워 수: {}", followerIds.size());

      // 비동기 메시지 전송
      FanoutMessage fanoutMessage = new FanoutMessage(author.getId(), post.getId(), followerIds);

      // RabbitMQ로 비동기 메시지 전송
      rabbitTemplate.convertAndSend(RabbitMQConstants.FANOUT_EXCHANGE,
          RabbitMQConstants.FANOUT_ROUTING_KEY, fanoutMessage);

      log.info("Fanout 메시지 큐 전송 완료: postId={}, 팔로워 수={}", post.getId(), followerIds.size());

    } catch (Exception e) {
      log.error("Fanout 처리 중 오류 발생: authorId={}, postId={}", author.getId(), post.getId(), e);
    }
  }
}
