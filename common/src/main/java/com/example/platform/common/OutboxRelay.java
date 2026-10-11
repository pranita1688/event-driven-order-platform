package com.example.platform.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and publishes to Kafka. Delivery is at-least-once: if the process dies
 * after the send but before the commit, the event is sent again, which is why every
 * consumer is idempotent.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafka) {
        this.repository = repository;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = repository.lockNextBatch();
        for (OutboxEvent event : batch) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.markPublished();
                log.info("Published {} {} to {}", event.getType(), event.getAggregateId(), event.getTopic());
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Could not publish outbox event {}, will retry: {}", event.getId(), e.toString());
                return; // keep ordering: stop at the first failure
            }
        }
    }
}
