package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.AccessLog;
import kst.toy.ai.logdetector.domain.LogEvent;
import kst.toy.ai.logdetector.dto.LogRequestDto;
import kst.toy.ai.logdetector.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;
    private final RedisWindowService windowService;
    private final RealTimeDetectionService detectionService;

    private final KafkaProducerService producer;

    public void publish(LogRequestDto dto) {

        LogEvent event = LogEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .ip(dto.getIp())
                .url(dto.getUrl())
                .status(dto.getStatus())
                .timestamp(System.currentTimeMillis())
                .createdAt(System.currentTimeMillis())
                .build();

        producer.send(event);
    }
    /**
     *    ↓
     * [Redis: IP별 카운트(+TTL)]
     *    ↓
     * [RealTimeDetector]
     *    ↓
     * [위험도 판단]
     *    ↓
     * [AnomalyResult 저장 + Alert]
     * */
    public AccessLog save(LogRequestDto dto) {

        // 1. Redis 증가
        long count = windowService.addRequest(dto.getIp());
        log.info("DETECT COUNT={}", count);
        // 2. 즉시 탐지
        detectionService.detect(dto.getIp(), dto.getStatus(), count);

        // 3. DB 저장
        AccessLog log = AccessLog.builder()
                .ip(dto.getIp())
                .url(dto.getUrl())
                .status(dto.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        return logRepository.save(log);
    }

}