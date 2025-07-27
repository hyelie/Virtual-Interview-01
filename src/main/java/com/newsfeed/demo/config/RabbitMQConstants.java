package com.newsfeed.demo.config;

/**
 * RabbitMQ 관련 상수 정의
 */
public final class RabbitMQConstants {

  // 큐 이름
  public static final String FANOUT_QUEUE = "fanout.queue";

  // 익스체인지 이름
  public static final String FANOUT_EXCHANGE = "fanout.exchange";

  // 라우팅 키
  public static final String FANOUT_ROUTING_KEY = "fanout.routing.key";

  private RabbitMQConstants() {
    // 유틸리티 클래스이므로 인스턴스화 방지
  }
}
