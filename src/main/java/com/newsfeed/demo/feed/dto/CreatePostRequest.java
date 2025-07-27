package com.newsfeed.demo.feed.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

  private Long userId;

  @NotBlank(message = "포스트 내용은 필수입니다.")
  @Size(max = 2000, message = "포스트 내용은 2000자를 초과할 수 없습니다.")
  private String content;

  private List<String> mediaUrls = new ArrayList<>(); // 향후 미디어 확장용
}
