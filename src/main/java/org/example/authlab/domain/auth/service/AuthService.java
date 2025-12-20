package org.example.authlab.domain.auth.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.RequiredArgsConstructor;
import org.example.authlab.domain.auth.dto.request.LoginRequest;
import org.example.authlab.domain.auth.dto.request.SignupRequest;
import org.example.authlab.domain.auth.dto.response.LoginResponse;
import org.example.authlab.domain.auth.dto.response.SignupResponse;
import org.example.authlab.domain.auth.jwt.JwtUtil;
import org.example.authlab.domain.user.entity.TwoFactorType;
import org.example.authlab.domain.user.entity.User;
import org.example.authlab.domain.user.service.UserService;
import org.example.authlab.global.util.RedisUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    // CSPRNG 난수 생성
    private final SecureRandom secureRandom = new SecureRandom();
    // Google OTP 검증
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

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

        // 2FA가 필요한지 확인
        if (user.getTwoFactorType() != null) {
            String preAuthToken = UUID.randomUUID().toString();
            redisUtil.set("PRE_AUTH:" + preAuthToken, user.getUsername(), 300, TimeUnit.SECONDS);

            if (TwoFactorType.CSPRNG.equals(user.getTwoFactorType())) {
                // 난수 생성
                String code = String.valueOf(100000 + secureRandom.nextInt(900000));

                // 코드 Redis 저장
                redisUtil.set("CSPRNG_CODE:" + user.getUsername(), code, 180, TimeUnit.SECONDS);

                // 이메일 발송
                sendEmailMock(user.getEmail(), code);
            }

            return LoginResponse.builder()
                    .requiresTwoFactor(true)
                    .preAuthToken(preAuthToken)
                    .message("2단계 인증(OTP)이 필요합니다.")
                    .build();
        }

        return issueTokens(user.getUsername());
    }

    public String logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7);

            // 토큰 남은 시간
            long remainingTime = jwtUtil.getRemainingTime(accessToken);

            // 토큰 시간이 남았으면 redis 블랙리스트에 등록
            if (remainingTime > 0) {
                redisUtil.setBlackList(accessToken, remainingTime);
                return "로그아웃 되었습니다.";
            }
        }
        return "이미 로그아웃 상태이거나 토큰이 유효하지 않습니다.";
    }

    @Transactional(readOnly = true)
    public LoginResponse verifySecondFactor(String preAuthToken, String code) {
        String username = redisUtil.get("PRE_AUTH:" + preAuthToken);
        if (username == null) {
            throw new IllegalArgumentException("인증 세션이 만료되었습니다.");
        }

        User user = userService.findByUsername(username);

        verifyCodeByType(user, code.trim());

        redisUtil.delete("PRE_AUTH:" + preAuthToken);

        return issueTokens(username);
    }

    private void verifyCodeByType(User user, String code) {
        if (TwoFactorType.CSPRNG.equals(user.getTwoFactorType())) {
            verifyEmailCode(user.getUsername(), code);
        } else if (TwoFactorType.TOTP.equals(user.getTwoFactorType())) {
            verifyTotpCode(user.getTotpSecret(), code);
        } else {
            throw new IllegalArgumentException("지원하지 않는 인증 방식입니다.");
        }
    }

    private void verifyTotpCode(String secretKey, String code) {
        int verificationCode;
        try {
            verificationCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("인증 코드는 숫자여야 합니다.");
        }

        boolean isCodeValid = gAuth.authorize(secretKey, verificationCode);

        if (!isCodeValid) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다. 다시 시도해주세요.");
        }
    }

    private void verifyEmailCode(String username, String code) {
        String storedCode = redisUtil.get("CSPRNG_CODE:" + username);

        if (storedCode == null || !storedCode.equals(code)) {
            throw new IllegalArgumentException("이메일 인증 코드가 일치하지 않습니다.");
        }
        redisUtil.delete("CSPRNG_CODE:" + username);

    }

    // 토큰 발급 로직
    private LoginResponse issueTokens(String username) {
        String accessToken = jwtUtil.createAccessToken(username);
        String refreshToken = jwtUtil.createRefreshToken(username);

        return LoginResponse.builder()
                .requiresTwoFactor(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("로그인 성공")
                .build();
    }

    private void sendEmailMock(String email, String code) {
        try {
            // 외부 SMTP 서버 통신 지연 시뮬레이션 (300ms ~ 500ms)
            Thread.sleep(300);
            System.out.println("[Email Send] To: " + email + ", Code: " + code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String generateTotpSecret() {
        return gAuth.createCredentials().getKey();
    }

    @Transactional
    public String activateTotp(String bearerToken, String secretKey, String code) {
        User user = userService.findByUsername(jwtUtil.getUsername(bearerToken.substring(7)));
        verifyTotpCode(secretKey, code.trim());
        user.enableTotp(secretKey);
        return "TOTP가 성공적으로 활성화되었습니다.";
    }
}
