package com.example.platform.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records an event in the outbox table. MANDATORY propagation guarantees the event is
 * written in the same transaction as the state change that caused it (no dual write).
 */
@Service
public class OutboxService {

    private final OutboxRepository repository;
    private final EventCodec codec;

    public OutboxService(OutboxRepository repository, EventCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String type, String aggregateId, Object payload) {
        Envelope envelope = codec.wrap(type, aggregateId, payload);
        repository.save(new OutboxEvent(envelope.eventId(), aggregateId, topic, type, codec.toJson(envelope)));
    }
}
