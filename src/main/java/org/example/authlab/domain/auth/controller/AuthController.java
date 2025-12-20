package org.example.authlab.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.authlab.domain.auth.dto.request.LoginRequest;
import org.example.authlab.domain.auth.dto.request.SignupRequest;
import org.example.authlab.domain.auth.dto.request.TotpActivationRequest;
import org.example.authlab.domain.auth.dto.request.Verify2faRequest;
import org.example.authlab.domain.auth.dto.response.LoginResponse;
import org.example.authlab.domain.auth.dto.response.SignupResponse;
import org.example.authlab.domain.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @ResponseStatus(HttpStatus.CREATED)
    @GetMapping("/totp/setup")
    public String setupTotp() {
        return authService.generateTotpSecret();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public SignupResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login/2fa")
    public LoginResponse verifyTwoFactor(@RequestBody Verify2faRequest request) {
        return authService.verifySecondFactor(request.getPreAuthToken(), request.getCode());
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String bearerToken) {
        return authService.logout(bearerToken);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/totp/activate")
    public String activateTotp(@RequestBody @Valid TotpActivationRequest request,
                               @RequestHeader("Authorization") String bearerToken) {
        return authService.activateTotp(bearerToken, request.getSecretKey(), request.getCode());
    }
}
