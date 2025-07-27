package com.newsfeed.demo.feed.repository;

import com.newsfeed.demo.feed.entity.Post;
import com.newsfeed.demo.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

  List<Post> findByUserOrderByIdDesc(User user, PageRequest pageRequest);

  @Query("SELECT p FROM Post p WHERE p.user IN :users ORDER BY p.id DESC")
  List<Post> findByUserInOrderByIdDesc(@Param("users") List<User> users, PageRequest pageRequest);

  @Query("SELECT p FROM Post p WHERE p.user IN :users AND p.id < :cursor ORDER BY p.id DESC")
  List<Post> findByUserInAndIdLessThanOrderByIdDesc(@Param("users") List<User> users, @Param("cursor") Long cursor,
      PageRequest pageRequest);
}
