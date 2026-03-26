package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    private static final String TOPIC = "logs";

    public void send(LogEvent event) {
        log.info("Producing event: {}", event);
        kafkaTemplate.send(TOPIC, event.getIp(), event);
    }
}
