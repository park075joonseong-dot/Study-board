package com.example.boardpractice.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentCreateRequest {
    private String content;

    @NotBlank(message = "댓글 내용을 입력해 주세요.")
    @Size(max = 2000, message = "댓글은 2000자 이하로 입력해 주세요.")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
