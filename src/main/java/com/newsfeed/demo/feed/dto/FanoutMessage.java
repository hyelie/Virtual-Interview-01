package com.newsfeed.demo.feed.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FanoutMessage implements Serializable {
  private Long authorId;
  private Long postId;
  private List<Long> followerIds;
}
