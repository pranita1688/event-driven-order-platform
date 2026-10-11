package com.example.platform.order;

import com.example.platform.common.EventCodec;
import com.example.platform.common.Envelope;
import com.example.platform.common.Events;
import com.example.platform.common.IdempotencyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Receives saga outcomes from inventory and payment and settles the order. */
@Component
public class OrderEventListener {

    private final EventCodec codec;
    private final IdempotencyService idempotency;
    private final OrderService orders;

    public OrderEventListener(EventCodec codec, IdempotencyService idempotency, OrderService orders) {
        this.codec = codec;
        this.idempotency = idempotency;
        this.orders = orders;
    }

    @KafkaListener(topics = {Events.Topics.INVENTORY, Events.Topics.PAYMENTS}, groupId = "order-service")
    @Transactional
    public void on(String message) {
        Envelope event = codec.parse(message);
        switch (event.type()) {
            case Events.Types.STOCK_REJECTED -> {
                if (idempotency.firstTime(event.eventId())) {
                    var payload = codec.payload(event, Events.StockRejected.class);
                    orders.reject(payload.orderId(), payload.reason());
                }
            }
            case Events.Types.PAYMENT_COMPLETED -> {
                if (idempotency.firstTime(event.eventId())) {
                    orders.confirm(UUID.fromString(event.aggregateId()));
                }
            }
            case Events.Types.PAYMENT_FAILED -> {
                if (idempotency.firstTime(event.eventId())) {
                    var payload = codec.payload(event, Events.PaymentFailed.class);
                    orders.cancel(payload.orderId(), payload.reason());
                }
            }
            default -> {
                // StockReserved and friends are not our concern
            }
        }
    }
}
