package com.newsfeed.demo.user.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.newsfeed.demo.user.dto.UserDto;
import com.newsfeed.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
    UserDto createdUser = userService.createUser(userDto);
    return ResponseEntity.ok(createdUser);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
    UserDto user = userService.getUserById(userId);
    return ResponseEntity.ok(user);
  }

  @GetMapping("/{userId}/following")
  public ResponseEntity<List<UserDto>> getFollowing(@PathVariable Long userId) {
    List<UserDto> following = userService.getFollowingList(userId);
    return ResponseEntity.ok(following);
  }

  @GetMapping("/{userId}/followers")
  public ResponseEntity<List<UserDto>> getFollowers(@PathVariable Long userId) {
    List<UserDto> followers = userService.getFollowersList(userId);
    return ResponseEntity.ok(followers);
  }

  @PostMapping("/{followerId}/follow/{userId}")
  public ResponseEntity<Void> followUser(@PathVariable Long followerId, @PathVariable Long userId) {

    userService.followUser(followerId, userId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{followerId}/follow/{userId}")
  public ResponseEntity<Void> unfollowUser(@PathVariable Long followerId,
      @PathVariable Long userId) {

    userService.unfollowUser(followerId, userId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{followerId}/is-following/{userId}")
  public ResponseEntity<Boolean> isFollowing(@PathVariable Long followerId,
      @PathVariable Long userId) {

    boolean isFollowing = userService.isFollowing(followerId, userId);
    return ResponseEntity.ok(isFollowing);
  }
}
