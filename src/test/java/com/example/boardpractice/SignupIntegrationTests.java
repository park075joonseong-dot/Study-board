package com.example.boardpractice;

import com.example.boardpractice.domain.post.repository.PostRepository;
import com.example.boardpractice.domain.comment.repository.CommentRepository;
import com.example.boardpractice.domain.user.entity.User;
import com.example.boardpractice.domain.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupIntegrationTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearTestDatabase() {
        commentRepository.deleteAll();
        userRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Test
    void signupStoresHashedPasswordAndStartsUnverified() throws Exception {
        signup("STUDENT@example.com", "홍길동", "Practice123!")
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        User user = userRepository.findByEmail("student@example.com").orElseThrow();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getPasswordHash()).isNotEqualTo("Practice123!");
        assertThat(passwordEncoder.matches("Practice123!", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("AnotherPassword!", user.getPasswordHash())).isFalse();
    }

    @Test
    void duplicateEmailReturns409AndPreservesFirstMember() throws Exception {
        signup("student@example.com", "홍길동", "Practice123!")
                .andExpect(status().isCreated());

        signup("student@example.com", "다른이름", "AnotherPassword!")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("이메일이 중복되었습니다."))
                .andExpect(jsonPath("$.detail").value("이미 가입된 이메일입니다."));

        assertThat(userRepository.count()).isEqualTo(1);
        User user = userRepository.findByEmail("student@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(passwordEncoder.matches("Practice123!", user.getPasswordHash())).isTrue();
    }

    @Test
    void emailCaseDifferencesStillCountAsDuplicate() throws Exception {
        signup("student@example.com", "홍길동", "Practice123!")
                .andExpect(status().isCreated());
        signup("STUDENT@EXAMPLE.COM", "홍길동", "Practice123!")
                .andExpect(status().isConflict());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void databaseAlsoEnforcesUniqueEmail() {
        userRepository.saveAndFlush(new User("student@example.com", "first-hash", "홍길동"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("student@example.com", "second-hash", "다른이름")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidEmailReturns400() throws Exception {
        signup("invalid-email", "홍길동", "Practice123!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "name", "password"})
    void blankFieldReturns400(String field) throws Exception {
        Map<String, String> request = new java.util.HashMap<>(Map.of(
                "email", "student@example.com", "name", "홍길동", "password", "Practice123!"));
        request.put(field, "   ");

        mvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).exists());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void oversizedNameReturns400() throws Exception {
        signup("student@example.com", "가".repeat(101), "Practice123!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void oversizedEmailReturns400() throws Exception {
        signup("a".repeat(243) + "@example.com", "홍길동", "Practice123!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void passwordAboveBcryptByteLimitReturns400() throws Exception {
        signup("student@example.com", "홍길동", "가".repeat(25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요."));
        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "가"})
    void passwordAtBcryptByteLimitCanBeVerified(String character) throws Exception {
        int byteLength = character.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        String password = character.repeat(72 / byteLength);
        signup("student@example.com", "홍길동", password)
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("student@example.com").orElseThrow();
        assertThat(passwordEncoder.matches(password, user.getPasswordHash())).isTrue();
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void existingPostCrudStillWorks() throws Exception {
        String createdPost = mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"원래 제목\",\"content\":\"원래 내용\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long postId = objectMapper.readTree(createdPost).get("postId").asLong();
        String path = "/api/posts/" + postId;

        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("원래 제목"));
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정 제목\",\"content\":\"수정 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 제목"));
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정 내용"));
        mvc.perform(delete(path)).andExpect(status().isNoContent());
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    private ResultActions signup(String email, String name, String password) throws Exception {
        return mvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email, "name", name, "password", password))));
    }
}
