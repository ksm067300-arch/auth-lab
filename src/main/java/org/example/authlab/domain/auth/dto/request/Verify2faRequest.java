package org.example.authlab.domain.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Verify2faRequest {
    private String preAuthToken;
    private String code;
}
