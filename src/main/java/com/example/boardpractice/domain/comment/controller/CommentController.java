package com.example.boardpractice.domain.comment.controller;

import com.example.boardpractice.domain.comment.dto.CommentCreateRequest;
import com.example.boardpractice.domain.comment.dto.CommentResponse;
import com.example.boardpractice.domain.comment.dto.CommentUpdateRequest;
import com.example.boardpractice.domain.comment.service.CommentService;
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
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@PathVariable Long postId,
                                                        @RequestBody @Valid CommentCreateRequest request) {
        CommentResponse response = commentService.createComment(postId, request.getContent());
        URI location = URI.create("/api/posts/" + postId + "/comments/" + response.getCommentId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return commentService.getComments(postId);
    }

    @GetMapping("/{commentId}")
    public CommentResponse getComment(@PathVariable Long postId, @PathVariable Long commentId) {
        return commentService.getComment(postId, commentId);
    }

    @PutMapping("/{commentId}")
    public CommentResponse updateComment(@PathVariable Long postId, @PathVariable Long commentId,
                                         @RequestBody @Valid CommentUpdateRequest request) {
        return commentService.updateComment(postId, commentId, request.getContent());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
