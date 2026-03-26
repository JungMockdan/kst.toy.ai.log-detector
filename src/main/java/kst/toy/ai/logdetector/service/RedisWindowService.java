package kst.toy.ai.logdetector.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisWindowService {

    private final StringRedisTemplate redisTemplate;

    private static final long WINDOW_DURATION_SECONDS = 300;

    public long addRequest(String ip) {

        String key = "WINDOW:" + ip;
        long now = System.currentTimeMillis();

        // timestamp 저장
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);

        // 오래된 데이터 제거
        long cutoffTime = now - (WINDOW_DURATION_SECONDS * 1000);
        redisTemplate.opsForZSet().removeRangeByScore(
                key, 0, cutoffTime
        );

        return redisTemplate.opsForZSet().size(key);
    }

    private ObjectMapper objectMapper;
    public Map<String, Object> getActiveIps() {
        log.debug("/active-alert");
        Set<String> keys = redisTemplate.keys("ALERT:*");


        if (keys.isEmpty()||keys==null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();

        for (String key : keys) {
            log.debug("IP: ", key);
            // String으로 가져온 뒤, JSON 파싱을 위해 Object로 저장
            // 1. Redis에서 문자열을 가져옵니다.
            String jsonStr = redisTemplate.opsForValue().get(key);
            try {
                // 2. 문자열을 "JSON 객체 구조(JsonNode)"로 파싱합니다.
                // 이렇게 해야 Jackson이 응답 시 이중 따옴표 처리를 하지 않습니다.
                JsonNode jsonNode = objectMapper.readTree(jsonStr);
                result.put(key.replace("ALERT:", ""), jsonNode);
            } catch (Exception e) {
                // 파싱 실패 시 원본 문자열이라도 담습니다.
                result.put(key.replace("ALERT:", ""), jsonStr);
            }
        }

        return result;
    }

}
