package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.AccessLog;
import kst.toy.ai.logdetector.domain.LogEvent;
import kst.toy.ai.logdetector.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final RedisWindowService windowService;
    private final RealTimeDetectionService detectionService;
    private final LogRepository logRepository;
    @Value("${kafka.test.delay.ms:0}")
    private long kafkaTestDelayMs;

    @KafkaListener(topics = "logs", groupId = "log-group", concurrency = "3")//처리량3배증가-> partition 3개로 증가  :Page 벤치마크용
    public void consume(LogEvent event, Acknowledgment ack) {

        
        long receiveTime = System.currentTimeMillis();

        if (event.getEventId() == null) {
            log.error("[INVALID EVENT] eventId is null. ip={}", event.getIp());
            return; // 또는 DLQ로 보내기
        }
        // consumedAt 기록
        event.setConsumedAt(receiveTime);
        long kafkaDelay = 0;
        if (event.getCreatedAt() != 0) {
            kafkaDelay = receiveTime - event.getCreatedAt();
        }

        long start = System.currentTimeMillis();

        try {
            log.debug("KAFKA CONSUMED eventid: {}, ip: {}", event.getEventId(), event.getIp());
            // 1. sliding window
            long count = windowService.addRequest(event.getIp());

            // 2. detection
            detectionService.detect(event.getIp(), event.getStatus(), count);
            // 🔹 의도적 딜레이 추가 (Kafka 효과 검증용) - detection 후, DB 저장 전
            try {
                Thread.sleep(kafkaTestDelayMs); // 100ms 딜레이 (설정값으로 조절 가능)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 3. DB 저장
            AccessLog accessLog = AccessLog.builder()
                    .ip(event.getIp())
                    .url(event.getUrl())
                    .status(event.getStatus())
                    .timestamp(LocalDateTime.now())
                    .build();

            logRepository.save(accessLog);
            long end = System.currentTimeMillis();

            long processingTime = end - start;
            long totalLatency = event.getCreatedAt() != 0
                    ? end - event.getCreatedAt()
                    : -1;

            log.info("""
                [CONSUMER METRIC]
                eventId={}
                ip={}
                kafkaDelay={}ms
                processingTime={}ms
                totalLatency={}ms
                """,
                event.getEventId(),
                event.getIp(),
                kafkaDelay,
                processingTime,
                totalLatency
            );

            // 성공 시 commit
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[CONSUME ERROR] eventId={}, error={}", event.getEventId(), e.getMessage());
        } finally {
            long processingTime = System.currentTimeMillis() - start;
            log.info("[EVENT PROCESSED] eventId={}, kafkaDelay={}ms, processingTime={}ms",
                    event.getEventId(), kafkaDelay, processingTime);
            ack.acknowledge();
        }
    }
}
