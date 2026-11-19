package com.example.boardpractice.domain.post.controller;

import com.example.boardpractice.domain.post.dto.PostCreateRequest;
import com.example.boardpractice.domain.post.dto.PostResponse;
import com.example.boardpractice.domain.post.dto.PostUpdateRequest;
import com.example.boardpractice.domain.post.service.PostService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody @Valid PostCreateRequest request) {
        PostResponse response = postService.createPost(request.getTitle(), request.getContent());
        URI location = URI.create("/api/posts/" + response.getPostId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<PostResponse> getPosts() {
        return postService.getPosts();
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }

    @PutMapping("/{postId}")
    public PostResponse updatePost(@PathVariable Long postId,
                                   @RequestBody @Valid PostUpdateRequest request) {
        return postService.updatePost(postId, request.getTitle(), request.getContent());
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
