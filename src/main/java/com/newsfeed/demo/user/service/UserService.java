package com.newsfeed.demo.user.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.newsfeed.demo.feed.cache.NewsFeedCacheService;
import com.newsfeed.demo.user.dto.UserDto;
import com.newsfeed.demo.user.entity.Follow;
import com.newsfeed.demo.user.entity.User;
import com.newsfeed.demo.user.repository.FollowRepository;
import com.newsfeed.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;
  private final NewsFeedCacheService newsFeedCacheService;

  public UserDto createUser(UserDto userDto) {
    User user = new User();
    user.setUsername(userDto.getUsername());
    user.setEmail(userDto.getEmail());

    User savedUser = userRepository.save(user);
    return convertToDto(savedUser, 0, 0);
  }

  public UserDto getUserById(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

    long followingCount = followRepository.countByFollower(user);
    long followersCount = followRepository.countByFollowing(user);

    return convertToDto(user, followingCount, followersCount);
  }

  public List<UserDto> getFollowingList(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

    List<User> followingUsers = followRepository.findFollowingByFollower(user);

    return followingUsers.stream().map(this::convertToSimpleDto).collect(Collectors.toList());
  }

  public List<UserDto> getFollowersList(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

    List<User> followers = followRepository.findFollowersByFollowing(user);

    return followers.stream().map(this::convertToSimpleDto).collect(Collectors.toList());
  }

  public void followUser(Long followerId, Long userId) {
    User follower = userRepository.findById(followerId)
        .orElseThrow(() -> new RuntimeException("팔로워 사용자를 찾을 수 없습니다."));
    User userToFollow = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("팔로우할 사용자를 찾을 수 없습니다."));

    if (followerId.equals(userId)) {
      throw new RuntimeException("자기 자신을 팔로우할 수 없습니다.");
    }

    if (followRepository.existsByFollowerAndFollowing(follower, userToFollow)) {
      throw new RuntimeException("이미 팔로우하고 있습니다.");
    }

    Follow follow = new Follow();
    follow.setFollower(follower);
    follow.setFollowing(userToFollow);
    followRepository.save(follow);

    // 팔로우 관계 변경으로 인한 뉴스피드 캐시 무효화
    newsFeedCacheService.invalidateUserFeed(followerId);
  }

  public void unfollowUser(Long followerId, Long userId) {
    User follower = userRepository.findById(followerId)
        .orElseThrow(() -> new RuntimeException("팔로워 사용자를 찾을 수 없습니다."));
    User userToUnfollow = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("언팔로우할 사용자를 찾을 수 없습니다."));

    Follow follow = followRepository.findByFollowerAndFollowing(follower, userToUnfollow)
        .orElseThrow(() -> new RuntimeException("팔로우 관계를 찾을 수 없습니다."));

    followRepository.delete(follow);

    // 언팔로우 관계 변경으로 인한 뉴스피드 캐시 무효화
    newsFeedCacheService.invalidateUserFeed(followerId);
  }

  public boolean isFollowing(Long followerId, Long userId) {
    User follower = userRepository.findById(followerId)
        .orElseThrow(() -> new RuntimeException("팔로워 사용자를 찾을 수 없습니다."));
    User userToCheck = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("확인할 사용자를 찾을 수 없습니다."));

    return followRepository.existsByFollowerAndFollowing(follower, userToCheck);
  }

  private UserDto convertToDto(User user, long followingCount, long followersCount) {
    return UserDto.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
        .createdAt(user.getCreatedAt()).followingCount(followingCount)
        .followersCount(followersCount).build();
  }

  private UserDto convertToSimpleDto(User user) {
    return UserDto.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
        .createdAt(user.getCreatedAt()).followingCount(0).followersCount(0).build();
  }
}
