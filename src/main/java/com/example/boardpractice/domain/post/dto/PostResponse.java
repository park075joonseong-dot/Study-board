package com.example.boardpractice.domain.post.dto;

import com.example.boardpractice.domain.post.entity.Post;

import java.time.LocalDateTime;

public class PostResponse {
    private Long postId;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    public PostResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.createdAt = post.getCreatedAt();
    }

    public Long getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
