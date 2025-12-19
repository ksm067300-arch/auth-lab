package org.example.authlab.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.authlab.domain.user.entity.User;
import org.example.authlab.domain.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    public String getMyInfo(@AuthenticationPrincipal User user) {
        return "인증 성공! 당신의 ID는: " + user.getUsername();
    }

}
