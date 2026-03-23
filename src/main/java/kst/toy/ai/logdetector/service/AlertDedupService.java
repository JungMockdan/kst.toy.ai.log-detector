package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.DetectionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AlertDedupService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public void saveAlertToRedis(DetectionResult result) {

        try {
            String key = "ALERT:" + result.getIp();

            String value = objectMapper.writeValueAsString(result);

            redisTemplate.opsForValue().set(
                    key,
                    value,
                    Duration.ofMinutes(1)
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public boolean isFirstDetection(String ip) {

        String key = "ALERT:" + ip;

        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(1));

        return Boolean.TRUE.equals(result);
    }
}