package org.example.authlab.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;

    private boolean requiresTwoFactor;
    private String preAuthToken;
    private String message;
}
