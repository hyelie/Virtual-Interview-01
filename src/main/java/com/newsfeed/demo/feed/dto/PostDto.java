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
public class PostDto {
  private Long id;
  private UserDto user; // 사용자 정보 (캐시에서 조회 시 포함)
  private String content;
  private List<String> mediaUrls;
  private LocalDateTime createdAt;
}
