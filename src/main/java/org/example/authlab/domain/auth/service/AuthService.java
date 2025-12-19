package org.example.authlab.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.authlab.domain.auth.dto.request.LoginRequest;
import org.example.authlab.domain.auth.dto.request.SignupRequest;
import org.example.authlab.domain.auth.dto.response.LoginResponse;
import org.example.authlab.domain.auth.dto.response.SignupResponse;
import org.example.authlab.domain.auth.jwt.JwtUtil;
import org.example.authlab.domain.user.entity.User;
import org.example.authlab.domain.user.service.UserService;
import org.example.authlab.global.util.RedisUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Transactional(rollbackFor = Exception.class)
    public SignupResponse signup(SignupRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = userService.save(
                User.builder()
                        .username(request.getUsername())
                        .password(encodedPassword)
                        .build());

        return new SignupResponse(true, "success", user.getId());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtUtil.createToken(user.getUsername());

        return new LoginResponse(token);
    }

    public void logout(String accessToken) {
        // 토큰 남은 시간
        long remainingTime = jwtUtil.getRemainingTime(accessToken);

        // 토큰 시간이 남았으면 redis 블랙리스트에 등록
        if (remainingTime > 0) {
            redisUtil.setBlackList(accessToken, remainingTime);
        }
    }
}
