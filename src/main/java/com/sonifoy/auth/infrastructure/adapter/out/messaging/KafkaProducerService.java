package com.sonifoy.auth.infrastructure.adapter.out.messaging;

import com.sonifoy.auth.application.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user-events";

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for email: {}", event.getEmail());
        kafkaTemplate.send(TOPIC, event.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published UserRegisteredEvent to topic: {}", TOPIC);
                    } else {
                        log.error("Failed to publish UserRegisteredEvent", ex);
                    }
                });
    }
}
