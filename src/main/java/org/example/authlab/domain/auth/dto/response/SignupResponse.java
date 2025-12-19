package org.example.authlab.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponse {
    private boolean success;
    private String message;
    private Long id;
}
