package com.newsfeed.demo.monitoring;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

  private final MonitoringService monitoringService;

  @GetMapping("/metrics")
  public Map<String, Object> getMetrics() {
    return monitoringService.getSystemMetrics();
  }

  @GetMapping("/health")
  public Map<String, String> getHealth() {
    return Map.of("status", "UP", "service", "NewsFeed Monitor");
  }
}
