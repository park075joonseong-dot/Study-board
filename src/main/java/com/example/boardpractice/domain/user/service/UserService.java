package com.example.boardpractice.domain.user.service;

import com.example.boardpractice.domain.user.dto.SignupRequest;
import com.example.boardpractice.domain.user.entity.User;
import com.example.boardpractice.domain.user.repository.UserRepository;
import com.example.boardpractice.global.exception.DuplicateEmailException;
import com.example.boardpractice.global.exception.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void signup(SignupRequest signupRequest) {
        String email = signupRequest.getEmail().strip().toLowerCase(Locale.ROOT);
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        // BCrypt limits passwords by UTF-8 bytes, which can exceed the character count.
        if (signupRequest.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요.");
        }

        String passwordHash = passwordEncoder.encode(signupRequest.getPassword());
        User user = new User(email, passwordHash, signupRequest.getName().strip());
        userRepository.save(user);
    }
}
