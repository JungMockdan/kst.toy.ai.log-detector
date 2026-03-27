package kst.toy.ai.logdetector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kst.toy.ai.logdetector.domain.AnomalyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class DashboardSseService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        log.info("[SSE] 구독자 연결. 현재 구독자 수: {}", emitters.size());
        return emitter;
    }

    public void broadcast(AnomalyResult result) {
        if (emitters.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SSE] JSON 직렬화 실패: {}", e.getMessage());
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("anomaly").data(json));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
        log.debug("[SSE] broadcast → {} 구독자, ip={}, risk={}", emitters.size(), result.getIp(), result.getRiskLevel());
    }
}
