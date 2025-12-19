package org.example.authlab.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // 2FA 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "two_factor_type")
    @Builder.Default
    private TwoFactorType twoFactorType = TwoFactorType.NONE;

    // TOTP 시크릿 키
    @Column(name = "totp_secret")
    private String totpSecret;


    // TOTP 활성화
    public void enableTotp(String secret) {
        this.twoFactorType = TwoFactorType.TOTP;
        this.totpSecret = secret;
    }

    // CSPRNG 활성화
    public void enableCsprng() {
        this.twoFactorType = TwoFactorType.CSPRNG;
        this.totpSecret = null;
    }
}
