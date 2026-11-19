package com.example.boardpractice;

import com.example.boardpractice.domain.comment.repository.CommentRepository;
import com.example.boardpractice.domain.post.repository.PostRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrudApiIntegrationTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void clearTestDatabase() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
    }

    @Test
    void postCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        MvcResult created = createPost("first", "initial content");
        long postId = id(created, "postId");
        String createdAt = JsonPath.read(created.getResponse().getContentAsString(), "$.createdAt");
        assertThat(createdAt).isNotBlank();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/posts/" + postId);

        mvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("first"))
                .andExpect(jsonPath("$.content").value("initial content"));

        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].postId").value((int) postId));

        mvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("updated", "updated content")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("updated"))
                .andExpect(jsonPath("$.createdAt").value(createdAt));

        // 새 요청으로 다시 조회하여 변경 내용이 실제로 저장됐는지 확인한다.
        mvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("updated content"));

        mvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/posts/{postId}", postId)).andExpect(status().isNotFound());
        assertThat(postRepository.count()).isZero();
    }

    @Test
    void listIsEmptyInitiallyAndNewestPostsComeFirst() throws Exception {
        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        long first = id(createPost("first", "body"), "postId");
        long second = id(createPost("second", "body"), "postId");

        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value((int) second))
                .andExpect(jsonPath("$[1].postId").value((int) first));
    }

    @Test
    void contentLongerThanOldVarcharLimitIsStoredCompletely() throws Exception {
        String content = "x".repeat(10000);
        long postId = id(createPost("long content", content), "postId");
        mvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    void invalidPostsAreRejectedWithoutWritingRows() throws Exception {
        List<String> invalidBodies = List.of(
                postBody("   ", "body"),
                postBody("title", "   "),
                "{}",
                "{\"title\":null,\"content\":\"body\"}",
                postBody("x".repeat(256), "body"),
                postBody("title", "x".repeat(10001)));

        for (String body : invalidBodies) {
            mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").exists());
        }
        assertThat(postRepository.count()).isZero();
    }

    @Test
    void invalidUpdateDoesNotChangeExistingPost() throws Exception {
        long postId = id(createPost("original", "original body"), "postId");
        mvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON).content(postBody(" ", "changed")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
        mvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("original"))
                .andExpect(jsonPath("$.content").value("original body"));
    }

    @Test
    void missingPostReturns404ForReadUpdateDeleteAndCommentRequests() throws Exception {
        long missingId = 9999999L;
        mvc.perform(get("/api/posts/{postId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/posts/{postId}", missingId)
                        .contentType(MediaType.APPLICATION_JSON).content(postBody("title", "body")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/{postId}", missingId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{postId}/comments", missingId)).andExpect(status().isNotFound());
        mvc.perform(post("/api/posts/{postId}/comments", missingId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody("comment")))
                .andExpect(status().isNotFound());
        assertThat(commentRepository.count()).isZero();
    }

    @Test
    void malformedOrMissingJsonIsRejected() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commentCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        long postId = id(createPost("post", "body"), "postId");
        mvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        MvcResult created = createComment(postId, "first comment");
        long commentId = id(created, "commentId");
        String createdAt = JsonPath.read(created.getResponse().getContentAsString(), "$.createdAt");
        assertThat(created.getResponse().getHeader("Location"))
                .isEqualTo("/api/posts/" + postId + "/comments/" + commentId);

        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value((int) postId))
                .andExpect(jsonPath("$.content").value("first comment"));

        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody("updated comment")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("updated comment"))
                .andExpect(jsonPath("$[0].createdAt").value(createdAt));

        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNotFound());
        assertThat(commentRepository.count()).isZero();
        assertThat(postRepository.existsById(postId)).isTrue();
    }

    @Test
    void commentCannotBeReadChangedOrDeletedThroughAnotherPost() throws Exception {
        long firstPost = id(createPost("first", "body"), "postId");
        long otherPost = id(createPost("other", "body"), "postId");
        long commentId = id(createComment(firstPost, "keep me"), "commentId");

        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", otherPost, commentId))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", otherPost, commentId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody("wrong update")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", otherPost, commentId))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", firstPost, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("keep me"));
    }

    @Test
    void deletingPostAlsoDeletesItsCommentsAndKeepsOtherPostComments() throws Exception {
        long removedPost = id(createPost("remove", "body"), "postId");
        long keptPost = id(createPost("keep", "body"), "postId");
        long firstComment = id(createComment(removedPost, "first"), "commentId");
        long secondComment = id(createComment(removedPost, "second"), "commentId");
        long keptComment = id(createComment(keptPost, "keep this comment"), "commentId");

        mvc.perform(delete("/api/posts/{postId}", removedPost)).andExpect(status().isNoContent());
        assertThat(commentRepository.existsById(firstComment)).isFalse();
        assertThat(commentRepository.existsById(secondComment)).isFalse();
        assertThat(commentRepository.existsById(keptComment)).isTrue();
        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", keptPost, keptComment))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCommentCreateAndUpdateDoNotWriteChanges() throws Exception {
        long postId = id(createPost("post", "body"), "postId");
        for (String body : List.of(commentBody(" "), "{}", commentBody("x".repeat(2001)))) {
            mvc.perform(post("/api/posts/{postId}/comments", postId)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.content").exists());
        }
        assertThat(commentRepository.count()).isZero();

        long commentId = id(createComment(postId, "unchanged"), "commentId");
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody(" ")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("unchanged"));
    }

    @Test
    void missingCommentReturns404() throws Exception {
        long postId = id(createPost("post", "body"), "postId");
        mvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, 9999999L))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, 9999999L)
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody("body")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, 9999999L))
                .andExpect(status().isNotFound());
    }

    private MvcResult createPost(String title, String content) throws Exception {
        return mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(title, content)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult createComment(long postId, String content) throws Exception {
        return mvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody(content)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long id(MvcResult result, String field) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), "$." + field);
        return value.longValue();
    }

    private String postBody(String title, String content) {
        return "{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}";
    }

    private String commentBody(String content) {
        return "{\"content\":\"" + content + "\"}";
    }
}
