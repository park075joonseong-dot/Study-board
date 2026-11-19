package com.example.boardpractice.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {
    private String email;
    private String name;
    private String password;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    @Size(max = 254, message = "이메일은 254자 이하로 입력해 주세요.")
    public String getEmail() {
        return email;
    }

    @NotBlank(message = "본명을 입력해 주세요.")
    @Size(max = 100, message = "이름은 100자 이하로 입력해 주세요.")
    public String getName() {
        return name;
    }

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(max = 72, message = "비밀번호는 72자 이하로 입력해 주세요.")
    public String getPassword() {
        return password;
    }
}
