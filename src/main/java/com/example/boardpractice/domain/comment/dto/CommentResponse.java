package com.example.boardpractice.domain.comment.dto;

import com.example.boardpractice.domain.comment.entity.Comment;
import java.time.LocalDateTime;

public class CommentResponse {
    private final Long commentId;
    private final Long postId;
    private final String content;
    private final LocalDateTime createdAt;

    public CommentResponse(Comment comment) {
        this.commentId = comment.getCommentId();
        this.postId = comment.getPost().getPostId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getPostId() {
        return postId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
