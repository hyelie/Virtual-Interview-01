package com.newsfeed.demo.feed.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.newsfeed.demo.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsFeedResponse {
  private List<PostWithUserDto> posts;
  private Long nextCursor;
  private boolean hasMore;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostWithUserDto {
    private Long id;
    private UserDto user;
    private String content;
    private List<String> mediaUrls;
    private LocalDateTime createdAt;
  }
}
