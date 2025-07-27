package com.newsfeed.demo.user.repository;

import com.newsfeed.demo.user.entity.Follow;
import com.newsfeed.demo.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

  // 팔로잉 관계 확인
  Optional<Follow> findByFollowerAndFollowing(User follower, User following);

  // 팔로잉 여부 확인
  boolean existsByFollowerAndFollowing(User follower, User following);

  // 팔로잉 목록 조회
  @Query("SELECT f.following FROM Follow f WHERE f.follower = :follower")
  List<User> findFollowingByFollower(User follower);

  // 팔로워 목록 조회
  @Query("SELECT f.follower FROM Follow f WHERE f.following = :following")
  List<User> findFollowersByFollowing(User following);

  // 팔로잉 수 조회
  long countByFollower(User follower);

  // 팔로워 수 조회
  long countByFollowing(User following);
}
