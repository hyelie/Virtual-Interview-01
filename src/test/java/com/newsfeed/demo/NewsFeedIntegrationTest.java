package com.newsfeed.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsfeed.demo.feed.dto.CreatePostRequest;
import com.newsfeed.demo.feed.dto.PostDto;
import com.newsfeed.demo.user.dto.UserDto;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NewsFeedIntegrationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  public void testNewsFeedFlow() throws Exception {
    // 1. 사용자 생성
    UserDto user1 = createUser("user1", "user1@example.com");
    UserDto user2 = createUser("user2", "user2@example.com");
    UserDto user3 = createUser("user3", "user3@example.com");

    // 2. 팔로우 관계 생성
    followUser(user2.getId(), user1.getId());
    followUser(user3.getId(), user1.getId());
    followUser(user3.getId(), user2.getId());

    // 3. 포스트 생성
    PostDto post1 = createPost(user1.getId(), "User1의 첫 번째 포스트");
    PostDto post2 = createPost(user1.getId(), "User1의 두 번째 포스트");
    PostDto post3 = createPost(user2.getId(), "User2의 포스트");

    // 4. 뉴스피드 조회 테스트 (첫 번째 요청 - Cache Miss 발생)
    mockMvc.perform(get("/api/feed").param("userId", user3.getId().toString()).param("limit", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.posts").isArray())
        .andExpect(jsonPath("$.posts.length()").value(3));

    // 5. 뉴스피드 조회 테스트 (두 번째 요청 - 캐시에서 조회)
    mockMvc.perform(get("/api/feed").param("userId", user3.getId().toString()).param("limit", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.posts").isArray())
        .andExpect(jsonPath("$.posts.length()").value(3));

    // 6. 팔로워/팔로잉 조회 테스트
    mockMvc.perform(get("/api/users/{userId}/followers", user1.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc.perform(get("/api/users/{userId}/following", user3.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  public void testCacheMissAndRebuild() throws Exception {
    // 1. 사용자 생성
    UserDto user1 = createUser("cacheuser1", "cacheuser1@example.com");
    UserDto user2 = createUser("cacheuser2", "cacheuser2@example.com");

    // 2. 팔로우 관계 생성
    followUser(user2.getId(), user1.getId());

    // 3. 포스트 생성
    createPost(user1.getId(), "캐시 테스트용 포스트");

    // 4. 첫 번째 뉴스피드 조회 (Cache Miss 발생)
    mockMvc.perform(get("/api/feed").param("userId", user2.getId().toString()).param("limit", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.posts").isArray())
        .andExpect(jsonPath("$.posts.length()").value(1));

    // 5. 두 번째 뉴스피드 조회 (캐시에서 조회)
    mockMvc.perform(get("/api/feed").param("userId", user2.getId().toString()).param("limit", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.posts").isArray())
        .andExpect(jsonPath("$.posts.length()").value(1));
  }

  private UserDto createUser(String username, String email) throws Exception {
    UserDto userDto = UserDto.builder().username(username).email(email).build();

    String userJson = objectMapper.writeValueAsString(userDto);

    String response = mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(userJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    return objectMapper.readValue(response, UserDto.class);
  }

  private void followUser(Long followerId, Long followingId) throws Exception {
    mockMvc.perform(post("/api/users/{followerId}/follow/{userId}", followerId, followingId))
        .andExpect(status().isOk());
  }

  private PostDto createPost(Long userId, String content) throws Exception {
    CreatePostRequest request = new CreatePostRequest();
    request.setUserId(userId);
    request.setContent(content);

    String postJson = objectMapper.writeValueAsString(request);

    String response = mockMvc
        .perform(post("/api/feed/posts").contentType(MediaType.APPLICATION_JSON).content(postJson))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    return objectMapper.readValue(response, PostDto.class);
  }
}
