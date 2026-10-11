package com.example.platform.inventory;

import com.example.platform.common.Envelope;
import com.example.platform.common.EventCodec;
import com.example.platform.common.Events;
import com.example.platform.common.IdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class InventoryEventListener {

    private final EventCodec codec;
    private final IdempotencyService idempotency;
    private final InventoryService inventory;

    public InventoryEventListener(EventCodec codec, IdempotencyService idempotency, InventoryService inventory) {
        this.codec = codec;
        this.idempotency = idempotency;
        this.inventory = inventory;
    }

    @KafkaListener(topics = {Events.Topics.ORDERS, Events.Topics.PAYMENTS}, groupId = "inventory-service")
    @Transactional
    public void on(String message) {
        Envelope event = codec.parse(message);
        switch (event.type()) {
            case Events.Types.ORDER_CREATED -> {
                if (idempotency.firstTime(event.eventId())) {
                    inventory.reserve(codec.payload(event, Events.OrderCreated.class));
                }
            }
            case Events.Types.PAYMENT_FAILED -> {
                if (idempotency.firstTime(event.eventId())) {
                    inventory.release(UUID.fromString(event.aggregateId()));
                }
            }
            default -> {
            }
        }
    }
}
