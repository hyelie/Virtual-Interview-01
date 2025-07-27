package com.newsfeed.demo.feed.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.newsfeed.demo.feed.dto.CreatePostRequest;
import com.newsfeed.demo.feed.dto.NewsFeedResponse;
import com.newsfeed.demo.feed.dto.PostDto;
import com.newsfeed.demo.feed.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

  private final FeedService feedService;

  /**
   * 포스트 생성
   */
  @PostMapping("/posts")
  public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostRequest request) {
    PostDto post =
        feedService.createPost(request.getUserId(), request.getContent(), request.getMediaUrls());
    return ResponseEntity.ok(post);
  }

  /**
   * 뉴스피드 조회
   */
  @GetMapping
  public ResponseEntity<NewsFeedResponse> getNewsFeed(@RequestParam Long userId,
      @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "20") int limit) {

    NewsFeedResponse response = feedService.getNewsFeedWithUserInfo(userId, cursor, limit);
    return ResponseEntity.ok(response);
  }
}
