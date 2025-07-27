package com.newsfeed.demo.feed.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.newsfeed.demo.feed.cache.NewsFeedCacheService;
import com.newsfeed.demo.feed.cache.PostCacheService;
import com.newsfeed.demo.feed.dto.NewsFeedResponse;
import com.newsfeed.demo.feed.dto.PostDto;
import com.newsfeed.demo.feed.entity.Post;
import com.newsfeed.demo.feed.repository.PostRepository;
import com.newsfeed.demo.user.cache.UserCacheService;
import com.newsfeed.demo.user.dto.UserDto;
import com.newsfeed.demo.user.entity.User;
import com.newsfeed.demo.user.repository.FollowRepository;
import com.newsfeed.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 피드 서비스 - 뉴스피드의 핵심 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeedService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final FollowRepository followRepository;
  private final NewsFeedCacheService newsFeedCacheService;
  private final PostCacheService postCacheService;
  private final UserCacheService userCacheService;
  private final FanoutService fanoutService;

  /**
   * 포스트 생성 및 팔로워들에게 팬아웃
   */
  public PostDto createPost(Long userId, String content, List<String> mediaUrls) {
    User user = findUserById(userId);
    Post post = Post.builder().user(user).content(content).mediaUrls(mediaUrls).build();
    Post savedPost = postRepository.save(post);

    log.info("포스트 생성 완료: postId={}, userId={}", savedPost.getId(), userId);

    // 비동기 팬아웃 처리
    fanoutService.fanoutToFollowers(user, savedPost);

    return convertToDto(savedPost);
  }

  /**
   * 사용자 정보를 포함한 뉴스피드 조회 (API 응답용)
   */
  public NewsFeedResponse getNewsFeedWithUserInfo(Long userId, Long cursor, int size) {
    List<Post> posts = getNewsFeed(userId, cursor, size);

    // 모든 필요한 사용자 ID 수집
    Set<Long> userIds =
        posts.stream().map(post -> post.getUser().getId()).collect(Collectors.toSet());

    // 배치로 사용자 정보 조회
    List<User> users = getUsersFromCacheOrDatabaseBatch(userIds);
    Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

    // Post Entity를 PostWithUserDto로 변환
    List<NewsFeedResponse.PostWithUserDto> postsWithUser = posts.stream().map(post -> {
      User user = userMap.get(post.getUser().getId());
      UserDto userDto = user != null ? convertToUserDto(user) : null;

      return NewsFeedResponse.PostWithUserDto.builder().id(post.getId()).user(userDto)
          .content(post.getContent()).mediaUrls(post.getMediaUrls()).createdAt(post.getCreatedAt())
          .build();
    }).collect(Collectors.toList());

    boolean hasMore = posts.size() == size;
    Long nextCursor = hasMore && !posts.isEmpty() ? posts.get(posts.size() - 1).getId() : null;

    return NewsFeedResponse.builder().posts(postsWithUser).hasMore(hasMore).nextCursor(nextCursor)
        .build();
  }

  /**
   * 사용자의 뉴스피드 조회 (캐시 우선, DB 폴백)
   */
  private List<Post> getNewsFeed(Long userId, Long cursor, int size) {
    User user = findUserById(userId);

    // 뉴스피드 캐시에서 조회
    List<Long> postIds = getNewsFeedFromCacheOrDatabase(userId, cursor, size);

    // 포스트 정보 조회
    List<Post> posts = getPostsFromCacheOrDatabaseBatch(postIds);

    return posts;
  }

  /**
   * 뉴스피드 캐시에서 조회하고, 미스 시 DB에서 재구성
   */
  private List<Long> getNewsFeedFromCacheOrDatabase(Long userId, Long cursor, int size) {
    // Cache Miss 확인
    if (newsFeedCacheService.isCacheMiss(userId, size)) {
      log.debug("뉴스피드 캐시 미스 감지: userId={}, requestedSize={}", userId, size);
      return rebuildNewsFeedCache(userId, cursor, size);
    }

    // 캐시에서 뉴스피드 조회
    List<Long> cachedPostIds = newsFeedCacheService.getUserFeed(userId, cursor, size);
    if (cachedPostIds.isEmpty()) {
      log.debug("뉴스피드 캐시 미스, DB에서 뉴스피드 조회: userId={}", userId);
      return rebuildNewsFeedCache(userId, cursor, size);
    }

    log.debug("뉴스피드 캐시에서 조회: userId={}, postCount={}", userId, cachedPostIds.size());
    return cachedPostIds;
  }

  /**
   * 포스트 목록을 배치로 캐시에서 조회하고, 미스 시 DB에서 조회
   */
  private List<Post> getPostsFromCacheOrDatabaseBatch(List<Long> postIds) {
    if (postIds.isEmpty()) {
      return List.of();
    }

    // 1. 캐시에서 배치 조회
    List<Post> posts = new ArrayList<>();
    List<Long> missedPostIds = new ArrayList<>();

    for (Long postId : postIds) {
      Object cachedPost = postCacheService.getCachedPost(postId);
      if (cachedPost instanceof Post) {
        posts.add((Post) cachedPost);
      } else {
        missedPostIds.add(postId);
      }
    }

    log.debug("포스트 캐시 조회 결과: total={}, cached={}, missed={}", postIds.size(), posts.size(),
        missedPostIds.size());

    // 2. 캐시 미스된 포스트들을 DB에서 배치 조회
    if (!missedPostIds.isEmpty()) {
      log.debug("포스트 캐시 미스, DB에서 조회: postIds={}", missedPostIds);
      List<Post> dbPosts = postRepository.findAllById(missedPostIds);

      // DB에서 조회한 포스트들을 캐시에 저장
      for (Post post : dbPosts) {
        postCacheService.cachePost(post.getId(), post);
        posts.add(post);
      }

      log.debug("포스트 DB 조회 및 캐싱 완료: cachedCount={}", dbPosts.size());
    }

    return posts;
  }

  /**
   * 사용자들을 배치로 캐시에서 조회하고, 미스 시 DB에서 조회
   */
  private List<User> getUsersFromCacheOrDatabaseBatch(Set<Long> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }

    // 1. 캐시에서 배치 조회
    List<User> users = new ArrayList<>();
    List<Long> missedUserIds = new ArrayList<>();

    for (Long userId : userIds) {
      Object cachedUser = userCacheService.getCachedUser(userId);
      if (cachedUser instanceof User) {
        users.add((User) cachedUser);
      } else {
        missedUserIds.add(userId);
      }
    }

    log.debug("사용자 캐시 조회 결과: total={}, cached={}, missed={}", userIds.size(), users.size(),
        missedUserIds.size());

    // 2. 캐시 미스된 사용자들을 DB에서 배치 조회
    if (!missedUserIds.isEmpty()) {
      log.debug("사용자 캐시 미스, DB에서 조회: userIds={}", missedUserIds);
      List<User> dbUsers = userRepository.findAllById(missedUserIds);

      // DB에서 조회한 사용자들을 캐시에 저장
      for (User user : dbUsers) {
        userCacheService.cacheUser(user.getId(), user);
        users.add(user);
      }

      log.debug("사용자 DB 조회 및 캐싱 완료: cachedCount={}", dbUsers.size());
    }

    return users;
  }

  /**
   * DB에서 뉴스피드 조회하고 뉴스피드 캐시만 재구성
   */
  private List<Long> rebuildNewsFeedCache(Long userId, Long cursor, int size) {
    User user = findUserById(userId);
    List<User> followingUsers = followRepository.findFollowingByFollower(user);
    List<Post> posts = getPostsFromDatabase(followingUsers, cursor, size);

    if (posts.isEmpty()) {
      return List.of();
    }

    // 포스트 ID 수집
    List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

    // 뉴스피드 캐시 재구성 (포스트 ID만 저장)
    posts.forEach(post -> newsFeedCacheService.addToUserFeed(userId, post.getId()));

    log.info("뉴스피드 캐시 재구성 완료: userId={}, postCount={}", userId, posts.size());
    return postIds;
  }

  /**
   * DB에서 포스트 조회
   */
  private List<Post> getPostsFromDatabase(List<User> followingUsers, Long cursor, int size) {
    if (cursor != null) {
      return postRepository.findByUserInAndIdLessThanOrderByIdDesc(followingUsers, cursor,
          PageRequest.of(0, size));
    } else {
      return postRepository.findByUserInOrderByIdDesc(followingUsers, PageRequest.of(0, size));
    }
  }

  /**
   * 사용자 ID로 사용자 조회
   */
  private User findUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
  }

  /**
   * Post 엔티티를 PostDto로 변환
   */
  private PostDto convertToDto(Post post) {
    return PostDto.builder().id(post.getId()).user(convertToUserDto(post.getUser()))
        .content(post.getContent()).mediaUrls(post.getMediaUrls()).createdAt(post.getCreatedAt())
        .build();
  }

  /**
   * User 엔티티를 UserDto로 변환
   */
  private UserDto convertToUserDto(User user) {
    return UserDto.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
        .createdAt(user.getCreatedAt()).build();
  }
}
