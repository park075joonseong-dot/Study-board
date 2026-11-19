package com.example.boardpractice.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostUpdateRequest {
    private String title;
    private String content;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 255, message = "제목은 255자 이하로 입력해 주세요.")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 10000, message = "내용은 10000자 이하로 입력해 주세요.")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
