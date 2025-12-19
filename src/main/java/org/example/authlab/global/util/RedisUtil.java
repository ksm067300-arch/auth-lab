package org.example.authlab.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 키 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 블랙리스트 등록
    public void setBlackList(String token, long remainingTime) {
        redisTemplate.opsForValue().set(token, "logout", remainingTime, TimeUnit.MILLISECONDS);
    }

    // 블랙리스트 확인
    public boolean isBlackListed(String token) {
        return hasKey(token);
    }
}
