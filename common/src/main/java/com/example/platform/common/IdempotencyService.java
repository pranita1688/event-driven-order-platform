package com.example.platform.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IdempotencyService {

    private final ProcessedMessageRepository repository;

    public IdempotencyService(ProcessedMessageRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns true the first time an event id is seen. Must run inside the transaction that
     * applies the event's effects, so "processed" and "effects applied" commit atomically.
     * If two deliveries race, the primary key rejects the second commit and it is retried.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(UUID eventId) {
        if (repository.existsById(eventId)) {
            return false;
        }
        repository.save(new ProcessedMessage(eventId));
        return true;
    }
}
