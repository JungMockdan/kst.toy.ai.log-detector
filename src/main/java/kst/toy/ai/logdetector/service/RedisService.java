package kst.toy.ai.logdetector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    private static final int WINDOW_SECONDS = 60;

    /**
     * 레디스 슬라이딩윈도우 카운터
     * 키: IP:{ip}
     * 값: 최근 N초 요청 수 : WINDOW_SECONDS
     * TTL: 60초 (예시)
     * */
    public Long incrementIpCount(String ip) {
        String key = "IP:" + ip;

        Long count = redisTemplate.opsForValue().increment(key);

        // TTL이 없으면 설정
        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl == -1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }

        return count;
    }

    public String getCount(String ip) {
        return redisTemplate.opsForValue().get("IP:" + ip);
    }
}
