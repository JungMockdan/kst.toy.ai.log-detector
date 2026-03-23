package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    private static final String TOPIC = "logs";

    public void send(LogEvent event) {
        kafkaTemplate.send(TOPIC, event.getIp(), event);
    }
}
