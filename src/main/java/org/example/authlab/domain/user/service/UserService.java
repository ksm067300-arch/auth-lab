package org.example.authlab.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.authlab.domain.user.entity.User;
import org.example.authlab.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional // 호출된 곳에 이미 트랜잭션이 있을 경우 새로 만들지 않고 합류함(Propagation.REQUIRED)
    public User save(User user) {
        return userRepository.save(user);
    }
}
