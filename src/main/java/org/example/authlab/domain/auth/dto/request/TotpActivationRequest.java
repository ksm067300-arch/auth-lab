package org.example.authlab.domain.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TotpActivationRequest {
    private String secretKey;
    private String code;
}
