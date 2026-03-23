package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.AccessLog;
import kst.toy.ai.logdetector.domain.LogEvent;
import kst.toy.ai.logdetector.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final RedisWindowService windowService;
    private final RealTimeDetectionService detectionService;
    private final LogRepository logRepository;

    @KafkaListener(topics = "logs", groupId = "log-group")
    public void consume(LogEvent event) {

        // 1. sliding window
        long count = windowService.addRequest(event.getIp());

        // 2. detection
        detectionService.detect(event.getIp(), event.getStatus(), count);

        // 3. DB 저장
        AccessLog log = AccessLog.builder()
                .ip(event.getIp())
                .url(event.getUrl())
                .status(event.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        logRepository.save(log);
    }
}
