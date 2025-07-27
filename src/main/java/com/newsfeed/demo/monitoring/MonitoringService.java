package com.newsfeed.demo.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RestTemplate restTemplate;

  public Map<String, Object> getSystemMetrics() {
    Map<String, Object> metrics = new HashMap<>();

    // JVM 메모리 정보
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    Map<String, Object> memoryInfo = new HashMap<>();
    memoryInfo.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
    memoryInfo.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
    memoryInfo.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
    memoryInfo.put("heapUsagePercent",
        (double) memoryBean.getHeapMemoryUsage().getUsed() / memoryBean.getHeapMemoryUsage().getMax() * 100);
    metrics.put("memory", memoryInfo);

    // CPU 정보
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Map<String, Object> cpuInfo = new HashMap<>();
    cpuInfo.put("systemLoad", osBean.getSystemLoadAverage());
    cpuInfo.put("availableProcessors", osBean.getAvailableProcessors());
    metrics.put("cpu", cpuInfo);

    // Redis 정보
    try {
      Map<String, Object> redisInfo = getRedisInfo();
      metrics.put("redis", redisInfo);
    } catch (Exception e) {
      log.error("Redis 정보 조회 실패", e);
      metrics.put("redis", Map.of("error", e.getMessage()));
    }

    // RabbitMQ 정보
    try {
      Map<String, Object> rabbitmqInfo = getRabbitMQInfo();
      metrics.put("rabbitmq", rabbitmqInfo);
    } catch (Exception e) {
      log.error("RabbitMQ 정보 조회 실패", e);
      metrics.put("rabbitmq", Map.of("error", e.getMessage()));
    }

    return metrics;
  }

  private Map<String, Object> getRedisInfo() {
    Map<String, Object> redisInfo = new HashMap<>();

    try {
      // Redis 연결 상태 확인
      String pong = redisTemplate.getConnectionFactory().getConnection().ping();
      redisInfo.put("status", "connected");
      redisInfo.put("ping", pong);

      // Redis 메모리 정보
      Properties info = redisTemplate.getConnectionFactory().getConnection().info("memory");
      redisInfo.put("usedMemory", info.getProperty("used_memory"));
      redisInfo.put("usedMemoryPeak", info.getProperty("used_memory_peak"));
      redisInfo.put("usedMemoryRss", info.getProperty("used_memory_rss"));

      // Redis 키 개수
      Long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();
      redisInfo.put("keyCount", dbSize);

    } catch (Exception e) {
      redisInfo.put("status", "disconnected");
      redisInfo.put("error", e.getMessage());
    }

    return redisInfo;
  }

  private Map<String, Object> getRabbitMQInfo() {
    Map<String, Object> rabbitmqInfo = new HashMap<>();

    try {
      // RabbitMQ Management API를 통해 정보 조회
      String baseUrl = "http://localhost:15672/api";

      // 연결 정보
      String overviewUrl = baseUrl + "/overview";
      Map<String, Object> overview = restTemplate.getForObject(overviewUrl, Map.class);

      if (overview != null) {
        rabbitmqInfo.put("status", "connected");
        rabbitmqInfo.put("version", overview.get("rabbitmq_version"));
        rabbitmqInfo.put("totalConnections", overview.get("object_totals"));
      } else {
        rabbitmqInfo.put("status", "disconnected");
      }

    } catch (Exception e) {
      rabbitmqInfo.put("status", "disconnected");
      rabbitmqInfo.put("error", e.getMessage());
    }

    return rabbitmqInfo;
  }
}
